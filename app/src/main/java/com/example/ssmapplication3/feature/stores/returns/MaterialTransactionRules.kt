package com.example.ssmapplication3.feature.stores.returns

fun outstandingQuantity(issued: Int, returned: Int): Int = (issued - returned).coerceAtLeast(0)
fun returnQuantityIsValid(value: Int, issued: Int, returned: Int): Boolean = value > 0 && value <= outstandingQuantity(issued, returned)
fun issueLineIsValid(componentId: String, locationId: String, quantity: Int): Boolean = componentId.isNotBlank() && locationId.isNotBlank() && quantity > 0
