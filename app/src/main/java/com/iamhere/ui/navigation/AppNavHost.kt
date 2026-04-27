package com.iamhere.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iamhere.ui.screen.chat.ChatScreen
import com.iamhere.ui.screen.contacts.ContactsScreen
import com.iamhere.ui.screen.home.HomeScreen
import com.iamhere.ui.screen.settings.SettingsScreen
import com.iamhere.ui.screen.splash.SplashScreen

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "splash") {
        composable("splash") { SplashScreen { nav.navigate("home") } }
        composable("home") { HomeScreen(onChat = { nav.navigate("chat/${'$'}it") }, onContacts = { nav.navigate("contacts") }, onSettings = { nav.navigate("settings") }) }
        composable("chat/{id}") { ChatScreen() }
        composable("contacts") { ContactsScreen() }
        composable("settings") { SettingsScreen() }
    }
}
