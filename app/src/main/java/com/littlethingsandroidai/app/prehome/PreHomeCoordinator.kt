package com.littlethingsandroidai.app.prehome

import androidx.navigation.NavController
import com.littlethingsandroidai.R
import com.littlethingsandroidai.domain.coordinator.Coordinator
import com.littlethingsandroidai.domain.coordinator.PreHomeRoute
import com.littlethingsandroidai.domain.coordinator.Route

class PreHomeCoordinator(
    private val navController: NavController,
) : Coordinator {
    override fun push(route: Route) {
        if (route !is PreHomeRoute) return

        when (route) {
            PreHomeRoute.LOGIN -> {
                if (navController.currentDestination?.id != R.id.signInFragment) {
                    navController.navigate(R.id.signInFragment)
                }
            }

            PreHomeRoute.SPLASH,
            PreHomeRoute.ONBOARDING,
            PreHomeRoute.WELCOME,
            PreHomeRoute.FIRST_QUESTION
            -> Unit
        }
    }

    override fun pop() {
        navController.popBackStack()
    }

    override fun popToRoot() {
        val startDestinationId = navController.graph.startDestinationId
        navController.popBackStack(startDestinationId, false)
    }
}
