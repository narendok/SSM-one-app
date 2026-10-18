package com.example.ssmapplication3.feature.documents

import androidx.compose.runtime.Composable
import com.example.ssmapplication3.feature.workspace.DepartmentDriveScreen
import com.example.ssmapplication3.feature.workspace.DepartmentDriveViewModel

/** All engineering entry points share department/project scoping and stale-response protection. */
@Composable
fun EngineeringDocumentsScreen(
    viewModel: DepartmentDriveViewModel,
    department: String,
    departmentId: String?,
    projectId: String? = null
) {
    DepartmentDriveScreen(viewModel, department, departmentId, projectId)
}
