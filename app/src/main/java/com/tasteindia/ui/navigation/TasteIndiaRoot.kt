package com.tasteindia.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tasteindia.ui.details.DetailsRoute
import com.tasteindia.ui.details.DetailsViewModel
import com.tasteindia.ui.favourites.FavouritesRoute
import com.tasteindia.ui.favourites.FavouritesViewModel
import com.tasteindia.ui.recipes.RecipesRoute
import com.tasteindia.ui.recipes.RecipesViewModel

private data class TopLevel(val route: String, val label: String, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevel(Destinations.RECIPES, "Recipes", Icons.Default.Home),
    TopLevel(Destinations.FAVOURITES, "Favourites", Icons.Default.Favorite),
)

@Composable
fun TasteIndiaRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (topLevelDestinations.any { it.route == currentRoute }) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    // Save/restore each tab so list context (filters, scroll) survives tab switches.
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.RECIPES,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destinations.RECIPES) {
                val vm: RecipesViewModel = viewModel(factory = AppViewModels.Recipes)
                RecipesRoute(vm, onOpenMeal = { navController.navigate(Destinations.details(it)) })
            }
            composable(Destinations.FAVOURITES) {
                val vm: FavouritesViewModel = viewModel(factory = AppViewModels.Favourites)
                FavouritesRoute(vm, onOpenMeal = { navController.navigate(Destinations.details(it)) })
            }
            composable(
                route = Destinations.DETAILS,
                arguments = listOf(navArgument(Destinations.ARG_MEAL_ID) { type = NavType.StringType }),
            ) {
                val vm: DetailsViewModel = viewModel(factory = AppViewModels.Details)
                DetailsRoute(vm, onBack = { navController.navigateUp() })
            }
        }
    }
}
