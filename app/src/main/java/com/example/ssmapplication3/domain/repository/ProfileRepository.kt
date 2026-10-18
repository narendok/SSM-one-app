package com.example.ssmapplication3.domain.repository

import com.example.ssmapplication3.domain.model.EmployeeRecord
import com.example.ssmapplication3.domain.model.UserProfile

interface ProfileRepository {
    suspend fun fetchUserProfile(userId: String): Result<UserProfile>
    suspend fun fetchEmployeeRecord(userId: String): Result<EmployeeRecord?>
}
