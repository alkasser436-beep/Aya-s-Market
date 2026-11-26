const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_aya_1";
const CUSTOMER_UID = "cust_ali_1";
const OTHER_UID = "cust_sara_2";
const DRIVER_UID = "driver_rami_1";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: "ayasmarket44@gmail.com",
    email_verified: true,
  });
}

function spoofedAdminContext() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: "ayasmarket44@gmail.com",
    email_verified: false,
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

test("Unauthenticated users are blocked from all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("store_config").doc("main").get());
  await assertFails(unauth.collection("products").get());
  await assertFails(unauth.collection("orders").get());
  await assertFails(unauth.collection("users").doc(CUSTOMER_UID).collection("addresses").get());
});

test("Email spoofing & sole owner test: unverified or non-owner emails rejected; Ayasmarket44@gmail.com allowed", async () => {
  const spoof = spoofedAdminContext().firestore();
  const nonOwner = testEnv.authenticatedContext("other_admin_try", {
    email: "alkasser436@gmail.com",
    email_verified: true,
  }).firestore();
  const owner = testEnv.authenticatedContext("admin_aya_2", {
    email: "Ayasmarket44@gmail.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);
  const configDoc = {
    exchangeRateLbp: 89500,
    deliveryFeeUsd: 2.0,
    freeDeliveryThresholdUsd: 25.0,
    storeNameEn: "Aya Market",
    storeNameAr: "أيا ماركت",
    supportPhone: "+961 70 123 456",
    updatedBy: "spoof_uid",
    updatedAt: now,
  };
  await assertFails(spoof.collection("store_config").doc("main").set(configDoc));
  await assertFails(nonOwner.collection("store_config").doc("main").set(configDoc));
  await assertSucceeds(
    owner.collection("store_config").doc("main").set({
      ...configDoc,
      updatedBy: "admin_aya_2",
    })
  );
});

test("Store config & products: verified admin can manage; customer can only read", async () => {
  const adminDb = adminContext().firestore();
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    adminDb.collection("store_config").doc("main").set({
      exchangeRateLbp: 89500,
      deliveryFeeUsd: 2.0,
      freeDeliveryThresholdUsd: 25.0,
      storeNameEn: "Aya Market",
      storeNameAr: "أيا ماركت",
      supportPhone: "+961 70 123 456",
      updatedBy: ADMIN_UID,
      updatedAt: now,
    })
  );

  await assertSucceeds(custDb.collection("store_config").doc("main").get());
  await assertFails(
    custDb.collection("store_config").doc("main").update({
      exchangeRateLbp: 1500,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin creates product (NO @DocumentId `id` field!)
  await assertSucceeds(
    adminDb.collection("products").doc("prod_tomato").set({
      nameEn: "Organic Vine Tomatoes",
      nameAr: "بندورة بلدية عضوية",
      descriptionEn: "Fresh Bekaa valley tomatoes",
      descriptionAr: "بندورة طازجة من البقاع",
      category: "FRUITS_VEG",
      priceUsd: 2.5,
      discountPercent: 10,
      stock: 40,
      unitEn: "1 kg",
      unitAr: "١ كغ",
      barcode: "5281000100011",
      imageKey: "tomatoes",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject ghost `id` field or future timestamp
  await assertFails(
    adminDb.collection("products").doc("prod_bad").set({
      id: "prod_bad",
      nameEn: "Bad",
      nameAr: "خطأ",
      category: "FRUITS_VEG",
      priceUsd: 1.0,
      discountPercent: 0,
      stock: 10,
      unitEn: "1 kg",
      unitAr: "١ كغ",
      active: true,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: now,
    })
  );

  await assertSucceeds(custDb.collection("products").get());
  await assertFails(
    custDb.collection("products").doc("prod_tomato").update({
      priceUsd: 0.01,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Customer addresses: strict PII isolation per user", async () => {
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).collection("addresses").doc("addr_home").set({
      label: "Home",
      addressLine: "Hamra Street, Bldg 14",
      city: "Beirut",
      phone: "+961 71 555 222",
      notes: "Ring 3rd floor bell",
      latitude: 33.8963,
      longitude: 35.4819,
      defaultAddress: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(custDb.collection("users").doc(CUSTOMER_UID).collection("addresses").get());
  await assertFails(otherDb.collection("users").doc(CUSTOMER_UID).collection("addresses").get());
  await assertFails(otherDb.collection("users").doc(CUSTOMER_UID).collection("addresses").doc("addr_home").get());
});

test("Orders lifecycle: Customer create -> Admin assign driver -> Driver pickup & deliver -> Terminal lock", async () => {
  const adminDb = adminContext().firestore();
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  const t0 = new Date(Date.now() - 10000);

  // Customer places order
  await assertSucceeds(
    custDb.collection("orders").doc("order_1").set({
      customerUid: CUSTOMER_UID,
      customerName: "Ali Hassan",
      customerPhone: "+961 71 555 222",
      deliveryAddress: "Hamra, Beirut",
      deliveryNotes: "Call on arrival",
      latitude: 33.8963,
      longitude: 35.4819,
      currency: "LBP",
      exchangeRateAtOrder: 89500,
      paymentMethod: "CASH_ON_DELIVERY",
      subtotalUsd: 5.0,
      deliveryFeeUsd: 2.0,
      totalUsd: 7.0,
      totalLbp: 626500,
      status: "PENDING",
      items: [{ productId: "prod_tomato", nameEn: "Tomatoes", nameAr: "بندورة", quantity: 2, unitPriceUsd: 2.5 }],
      cashCollected: false,
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // Customer cannot self-approve order
  await assertFails(
    custDb.collection("orders").doc("order_1").update({
      status: "DELIVERED",
      updatedAt: new Date(Date.now() - 5000),
    })
  );

  // Unfiltered query by customer fails; filtered customerUid query succeeds
  await assertFails(custDb.collection("orders").get());
  await assertSucceeds(custDb.collection("orders").where("customerUid", "==", CUSTOMER_UID).get());
  await assertFails(otherDb.collection("orders").doc("order_1").get());

  // Admin lists all orders, moves to PREPARING & assigns driver
  await assertSucceeds(adminDb.collection("orders").get());
  await assertSucceeds(
    adminDb.collection("orders").doc("order_1").update({
      status: "PREPARING",
      driverUid: DRIVER_UID,
      driverName: "Rami Khoury",
      driverPhone: "+961 76 888 999",
      updatedAt: new Date(Date.now() - 4000),
    })
  );

  // Driver queries assigned orders
  await assertSucceeds(driverDb.collection("orders").where("driverUid", "==", DRIVER_UID).get());

  // Driver cannot jump straight to DELIVERED or tamper totalUsd
  await assertFails(
    driverDb.collection("orders").doc("order_1").update({
      status: "DELIVERED",
      cashCollected: true,
      updatedAt: new Date(Date.now() - 3000),
    })
  );
  await assertFails(
    driverDb.collection("orders").doc("order_1").update({
      status: "OUT_FOR_DELIVERY",
      totalUsd: 1.0,
      updatedAt: new Date(Date.now() - 3000),
    })
  );

  // Driver marks Picked Up -> OUT_FOR_DELIVERY
  await assertSucceeds(
    driverDb.collection("orders").doc("order_1").update({
      status: "OUT_FOR_DELIVERY",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Driver confirms cash collected & Delivered
  await assertSucceeds(
    driverDb.collection("orders").doc("order_1").update({
      status: "DELIVERED",
      cashCollected: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state locking: driver cannot mutate DELIVERED order anymore
  await assertFails(
    driverDb.collection("orders").doc("order_1").update({
      status: "OUT_FOR_DELIVERY",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Customer user profile: strict per-user creation/update isolation", async () => {
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID, {
    email: "ali@gmail.com",
    email_verified: true,
  }).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID, {
    email: "sara@gmail.com",
    email_verified: true,
  }).firestore();
  const adminDb = adminContext().firestore();
  const t0 = new Date(Date.now() - 2000);

  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).set({
      uid: CUSTOMER_UID,
      fullName: "Ali Hassan",
      email: "ali@gmail.com",
      phone: "+961 71 555 222",
      city: "Beirut",
      defaultAddressLine: "Hamra Street",
      createdAt: t0,
      updatedAt: t0,
    })
  );

  await assertSucceeds(custDb.collection("users").doc(CUSTOMER_UID).get());
  await assertSucceeds(adminDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(otherDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(
    otherDb.collection("users").doc(CUSTOMER_UID).update({
      phone: "+961 00 000 000",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_UID).update({
      uid: OTHER_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Admin-only driver onboarding: email pre-registration, UID claim, and driverEmail order access", async () => {
  const adminDb = adminContext().firestore();
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID, {
    email: "ali@gmail.com",
    email_verified: true,
  }).firestore();
  const driverDb = testEnv.authenticatedContext(DRIVER_UID, {
    email: "Driver.Rami@gmail.com",
    email_verified: true,
  }).firestore();
  const intruderDb = testEnv.authenticatedContext("intruder_1", {
    email: "intruder@gmail.com",
    email_verified: true,
  }).firestore();
  const t0 = new Date(Date.now() - 5000);

  // Non-admin cannot self-register as a driver
  await assertFails(
    intruderDb.collection("drivers").doc("drv_intruder").set({
      uid: "intruder_1",
      email: "intruder@gmail.com",
      nameEn: "Intruder",
      nameAr: "متطفل",
      phone: "+961 70 111 111",
      vehicleLabel: "Van",
      available: true,
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // Admin registers driver by email before driver logs in (uid is "")
  await assertSucceeds(
    adminDb.collection("drivers").doc("drv_rami").set({
      uid: "",
      email: "driver.rami@gmail.com",
      nameEn: "Rami Khoury",
      nameAr: "رامي خوري",
      phone: "+961 76 888 999",
      vehicleLabel: "Scooter 12",
      available: true,
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // Intruder cannot list driver's profile by email or claim it
  await assertFails(intruderDb.collection("drivers").where("email", "==", "driver.rami@gmail.com").get());
  await assertFails(
    intruderDb.collection("drivers").doc("drv_rami").update({
      uid: "intruder_1",
      updatedAt: new Date(Date.now() - 4000),
    })
  );

  // Driver queries own email roster entry & claims uid
  await assertSucceeds(driverDb.collection("drivers").where("email", "==", "driver.rami@gmail.com").get());
  await assertSucceeds(
    driverDb.collection("drivers").doc("drv_rami").update({
      uid: DRIVER_UID,
      updatedAt: new Date(Date.now() - 3000),
    })
  );

  // Driver can toggle availability after claiming uid, cannot tamper phone/email
  await assertSucceeds(
    driverDb.collection("drivers").doc("drv_rami").update({
      available: false,
      updatedAt: new Date(Date.now() - 2000),
    })
  );
  await assertFails(
    driverDb.collection("drivers").doc("drv_rami").update({
      email: "hijacked@gmail.com",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Order assigned by driverEmail (with blank driverUid) is readable/updatable by verified driver email
  await assertSucceeds(
    custDb.collection("orders").doc("order_email_1").set({
      customerUid: CUSTOMER_UID,
      customerName: "Ali Hassan",
      customerPhone: "+961 71 555 222",
      deliveryAddress: "Hamra, Beirut",
      latitude: 33.8963,
      longitude: 35.4819,
      currency: "USD",
      exchangeRateAtOrder: 89500,
      paymentMethod: "CASH_ON_DELIVERY",
      subtotalUsd: 10.0,
      deliveryFeeUsd: 2.0,
      totalUsd: 12.0,
      totalLbp: 1074000,
      status: "PENDING",
      items: [{ productId: "prod_tomato", nameEn: "Tomatoes", nameAr: "بندورة", quantity: 4, unitPriceUsd: 2.5 }],
      cashCollected: false,
      createdAt: t0,
      updatedAt: t0,
    })
  );
  await assertSucceeds(
    adminDb.collection("orders").doc("order_email_1").update({
      status: "PREPARING",
      driverUid: "",
      driverEmail: "driver.rami@gmail.com",
      driverName: "Rami Khoury",
      driverPhone: "+961 76 888 999",
      updatedAt: new Date(Date.now() - 2500),
    })
  );
  await assertSucceeds(driverDb.collection("orders").where("driverEmail", "==", "driver.rami@gmail.com").get());
  await assertFails(intruderDb.collection("orders").where("driverEmail", "==", "driver.rami@gmail.com").get());
  await assertSucceeds(
    driverDb.collection("orders").doc("order_email_1").update({
      status: "OUT_FOR_DELIVERY",
      updatedAt: new Date(Date.now() - 1500),
    })
  );
});

