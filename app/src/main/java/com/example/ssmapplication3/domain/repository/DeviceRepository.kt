package com.example.ssmapplication3.domain.repository

interface DeviceRepository {
    suspend fun registerDevice(userId: String, deviceMetadata: Map<String, String>): Result<Boolean>
    suspend fun checkDeviceRevoked(deviceId: String): Result<Boolean>
}
