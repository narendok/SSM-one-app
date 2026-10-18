package com.example.ssmapplication3.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseRequestItem(
    val id: String,
    @SerialName("request_number") val prNumber: String,
    val requesterId: String? = null,
    val departmentId: String? = null,
    val projectId: String? = null,
    val status: String,
    val priority: String = "NORMAL",
    val requiredDate: String? = null,
    val createdDate: String? = null
)

@Serializable
data class PrLineItem(
    val id: String,
    @SerialName("purchase_request_id") val prId: String,
    @SerialName("component_id") val componentId: String? = null,
    val mpn: String? = null,
    val description: String? = null,
    @SerialName("required_quantity") val requestedQuantity: Double,
    @SerialName("shortage_quantity") val shortageQuantity: Double? = null,
    val uom: String = "PCS",
    val requiredDate: String? = null,
    val notes: String? = null
)

@Serializable
data class RfqItem(
    val id: String,
    @SerialName("rfq_number") val rfqNumber: String,
    val title: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("purchase_request_id") val prId: String? = null,
    val status: String,
    @SerialName("response_due_date") val dueDate: String? = null,
    val invitedVendorCount: Int = 0,
    val respondedVendorCount: Int = 0
)

@Serializable
data class PurchaseOrderItem(
    val id: String,
    @SerialName("po_number") val poNumber: String,
    val vendorId: String? = null,
    val vendorName: String? = null,
    val poRevision: String = "Rev 0",
    val status: String,
    val orderDate: String? = null,
    @SerialName("expected_delivery_date") val expectedDeliveryDate: String? = null,
    val shipToAddress: String? = null,
    val acknowledgementStatus: String = "PENDING"
)

@Serializable
data class PoLineItem(
    val id: String,
    val poId: String,
    val lineNumber: Int,
    val internalPartNumber: String? = null,
    val mpn: String? = null,
    val description: String? = null,
    val orderedQuantity: Double,
    val receivedQuantity: Double = 0.0,
    val openQuantity: Double,
    val uom: String = "PCS",
    val expectedDeliveryDate: String? = null
)

@Serializable
data class SupplierAsnItem(
    val id: String,
    val asnNumber: String,
    val poNumber: String,
    val vendorName: String? = null,
    val shipmentDate: String? = null,
    val expectedArrivalDate: String? = null,
    val carrierName: String? = null,
    val trackingNumber: String? = null,
    val status: String
)

@Serializable
data class VendorItem(
    val id: String,
    @SerialName("vendor_code") val vendorCode: String,
    @SerialName("name") val vendorName: String,
    val isActive: Boolean = true,
    @SerialName("supplier_status") val status: String,
    @SerialName("quality_risk") val qualityRisk: String? = null
)
