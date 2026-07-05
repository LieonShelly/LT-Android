package com.littlethingsandroidai.domain.coordinator

interface Coordinator {
    fun push(route: Route)
    fun pop()
    fun popToRoot()
}
