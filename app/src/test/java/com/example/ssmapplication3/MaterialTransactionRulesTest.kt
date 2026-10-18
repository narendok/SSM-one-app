package com.example.ssmapplication3

import com.example.ssmapplication3.feature.stores.returns.issueLineIsValid
import com.example.ssmapplication3.feature.stores.returns.outstandingQuantity
import com.example.ssmapplication3.feature.stores.returns.returnQuantityIsValid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialTransactionRulesTest {
    @Test fun outstandingIsClamped() { assertEquals(0, outstandingQuantity(2, 4)) }
    @Test fun returnCannotExceedOutstanding() { assertTrue(returnQuantityIsValid(2, 5, 3)); assertFalse(returnQuantityIsValid(3, 5, 3)) }
    @Test fun issueLineNeedsIdsAndPositiveQuantity() { assertTrue(issueLineIsValid("component", "location", 1)); assertFalse(issueLineIsValid("", "location", 1)); assertFalse(issueLineIsValid("component", "location", 0)) }
}
