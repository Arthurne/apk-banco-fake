package com.purplebank.simulator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.purplebank.simulator.ui.Loading
import com.purplebank.simulator.ui.MainViewModel
import com.purplebank.simulator.ui.screens.*
import com.purplebank.simulator.ui.theme.BankColors
import com.purplebank.simulator.ui.theme.PurpleBankTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { PurpleBankApp() }
    }
}
private data class Tab(val route: String, val label: String, val icon: ImageVector)
private val tabs = listOf(
    Tab("home", "Início", Icons.Filled.Home),
    Tab("account", "Conta", Icons.Filled.AccountBalanceWallet),
    Tab("card", "Cartão", Icons.Filled.CreditCard),
    Tab("history", "Atividade", Icons.Filled.ReceiptLong),
    Tab("settings", "Ajustes", Icons.Filled.Settings)
)
@Composable
fun PurpleBankApp(vm: MainViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    val card by vm.card.collectAsStateWithLifecycle()
    val loaded = settings != null && profile != null && account != null && card != null
    val dark = when (settings?.theme) { "DARK" -> true; "LIGHT" -> false; else -> isSystemInDarkTheme() }
    val primary = remember(settings?.primaryColor) { BankColors.parse(settings?.primaryColor) }
    PurpleBankTheme(primary, dark) {
        val nav = rememberNavController()
        val snack = remember { SnackbarHostState() }
        LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }
        val route = nav.currentBackStackEntryAsState().value?.destination?.route
        fun goTab(r: String) = nav.navigate(r) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        Scaffold(
            snackbarHost = { SnackbarHost(snack) },
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (route == null || tabs.any { it.route == route }) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = androidx.compose.ui.unit.Dp(0f)) {
                        tabs.forEach { t ->
                            NavigationBarItem(
                                selected = route == t.route, onClick = { goTab(t.route) },
                                icon = { Icon(t.icon, contentDescription = t.label) },
                                label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { pad ->
            if (!loaded) Loading()
            else NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
                composable("home") { DashboardScreen(vm, ::goTab) { nav.navigate(it) } }
                composable("account") { AccountScreen(vm) { nav.navigate(it) } }
                composable("card") { CardScreen(vm) { nav.navigate(it) } }
                composable("history") { HistoryScreen(vm) }
                composable("settings") { SettingsScreen(vm) { nav.navigate(it) } }
                composable("admin") { AdminScreen(vm, onBack = { nav.popBackStack() }, onOpen = { goTab(it) }) }
                composable("invoice/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    InvoiceScreen(vm, it.arguments?.getLong("id") ?: 0L) { nav.popBackStack() }
                }
            }
        }
    }
}
