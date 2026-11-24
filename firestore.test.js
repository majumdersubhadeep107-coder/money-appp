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

function validUserAccountPayload(uid) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    displayName: "PaiseJar User",
    dataConsentAccepted: true,
    consentVersion: "2026-11-v1",
    providerConnectionStatus: "NOT_CONNECTED",
    notificationsEnabled: true,
    weeklyDigestEnabled: false,
    roundUpTargetRupees: 100,
    maxPerPurchaseRupees: 50,
    weeklyCapRupees: 500,
    monthlyCapRupees: 1500,
    minTransactionAmountRupees: 50,
    excludedCategories: ["SUBSCRIPTIONS"],
    excludedMerchants: [],
    paused: false,
    selectedGoalId: "goal_emergency",
    monthlyGoalTargetRupees: 1000,
    createdAt: now,
    updatedAt: now,
  };
}

function validGoalPayload(uid) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    name: "Emergency fund",
    targetAmountRupees: 10000,
    confirmedBalanceRupees: 0,
    pendingTransferRupees: 0,
    failedTransferCount: 0,
    targetDateLabel: "Jun 2027",
    iconKey: "shield",
    archived: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validTxPayload(uid, idempotencyKey = "idem_tx_1") {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    idempotencyKey,
    sourceType: "SANDBOX_PREVIEW",
    merchantName: "Third Wave Coffee",
    rawMerchantLabel: "TWC*IND*BLR",
    description: "Iced Americano",
    category: "CAFE",
    purchaseAmountRupees: 180,
    purchaseEpochMillis: Date.now() - 3600000,
    weekOfMonth: 4,
    currentMonth: true,
    dateRangeBucket: "THIS_WEEK",
    eventKind: "PURCHASE",
    transferInstructionStatus: "SIMULATED_PREVIEW",
    transferConfirmationStatus: "UNCONFIRMED_NO_PARTNER",
    userExcluded: false,
    providerReference: "SANDBOX-001",
    createdAt: now,
    updatedAt: now,
  };
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

test("Unauthenticated requests are denied across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).set(validUserAccountPayload(ALICE_UID)));
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("goals").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Authenticated user can create and partially update own account, but cannot spoof partner connection or extra keys", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = validUserAccountPayload(ALICE_UID);
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).set(payload));

  // Partial update allowed
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      roundUpTargetRupees: 50,
      paused: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject spoofing providerConnectionStatus
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      providerConnectionStatus: "CONNECTED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject monthlyCap < weeklyCap
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      weeklyCapRupees: 2000,
      monthlyCapRupees: 1000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field on create
  await assertFails(
    alice.collection("users").doc(ALICE_UID + "_ghost").set({
      ...validUserAccountPayload(ALICE_UID + "_ghost"),
      isAdmin: true,
    })
  );
});

test("Cross-user access is strictly blocked", async () => {
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bob.collection("users").doc(BOB_UID).set(validUserAccountPayload(BOB_UID)));
  await assertSucceeds(
    bob.collection("users").doc(BOB_UID).collection("goals").doc("g1").set(validGoalPayload(BOB_UID))
  );

  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(alice.collection("users").doc(BOB_UID).get());
  await assertFails(alice.collection("users").doc(BOB_UID).collection("goals").get());
  await assertFails(alice.collection("users").doc(BOB_UID).collection("goals").doc("g1").delete());
});

test("Goals enforce confirmedBalanceRupees == 0 and allow archive/rename updates", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("goals").doc("goal_1").set(validGoalPayload(ALICE_UID))
  );

  // Reject non-zero confirmedBalanceRupees
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("goals").doc("goal_2").set({
      ...validGoalPayload(ALICE_UID),
      confirmedBalanceRupees: 500,
    })
  );

  // Allow rename & archive
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("goals").doc("goal_1").update({
      name: "Medical Buffer",
      archived: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Transactions enforce SANDBOX_PREVIEW sourceType, forbid @DocumentId key 'id', and allow exclude toggle", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set(validTxPayload(ALICE_UID))
  );

  // Reject live bank feed spoofing
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_2").set({
      ...validTxPayload(ALICE_UID, "idem_2"),
      sourceType: "LIVE_BANK_FEED",
    })
  );

  // Reject extra 'id' field
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_3").set({
      ...validTxPayload(ALICE_UID, "idem_3"),
      id: "tx_3",
    })
  );

  // Allow userExcluded toggle
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({
      userExcluded: true,
      transferInstructionStatus: "CANCELLED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // List query succeeds for owner
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
});

test("Audit logs and support tickets enforce append-only owner creation", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("auditLogs").doc("log_1").set({
      userId: ALICE_UID,
      action: "RULES_UPDATED",
      detail: "Changed round-up increment to ₹100",
      createdAt: now,
    })
  );

  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("auditLogs").doc("log_1").update({
      action: "TAMPERED",
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("supportTickets").doc("t_1").set({
      userId: ALICE_UID,
      topic: "Partner timeline",
      message: "When will UPI Autopay partners be added?",
      status: "OPEN",
      createdAt: now,
    })
  );
});
