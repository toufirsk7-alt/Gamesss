const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-apexrace-5e108195-fbdf-4e38-b74b-10de7097086d";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      databaseId: DATABASE_ID,
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085,
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

test("unauthenticated read / write fails on users", async () => {
  const unauthContext = testEnv.unauthenticatedContext();
  const db = unauthContext.firestore();
  await assertFails(db.collection("users").doc("user1").get());
  await assertFails(db.collection("users").doc("user1").set({
    userId: "user1",
    displayName: "Racer",
    careerLevel: 1,
    cash: 50000,
    reputation: 100,
    selectedCarId: "car1",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("authenticated user can create and read their own profile", async () => {
  const aliceContext = testEnv.authenticatedContext("alice");
  const db = aliceContext.firestore();
  const userRef = db.collection("users").doc("alice");

  await assertSucceeds(userRef.set({
    userId: "alice",
    displayName: "SpeedDemon",
    careerLevel: 1,
    cash: 50000,
    reputation: 100,
    selectedCarId: "car_m4",
    createdAt: new Date(),
    updatedAt: new Date()
  }));

  await assertSucceeds(userRef.get());
});

test("authenticated user cannot access another user profile", async () => {
  const bobContext = testEnv.authenticatedContext("bob");
  const db = bobContext.firestore();
  await assertFails(db.collection("users").doc("alice").get());
});

test("user can add and tune car in garage", async () => {
  const aliceContext = testEnv.authenticatedContext("alice");
  const db = aliceContext.firestore();
  const carRef = db.collection("users").doc("alice").collection("garage").doc("car1");

  await assertSucceeds(carRef.set({
    id: "car1",
    userId: "alice",
    carModelKey: "bmw_m4",
    name: "Apex M4",
    isCustomBuild: false,
    paintColorHex: "#00E5FF",
    engineLevel: 1,
    turboLevel: 0,
    suspensionLevel: 1,
    transmissionLevel: 1,
    brakesLevel: 1,
    nitroLevel: 1,
    spoilerType: 0,
    bodyKitType: 0,
    underglowColorHex: "#FF0055",
    windowTintLevel: 20,
    wheelType: 1,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});
