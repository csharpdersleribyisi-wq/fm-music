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
const CHARLIE_UID = "charlie_789";

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

test("Unauthenticated user cannot read or write playlists, profiles, or collaborative playlists", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("playlists").get());
  await assertFails(unauthDb.collection("collaborativePlaylists").doc("cp_1").get());
  await assertFails(unauthDb.collection("userDirectory").get());
  await assertFails(unauthDb.collection("collabInvites").doc("CODE12").get());
});

test("Authenticated owner can create and update taste profile and playlists matching Kotlin payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      username: "alice_vibe",
      favoriteGenres: ["Synthwave", "Lo-Fi Chill"],
      preferredMood: "Midnight",
      offlineModeEnabled: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("userDirectory").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_vibe",
      displayName: "Alice",
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      offlineModeEnabled: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_mix_1").set({
      userId: ALICE_UID,
      title: "Midnight Focus",
      description: "Crafted from Synthwave favorites",
      moodTag: "Focus",
      coverKey: "midnight_synth",
      trackIds: ["track_neon_pulse", "track_velvet_rain"],
      personalized: true,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Collaborative playlists allow invited collaborators to add, remove, and reorder tracks while blocking unauthorized edits", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("collaborativePlaylists").doc("cp_roadtrip").set({
      ownerId: ALICE_UID,
      ownerName: "Alice",
      title: "Roadtrip Mix",
      description: "Shared car queue",
      moodTag: "Energetic",
      coverKey: "sonic_horizon",
      trackIds: ["track_neon_pulse", "track_velvet_rain"],
      collaboratorUids: [ALICE_UID],
      invitedHandles: [],
      inviteCode: "ROAD24",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("collabInvites").doc("ROAD24").set({
      inviteCode: "ROAD24",
      playlistId: "cp_roadtrip",
      title: "Roadtrip Mix",
      ownerId: ALICE_UID,
      ownerName: "Alice",
      createdAt: created,
    })
  );

  // Invite listing is strictly forbidden
  await assertFails(bobDb.collection("collabInvites").get());

  // Non-collaborator Charlie cannot read or hijack tracks before joining
  await assertFails(charlieDb.collection("collaborativePlaylists").doc("cp_roadtrip").get());
  await assertFails(
    charlieDb.collection("collaborativePlaylists").doc("cp_roadtrip").update({
      trackIds: ["track_neon_pulse"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob looks up the secret invite code and joins himself
  await assertSucceeds(bobDb.collection("collabInvites").doc("ROAD24").get());
  await assertSucceeds(
    bobDb.collection("collaborativePlaylists").doc("cp_roadtrip").update({
      collaboratorUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob can now reorder and add tracks
  await assertSucceeds(
    bobDb.collection("collaborativePlaylists").doc("cp_roadtrip").update({
      trackIds: ["track_velvet_rain", "track_neon_pulse", "track_aurora_drive"],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot rename Alice's playlist or delete it
  await assertFails(
    bobDb.collection("collaborativePlaylists").doc("cp_roadtrip").update({
      title: "Renamed by Bob",
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(bobDb.collection("collaborativePlaylists").doc("cp_roadtrip").delete());

  // Alice can delete her own collaborative playlist
  await assertSucceeds(aliceDb.collection("collaborativePlaylists").doc("cp_roadtrip").delete());
});

test("Public online playlists are readable by all signed-in users and writable only by owner", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("publicPlaylists").doc("alice_123_pl_1").set({
      ownerId: ALICE_UID,
      ownerName: "Alice",
      ownerUsername: "alice_vibe",
      sourcePlaylistId: "pl_1",
      title: "Sunrise Synths",
      description: "Published online for everyone",
      moodTag: "Uplifting",
      coverKey: "midnight_synth",
      trackIds: ["track_neon_pulse", "track_aurora_drive"],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Unauthenticated cannot read or write
  await assertFails(unauthDb.collection("publicPlaylists").get());

  // Bob (any signed-in listener) can read and list
  await assertSucceeds(bobDb.collection("publicPlaylists").get());
  await assertSucceeds(bobDb.collection("publicPlaylists").doc("alice_123_pl_1").get());

  // Bob cannot overwrite or delete Alice's public playlist
  await assertFails(
    bobDb.collection("publicPlaylists").doc("alice_123_pl_1").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("publicPlaylists").doc("alice_123_pl_1").delete());

  // Alice can update and unpublish
  await assertSucceeds(
    aliceDb.collection("publicPlaylists").doc("alice_123_pl_1").update({
      title: "Sunrise Synths Vol 2",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(aliceDb.collection("publicPlaylists").doc("alice_123_pl_1").delete());
});
