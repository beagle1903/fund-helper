package com.burha.fundhelper.ui

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.burha.fundhelper.ui.detail.DetailScreen
import com.burha.fundhelper.ui.detail.DetailViewModel
import com.burha.fundhelper.ui.search.SearchScreen
import com.burha.fundhelper.ui.search.SearchViewModel
import com.burha.fundhelper.ui.watchlist.WatchlistScreen
import com.burha.fundhelper.ui.watchlist.WatchlistViewModel

object Routes {
    const val WATCHLIST = "watchlist"
    const val SEARCH = "search"
    const val DETAIL = "detail/{fundCode}"
    fun detail(code: String) = "detail/$code"
}

@Composable
fun FundHelperNav() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.WATCHLIST) {
        composable(Routes.WATCHLIST) {
            val vm: WatchlistViewModel = hiltViewModel()
            WatchlistScreen(
                viewModel = vm,
                onSearch = { navController.navigate(Routes.SEARCH) },
                onOpen = { code -> navController.navigate(Routes.detail(code)) },
            )
        }
        composable(Routes.SEARCH) {
            val vm: SearchViewModel = hiltViewModel()
            SearchScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpen = { code -> navController.navigate(Routes.detail(code)) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("fundCode") { type = NavType.StringType }),
        ) {
            val vm: DetailViewModel = hiltViewModel()
            DetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}