package com.example.ssmapplication3.feature.receiving

/** Local validation only. A valid draft still requires an online server posting to become a GRN. */
object ReceivingDraftRules {
    fun receiptQuantityIsValid(value: String, remaining: Double): Boolean =
        value.toIntOrNull()?.let { it > 0 && it <= remaining } == true

    fun remainingQuantity(ordered: Double?, received: Double?, rejected: Double?): Double =
        ((ordered ?: 0.0) - (received ?: 0.0) - (rejected ?: 0.0)).coerceAtLeast(0.0)
}
