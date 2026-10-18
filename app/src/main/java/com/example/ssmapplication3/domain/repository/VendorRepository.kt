package com.example.ssmapplication3.domain.repository

import com.example.ssmapplication3.core.ui.ResourceResult
import com.example.ssmapplication3.domain.model.VendorItem

interface VendorRepository {
    suspend fun fetchVendors(query: String? = null): ResourceResult<List<VendorItem>>
    suspend fun fetchVendorDetail(vendorId: String): ResourceResult<VendorItem>
}
