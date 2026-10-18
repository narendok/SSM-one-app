package com.example.ssmapplication3.navigation

import com.example.ssmapplication3.core.security.CapabilitiesManager
import com.example.ssmapplication3.domain.model.UserProfile
import java.util.Locale

/** Navigation presentation only; backend RLS remains authoritative. */
object WorkspacePolicy {
    fun category(name: String): String {
        val normalized = name.trim().lowercase(Locale.ROOT)
        // The backend now uses canonical department codes (for example,
        // "PROC — Procurement, Stores & Incoming Quality"). Keep mobile
        // presentation aligned without treating the label as authorization.
        when (normalized.substringBefore("—").trim()) {
            "rnd" -> return "engineering"
            "proc" -> return "procurement"
            "prod" -> return "production"
            "fac" -> return "facility"
            "ops" -> return "operations"
            "sal" -> return "sales"
            "hr" -> return "hr"
            "fin" -> return "finance"
            "adm" -> return "administration"
        }
        return when (normalized) {
        "hw", "hardware", "hardware & r&d", "embedded hardware", "hardware engineering", "engineering", "r&d", "research & development", "research and development", "firmware", "mechanical", "mechanical engineering", "software", "software engineering" -> "engineering"
        "hr", "human resources", "human resource" -> "hr"
        "purchase", "purchasing", "procurement", "procurement, stores & incoming quality" -> "procurement"
        "stores", "store" -> "stores"
        "production", "engineering production", "production & calibration" -> "production"
        "quality", "incoming quality", "qa" -> "quality"
        "security" -> "security"
        "facility", "facilities", "maintenance", "electrical", "utilities", "facility, maintenance, security, utilities & assets" -> "facility"
        "operations & qms" -> "operations"
        "finance" -> "finance"
        "qms", "quality management system" -> "qms"
        "calibration" -> "calibration"
            else -> normalized
        }
    }

    /** Department navigation presentation only; BOM reads still require backend RLS. */
    fun showsProjectBoms(department: String): Boolean =
        category(department) in setOf("engineering", "procurement", "stores", "production")

    fun allows(profile: UserProfile, department: String, route: String): Boolean {
        if (!CapabilitiesManager.isInternalEmployee(profile)) return false
        val group = category(department)
        fun has(vararg keys: String) = CapabilitiesManager.hasAnyCapability(profile, keys.toList())
        return when (route) {
            Screen.MyDay.route, Screen.MyWork.route, Screen.Notifications.route, Screen.DepartmentWork.route, Screen.Search.route, Screen.Drive.route, Screen.More.route -> true
            Screen.Projects.route, Screen.ProjectDetail.route, Screen.ProjectDocuments.route, Screen.EngineeringDocuments.route -> group == "engineering" && has("projects.view", "projects.manage")
            // Presentation follows the verified least-privilege backend model. RLS remains
            // authoritative and can still return no records for an otherwise visible feature.
            Screen.Inventory.route, Screen.Scanner.route -> group in setOf("engineering", "procurement", "stores", "production", "quality") &&
                has("inventory.view", "inventory.manage")
            Screen.Procurement.route -> group in setOf("engineering", "procurement", "stores") && has("procurement.view", "procurement.manage")
            Screen.ReceivingDraft.route -> group in setOf("procurement", "stores") && has("procurement.view", "procurement.manage", "inventory.manage")
            Screen.MaterialIssue.route, Screen.MaterialReturns.route, Screen.StockAdjustment.route, Screen.StockTransfer.route -> group in setOf("procurement", "stores") && has("inventory.manage")
            Screen.HrEmployees.route -> group == "hr"
            Screen.Assets.route -> group in setOf("facility", "production", "operations", "engineering") || "admin" in profile.roles
            Screen.PurchaseDraftReview.route -> group in setOf("procurement", "stores", "engineering") && has("procurement.view", "procurement.manage", "inventory.manage")
            else -> false
        }
    }
}
