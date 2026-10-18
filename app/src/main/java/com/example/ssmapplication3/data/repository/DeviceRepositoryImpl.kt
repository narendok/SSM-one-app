package com.example.ssmapplication3.data.repository

import com.example.ssmapplication3.domain.repository.DeviceRepository

/** No verified device registration/revocation contract is present in the handoff. */
class DeviceRepositoryImpl : DeviceRepository {
    override suspend fun registerDevice(userId: String, deviceMetadata: Map<String, String>): Result<Boolean> =
        Result.failure(UnsupportedOperationException("Device registration contract is not available."))

    override suspend fun checkDeviceRevoked(deviceId: String): Result<Boolean> =
        Result.failure(UnsupportedOperationException("Device revocation contract is not available."))
}
