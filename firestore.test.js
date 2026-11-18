const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated users cannot read or write matrimonial collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("matrimonialProfiles").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("matrimonialProfiles").where("active", "==", true).get()
  );
  await assertFails(
    unauthDb
      .collection("matrimonialRequests")
      .where("participantIds", "array-contains", ALICE_UID)
      .get()
  );
});

test("Authenticated user can observe own non-existent profile doc and filtered active profiles while blocking inactive or cross-owner edits", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Observing own profile doc before creation succeeds
  await assertSucceeds(aliceDb.collection("matrimonialProfiles").doc(ALICE_UID).get());

  // Observing another user's non-existent doc fails
  await assertFails(bobDb.collection("matrimonialProfiles").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("matrimonialProfiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      profileCode: "MB-1001",
      gender: "PATRO",
      age: 27,
      division: "ঢাকা",
      education: "বিএসসি ইঞ্জিনিয়ারিং",
      profession: "সফটওয়্যার প্রকৌশলী",
      practicingNote: "পাঁচ ওয়াক্ত নামাজ ও সুন্নাহ অনুসরণ",
      bioBangla: "দ্বীনদার ও সাদাসিধে জীবনযাপন পছন্দ করি।",
      waliRelation: "বাবা",
      photoBlurred: true,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update preserves profileCode and createdAt
  await assertSucceeds(
    aliceDb.collection("matrimonialProfiles").doc(ALICE_UID).update({
      division: "সিলেট",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Filtered active query succeeds for Bob
  await assertSucceeds(
    bobDb.collection("matrimonialProfiles").where("active", "==", true).get()
  );

  // Unfiltered list fails
  await assertFails(bobDb.collection("matrimonialProfiles").get());

  // Bob cannot edit Alice's profile
  await assertFails(
    bobDb.collection("matrimonialProfiles").doc(ALICE_UID).update({
      bioBangla: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Matrimonial requests & messages enforce permission gating and participant isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    bobDb.collection("matrimonialProfiles").doc(BOB_UID).set({
      userId: BOB_UID,
      profileCode: "MB-2002",
      gender: "PATRI",
      age: 24,
      division: "চট্টগ্রাম",
      education: "স্নাতকোত্তর",
      profession: "শিক্ষিকা",
      practicingNote: "পাঁচ ওয়াক্ত নামাজ ও হিজাব",
      bioBangla: "ইসলামিক পরিবেশে বেড়ে ওঠা।",
      waliRelation: "বাবা",
      photoBlurred: true,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice sends interest request to Bob
  await assertSucceeds(
    aliceDb.collection("matrimonialRequests").doc("req_1").set({
      senderId: ALICE_UID,
      receiverId: BOB_UID,
      participantIds: [ALICE_UID, BOB_UID],
      senderName: "আবদুল্লাহ (MB-1001)",
      receiverName: "ফাতেমা (MB-2002)",
      status: "PENDING",
      introMessage: "আসসালামু আলাইকুম, আমাদের পরিবার প্রস্তাবটি নিয়ে কথা বলতে আগ্রহী।",
      photoRevealApproved: false,
      waliVerified: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Messaging is BLOCKED while request is still PENDING
  await assertFails(
    aliceDb
      .collection("matrimonialRequests")
      .doc("req_1")
      .collection("messages")
      .doc("msg_1")
      .set({
        senderId: ALICE_UID,
        participantIds: [ALICE_UID, BOB_UID],
        text: "অনুমতি ছাড়া বার্তা",
        createdAt: new Date(Date.now() - 1000),
      })
  );

  // Sender Alice cannot self-approve
  await assertFails(
    aliceDb.collection("matrimonialRequests").doc("req_1").update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Receiver Bob accepts
  await assertSucceeds(
    bobDb.collection("matrimonialRequests").doc("req_1").update({
      status: "ACCEPTED",
      photoRevealApproved: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Now messaging succeeds for participants
  await assertSucceeds(
    aliceDb
      .collection("matrimonialRequests")
      .doc("req_1")
      .collection("messages")
      .doc("msg_1")
      .set({
        senderId: ALICE_UID,
        participantIds: [ALICE_UID, BOB_UID],
        text: "আসসালামু আলাইকুম, ওয়ালির সাথে যোগাযোগের সময় জানাবেন কি?",
        createdAt: new Date(Date.now() - 500),
      })
  );

  // Participant-filtered query succeeds for Bob; unauthorized reads/writes fail for Charlie
  await assertSucceeds(
    bobDb
      .collection("matrimonialRequests")
      .doc("req_1")
      .collection("messages")
      .where("participantIds", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    charlieDb
      .collection("matrimonialRequests")
      .doc("req_1")
      .collection("messages")
      .doc("msg_1")
      .get()
  );
  await assertFails(
    charlieDb
      .collection("matrimonialRequests")
      .doc("req_1")
      .collection("messages")
      .where("participantIds", "array-contains", ALICE_UID)
      .get()
  );
});
