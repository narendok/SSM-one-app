package com.example.ssmapplication3.core.security

import android.content.Context
import android.os.Build
import com.example.ssmapplication3.domain.repository.DeviceRepository
import java.util.UUID

class DeviceRegistrationManager(
    private val context: Context,
    private val deviceRepository: DeviceRepository
) {

    suspend fun registerCurrentDevice(userId: String): Result<Boolean> {
        val deviceId = getOrCreateDeviceId()
        val metadata = mapOf(
            "device_id" to deviceId,
            "platform" to "ANDROID",
            "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "os_version" to "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            "app_version" to "1.0.0",
            "status" to "ACTIVE"
        )
        return deviceRepository.registerDevice(userId, metadata)
    }

    suspend fun isDeviceRevoked(): Result<Boolean> {
        val deviceId = getOrCreateDeviceId()
        return deviceRepository.checkDeviceRevoked(deviceId)
    }

    private fun getOrCreateDeviceId(): String {
        val prefs = context.getSharedPreferences("ssm_device_prefs", Context.MODE_PRIVATE)
        var deviceId = prefs.getString("device_id", null)
        if (deviceId.isNullOrEmpty()) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", deviceId).apply()
        }
        return deviceId
    }
}
