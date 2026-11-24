const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_001";
const ADMIN_EMAIL = "nirjonnahuel00ofc@gmail.com";
const USER_UID = "viewer_uid_002";
const USER_EMAIL = "viewer@example.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function spoofedUnverifiedAdminContext() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
}

function regularUserContext() {
  return testEnv.authenticatedContext(USER_UID, {
    email: USER_EMAIL,
    email_verified: true,
  });
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Guest & regular users can read channels and updates_inbox, but cannot write them", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("channels").doc("ch_news_1").set({
      name: "Global News Live",
      category: "News",
      language: "English",
      logo: "https://example.com/logo.png",
      primary_url: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
      backup_url: "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
      is_active: true,
      created_at: past,
      updated_at: past,
    });
    await db.collection("updates_inbox").doc("upd_1").set({
      title: "Welcome to PulseTV",
      message: "All live streams synced in real time.",
      category: "Notice",
      timestamp: past,
    });
  });

  const guestDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(guestDb.collection("channels").get());
  await assertSucceeds(guestDb.collection("channels").doc("ch_news_1").get());
  await assertSucceeds(guestDb.collection("updates_inbox").get());

  // Guest cannot create, update, or delete channels or notices
  await assertFails(
    guestDb.collection("channels").doc("ch_hack").set({
      name: "Rogue",
      category: "News",
      language: "English",
      primary_url: "https://example.com/stream.m3u8",
      is_active: true,
      created_at: past,
      updated_at: past,
    })
  );
  await assertFails(
    guestDb.collection("channels").doc("ch_news_1").update({ is_active: false, updated_at: new Date(Date.now() - 1000) })
  );
  await assertFails(guestDb.collection("channels").doc("ch_news_1").delete());
  await assertFails(
    guestDb.collection("updates_inbox").doc("upd_hack").set({
      title: "Fake Notice",
      message: "Unauthorized",
      timestamp: past,
    })
  );

  // Regular signed-in user also cannot modify admin collections
  const userDb = regularUserContext().firestore();
  await assertFails(
    userDb.collection("channels").doc("ch_news_1").update({ name: "Hijacked", updated_at: new Date(Date.now() - 1000) })
  );
  await assertFails(userDb.collection("updates_inbox").doc("upd_1").delete());
});

test("Verified Admin can create, partially update, toggle, and delete channels", async () => {
  const adminDb = adminContext().firestore();
  const created = new Date(Date.now() - 10000);

  // Create channel (mirrors Kotlin ChannelRepository.createChannel map - NO 'id' field)
  await assertSucceeds(
    adminDb.collection("channels").doc("ch_sports_1").set({
      name: "Apex Sports HD",
      category: "Sports",
      language: "English",
      primary_url: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
      is_active: true,
      created_at: created,
      updated_at: created,
    })
  );

  // Partial update: toggle is_active and update stream URL while preserving created_at
  await assertSucceeds(
    adminDb.collection("channels").doc("ch_sports_1").update({
      is_active: false,
      backup_url: "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
      updated_at: new Date(Date.now() - 1000),
    })
  );

  // Reject mutating created_at
  await assertFails(
    adminDb.collection("channels").doc("ch_sports_1").update({
      created_at: new Date(Date.now() - 500),
      updated_at: new Date(Date.now() - 500),
    })
  );

  // Reject ghost 'id' field on create (parity with @DocumentId)
  await assertFails(
    adminDb.collection("channels").doc("ch_extra_id").set({
      id: "ch_extra_id",
      name: "Bad Payload",
      category: "Movies",
      language: "English",
      primary_url: "https://example.com/a.m3u8",
      is_active: true,
      created_at: created,
      updated_at: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    adminDb.collection("channels").doc("ch_future").set({
      name: "Future Stream",
      category: "Movies",
      language: "English",
      primary_url: "https://example.com/a.m3u8",
      is_active: true,
      created_at: new Date(Date.now() + 60000),
      updated_at: new Date(Date.now() + 60000),
    })
  );

  // Admin delete succeeds
  await assertSucceeds(adminDb.collection("channels").doc("ch_sports_1").delete());
});

test("Spoofed unverified admin email is rejected", async () => {
  const spoofDb = spoofedUnverifiedAdminContext().firestore();
  const past = new Date(Date.now() - 1000);
  await assertFails(
    spoofDb.collection("channels").doc("ch_spoof").set({
      name: "Spoofed Channel",
      category: "News",
      language: "English",
      primary_url: "https://example.com/live.m3u8",
      is_active: true,
      created_at: past,
      updated_at: past,
    })
  );
});

test("Broken link reports: guests can submit for existing channels; only admins can read or clear", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("channels").doc("ch_live_1").set({
      name: "Cinema 24/7",
      category: "Movies",
      language: "English",
      primary_url: "https://example.com/cinema.m3u8",
      is_active: true,
      created_at: past,
      updated_at: past,
    });
  });

  const guestDb = testEnv.unauthenticatedContext().firestore();

  // Guest can create valid report for existing channel
  await assertSucceeds(
    guestDb.collection("broken_reports").doc("rep_1").set({
      channel_id: "ch_live_1",
      channel_name: "Cinema 24/7",
      reason: "Primary and backup streams timed out",
      timestamp: new Date(Date.now() - 1000),
    })
  );

  // Guest cannot submit report for non-existent channel
  await assertFails(
    guestDb.collection("broken_reports").doc("rep_orphan").set({
      channel_id: "non_existent_ch",
      channel_name: "Ghost",
      reason: "Spam",
      timestamp: new Date(Date.now() - 1000),
    })
  );

  // Guest & regular user cannot read or delete broken_reports
  await assertFails(guestDb.collection("broken_reports").get());
  await assertFails(guestDb.collection("broken_reports").doc("rep_1").delete());
  const userDb = regularUserContext().firestore();
  await assertFails(userDb.collection("broken_reports").get());

  // Verified Admin can read and clear broken_reports
  const adminDb = adminContext().firestore();
  await assertSucceeds(adminDb.collection("broken_reports").get());
  await assertSucceeds(adminDb.collection("broken_reports").doc("rep_1").delete());
});

test("Admin can publish and delete Update Inbox notices", async () => {
  const adminDb = adminContext().firestore();
  await assertSucceeds(
    adminDb.collection("updates_inbox").doc("notice_1").set({
      title: "New Sports Feed Added",
      message: "Apex Sports HD is now live in 1080p.",
      category: "Channel Update",
      timestamp: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(adminDb.collection("updates_inbox").doc("notice_1").delete());
});

test("Live Events: public read across devices, admin-only CRUD, and broken report support", async () => {
  const created = new Date(Date.now() - 5000);
  const adminDb = adminContext().firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();
  const userDb = regularUserContext().firestore();

  await assertSucceeds(
    adminDb.collection("live_events").doc("evt_cricket_1").set({
      title: "IND vs AUS - T20 Final",
      sport: "Cricket",
      teams: "India vs Australia",
      status: "LIVE",
      thumbnail: "",
      stream_url: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
      backup_url: "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
      is_active: true,
      created_at: created,
      updated_at: created,
    })
  );

  // Guest and regular users can read live_events, cannot write
  await assertSucceeds(guestDb.collection("live_events").get());
  await assertSucceeds(guestDb.collection("live_events").doc("evt_cricket_1").get());
  await assertFails(
    guestDb.collection("live_events").doc("evt_hack").set({
      title: "Fake Match",
      sport: "Cricket",
      status: "LIVE",
      stream_url: "https://example.com/a.m3u8",
      is_active: true,
      created_at: created,
      updated_at: created,
    })
  );
  await assertFails(
    userDb.collection("live_events").doc("evt_cricket_1").update({
      is_active: false,
      updated_at: new Date(Date.now() - 1000),
    })
  );

  // Guest can submit broken report for an active live_event
  await assertSucceeds(
    guestDb.collection("broken_reports").doc("rep_evt_1").set({
      channel_id: "evt_cricket_1",
      channel_name: "IND vs AUS - T20 Final",
      reason: "Stream stalled",
      timestamp: new Date(Date.now() - 1000),
    })
  );

  // Admin can update and delete live_event
  await assertSucceeds(
    adminDb.collection("live_events").doc("evt_cricket_1").update({
      status: "ENDED",
      is_active: false,
      updated_at: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(adminDb.collection("live_events").doc("evt_cricket_1").delete());
});

test("Notice Banners: public read across devices, admin-only upload (URL or base64 data URI) and delete", async () => {
  const created = new Date(Date.now() - 5000);
  const adminDb = adminContext().firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();

  await assertSucceeds(
    adminDb.collection("banners").doc("ban_1").set({
      title: "Match Day Maintenance Notice",
      message: "New backup servers added for all sports feeds.",
      image_data: "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/",
      action_url: "https://t.me/pulsetv_dl_ofc",
      is_active: true,
      created_at: created,
      updated_at: created,
    })
  );

  // Guest can read banners, cannot write
  await assertSucceeds(guestDb.collection("banners").get());
  await assertFails(
    guestDb.collection("banners").doc("ban_hack").set({
      title: "Hack",
      image_data: "https://example.com/a.jpg",
      is_active: true,
      created_at: created,
      updated_at: created,
    })
  );
  await assertFails(guestDb.collection("banners").doc("ban_1").delete());

  // Admin can toggle active and delete
  await assertSucceeds(
    adminDb.collection("banners").doc("ban_1").update({
      is_active: false,
      updated_at: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(adminDb.collection("banners").doc("ban_1").delete());
});

test("Channels support country field and app_metrics allow +1 viewer increments with admin-only reads", async () => {
  const past = new Date(Date.now() - 5000);
  const adminDb = adminContext().firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();

  await assertSucceeds(
    adminDb.collection("channels").doc("ch_bd_1").set({
      name: "Somoy TV",
      category: "News",
      language: "Bengali",
      country: "Bangladesh",
      logo: "",
      primary_url: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
      is_active: true,
      created_at: past,
      updated_at: past,
    })
  );

  // Guest cannot read analytics
  await assertFails(guestDb.collection("app_metrics").doc("summary").get());

  // First create by guest (delta <= 1) succeeds
  await assertSucceeds(
    guestDb.collection("app_metrics").doc("summary").set({
      app_opens: 1,
      channel_plays: 0,
      event_plays: 0,
      updated_at: new Date(Date.now() - 1000),
    })
  );

  // Subsequent +1 update succeeds
  await assertSucceeds(
    guestDb.collection("app_metrics").doc("summary").update({
      channel_plays: 1,
      updated_at: new Date(Date.now() - 500),
    })
  );

  // Inflating counter by +50 fails
  await assertFails(
    guestDb.collection("app_metrics").doc("summary").update({
      channel_plays: 50,
      updated_at: new Date(Date.now() - 200),
    })
  );

  // Admin can read analytics
  await assertSucceeds(adminDb.collection("app_metrics").doc("summary").get());
});


