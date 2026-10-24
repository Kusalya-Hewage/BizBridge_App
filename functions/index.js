const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Trigger: When a new booking is created.
 * Sends notifications to the business owner and logs reference.
 */
exports.onBookingCreated = functions.firestore
  .document("bookings/{bookingId}")
  .onCreate(async (snap, context) => {
    const booking = snap.data();
    const businessId = booking.businessId;

    try {
      const bizSnap = await db.collection("businesses").document(businessId).get();
      if (bizSnap.exists) {
        const ownerId = bizSnap.data().ownerId;
        if (ownerId) {
          await db.collection("notifications").add({
            userId: ownerId,
            title: "New Booking Request",
            message: `New appointment request for ${booking.serviceNameSnapshot} on ${booking.appointmentDate} at ${booking.appointmentTime}.`,
            category: "BOOKING",
            relatedBookingId: context.params.bookingId,
            relatedBusinessId: businessId,
            isRead: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
          });
        }
      }
    } catch (e) {
      console.error("Error in onBookingCreated:", e);
    }
  });

/**
 * Trigger: When a review is submitted.
 * Recalculates average rating and Trust Score on trusted backend.
 */
exports.onReviewSubmitted = functions.firestore
  .document("reviews/{reviewId}")
  .onCreate(async (snap, context) => {
    const review = snap.data();
    const businessId = review.businessId;

    try {
      const allReviewsSnap = await db.collection("reviews")
        .where("businessId", "==", businessId)
        .where("moderationStatus", "==", "APPROVED")
        .get();

      let totalRating = 0;
      let count = allReviewsSnap.size;

      allReviewsSnap.forEach((doc) => {
        totalRating += doc.data().rating || 5;
      });

      const avgRating = count > 0 ? parseFloat((totalRating / count).toFixed(1)) : 5.0;

      // Calculate Trust Score
      const bizRef = db.collection("businesses").document(businessId);
      const bizSnap = await bizRef.get();
      if (bizSnap.exists) {
        const bizData = bizSnap.data();
        let trustScore = 80;
        if (bizData.approved) trustScore += 10;
        if (bizData.verificationStatus === "VERIFIED") trustScore += 5;
        if (count >= 5 && avgRating >= 4.5) trustScore += 5;
        trustScore = Math.min(100, trustScore);

        await bizRef.update({
          rating: avgRating,
          reviewCount: count,
          trustScore: trustScore,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
    } catch (e) {
      console.error("Error updating review stats:", e);
    }
  });

/**
 * Privileged: Set Admin Custom Claims
 * Grants admin privilege only to verified corporate administrator email.
 */
exports.setAdminClaim = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
  }

  const callerEmail = context.auth.token.email;
  const targetEmail = data.email;

  // Only alex@bizbridge.example or existing admin can grant admin claim
  if (callerEmail !== "alex@bizbridge.example") {
    throw new functions.https.HttpsError("permission-denied", "Unauthorized caller.");
  }

  const user = await admin.auth().getUserByEmail(targetEmail);
  await admin.auth().setCustomUserClaims(user.uid, { admin: true });
  await db.collection("users").document(user.uid).update({ role: "ADMIN" });

  return { success: true, message: `Admin custom claim granted to ${targetEmail}` };
});
