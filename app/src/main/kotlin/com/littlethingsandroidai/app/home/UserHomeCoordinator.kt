package com.littlethingsandroidai.app.home

import com.littlethingsandroidai.domain.coordinator.Coordinator
import com.littlethingsandroidai.domain.coordinator.Route

class UserHomeCoordinator : Coordinator {
    override fun push(route: Route) = Unit

    override fun pop() = Unit

    override fun popToRoot() = Unit
}
