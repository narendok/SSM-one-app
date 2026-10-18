package com.example.ssmapplication3

import com.example.ssmapplication3.feature.receiving.ReceivingDraftRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceivingDraftRulesTest {
    @Test fun remainingQuantityExcludesPriorReceivedAndRejected() =
        assertEquals(6.0, ReceivingDraftRules.remainingQuantity(10.0, 3.0, 1.0), 0.0)

    @Test fun receiptQuantityMustBePositiveAndWithinRemainingBalance() {
        assertTrue(ReceivingDraftRules.receiptQuantityIsValid("6", 6.0))
        assertFalse(ReceivingDraftRules.receiptQuantityIsValid("0", 6.0))
        assertFalse(ReceivingDraftRules.receiptQuantityIsValid("7", 6.0))
    }
}
