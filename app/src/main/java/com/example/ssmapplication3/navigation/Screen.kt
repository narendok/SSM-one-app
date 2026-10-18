package com.example.ssmapplication3.navigation

sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Login")
    object MyDay : Screen("my_day", "Dashboard")
    object DepartmentWork : Screen("department_work", "Work")
    object Search : Screen("search", "Search")
    object Drive : Screen("drive", "Drive")
    object Projects : Screen("projects", "Projects")
    object ProjectDetail : Screen("project/{id}", "Project")
    object ProjectDocuments : Screen("project/{id}/documents", "Project documents")
    object HrEmployees : Screen("hr_employees", "HR Employees")
    object EngineeringDocuments : Screen("engineering_documents", "Documents")
    object MyWork : Screen("my_work", "My Work")
    object Scanner : Screen("scanner", "Scanner")
    object Notifications : Screen("notifications", "Notifications")
    object More : Screen("more", "More")
    object Procurement : Screen("procurement", "Procurement")
    object ReceivingDraft : Screen("receiving_draft", "Receiving")
    object Inventory : Screen("inventory", "Inventory")
    object MaterialReturns : Screen("material_returns", "Returns")
    object MaterialIssue : Screen("material_issue", "Issue")
    object StockAdjustment : Screen("stock_adjustment", "Adjustment")
    object StockTransfer : Screen("stock_transfer", "Transfer")
}


