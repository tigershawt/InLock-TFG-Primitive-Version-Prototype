package com.jetbrains.kmpapp.navigation

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import com.jetbrains.kmpapp.data.UserData
import com.jetbrains.kmpapp.data.UserRole
import com.jetbrains.kmpapp.screens.admin.AdminScreen
import com.jetbrains.kmpapp.screens.home.HomeScreen
import com.jetbrains.kmpapp.screens.manufacturer.ManufacturerDashboardScreenCommon


object RoleNavigation {


    fun hasRole(userData: UserData?, requiredRole: UserRole): Boolean {
        if (userData == null) return false

        return when (requiredRole) {
            UserRole.USER -> true
            UserRole.MANUFACTURER -> userData.role == UserRole.MANUFACTURER || userData.role == UserRole.ADMIN
            UserRole.ADMIN -> userData.role == UserRole.ADMIN
        }
    }

    fun getHomeScreenForRole(userData: UserData?): Screen {
        return when {
            userData?.role == UserRole.ADMIN -> HomeScreen
            userData?.role == UserRole.MANUFACTURER -> HomeScreen
            else -> HomeScreen
        }
    }

    fun navigateIfPermitted(
        navigator: Navigator,
        userData: UserData?,
        requiredRole: UserRole,
        destination: Screen
    ) {
        if (hasRole(userData, requiredRole)) {
            navigator.push(destination)
        }
    }

    fun getAccessibleScreens(userData: UserData?): List<RoleScreen> {
        val screens = mutableListOf<RoleScreen>()

        screens.add(RoleScreen("Home", UserRole.USER, HomeScreen))

        if (hasRole(userData, UserRole.MANUFACTURER)) {
            screens.add(RoleScreen("Manufacturer Dashboard", UserRole.MANUFACTURER, ManufacturerDashboardScreenCommon()))
        }

        if (hasRole(userData, UserRole.ADMIN)) {
            screens.add(RoleScreen("Admin Panel", UserRole.ADMIN, AdminScreen()))
        }

        return screens
    }
}

data class RoleScreen(
    val title: String,
    val requiredRole: UserRole,
    val screen: Screen
)