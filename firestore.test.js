const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("usernames").doc("alice_connect").get());
  await assertFails(unauthDb.collection("public_profiles").where("discoverable", "==", true).get());
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("stories").where("visibility", "==", "PUBLIC").get());
});

test("Phase 2 user profile: owner create/update succeeds, cross-user read/write & status tampering denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      userId: ALICE_UID,
      fullName: "Alice Rahman",
      username: "alice_connect",
      email: "alice@connect.example",
      profilePhoto: "https://example.com/alice.jpg",
      bio: "Connecting globally",
      preferredLanguage: "en",
      languageCode: "en",
      themeMode: "SYSTEM",
      accountStatus: "ACTIVE",
      privateAccount: false,
      notificationsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "bn",
      languageCode: "bn",
      themeMode: "DARK",
      bio: "Updated bio",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user PII isolation (read, update, delete)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ preferredLanguage: "hi", languageCode: "hi" }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).delete());

  // Account status tampering & ghost field rejection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      accountStatus: "SUSPENDED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      plaintextPassword: "secret",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Username reservations: prevents duplicate usernames and cross-user deletion", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("usernames").doc("alice_connect").set({
      uid: ALICE_UID,
      username: "alice_connect",
      createdAt: now,
    })
  );

  // Availability check via single-doc get allowed; list scraping forbidden
  await assertSucceeds(bobDb.collection("usernames").doc("alice_connect").get());
  await assertFails(bobDb.collection("usernames").get());

  // Duplicate username registration by Bob must fail
  await assertFails(
    bobDb.collection("usernames").doc("alice_connect").set({
      uid: BOB_UID,
      username: "alice_connect",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot delete Alice's username lock
  await assertFails(bobDb.collection("usernames").doc("alice_connect").delete());
  // Alice can delete her own username lock when changing handle
  await assertSucceeds(aliceDb.collection("usernames").doc("alice_connect").delete());
});

test("Public profiles: owner pre-create get succeeds, non-discoverable/missing cross-user get fails, discoverable query succeeds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Owner can observe/get own profile doc even before ensureUserProvisioned creates it (prevents PERMISSION_DENIED race)
  await assertSucceeds(aliceDb.collection("public_profiles").doc(ALICE_UID).get());
  // Another user must NOT be able to probe a non-existent public profile
  await assertFails(bobDb.collection("public_profiles").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_connect",
      displayName: "Alice Rahman",
      bio: "Sharing life moments",
      discoverable: true,
      privateAccount: false,
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(bobDb.collection("public_profiles").doc(ALICE_UID).get());
  await assertSucceeds(bobDb.collection("public_profiles").where("discoverable", "==", true).get());
  await assertFails(bobDb.collection("public_profiles").get());

  // Non-discoverable profile remains readable by owner but denied to others
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      discoverable: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(aliceDb.collection("public_profiles").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("public_profiles").doc(ALICE_UID).get());

  // Restore discoverable for remaining assertions
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      discoverable: true,
      updatedAt: new Date(Date.now() - 800),
    })
  );

  await assertSucceeds(
    bobDb.collection("public_profiles").doc(ALICE_UID).update({
      followersCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    bobDb.collection("public_profiles").doc(ALICE_UID).update({
      bio: "Hacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Posts, likes, comments, follows, stories, and notifications", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice_connect",
      authorDisplayName: "Alice Rahman",
      caption: "Sunset over Dhaka",
      mediaType: "IMAGE",
      mediaUrl: "https://example.com/photo.jpg",
      category: "MOMENTS",
      visibility: "PUBLIC",
      likesCount: 0,
      commentsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "PUBLIC").get());
  await assertFails(bobDb.collection("posts").get());

  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID).set({
      userId: BOB_UID,
      createdAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(BOB_UID).collection("notifications").doc("notif_1").set({
      recipientId: BOB_UID,
      actorId: ALICE_UID,
      actorUsername: "alice_connect",
      type: "FOLLOW",
      message: "started following you",
      readFlag: false,
      createdAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).collection("notifications").get());
});
