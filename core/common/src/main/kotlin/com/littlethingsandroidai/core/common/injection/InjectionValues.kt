package com.littlethingsandroidai.core.common.injection

object InjectionValues {
    private val values = mutableMapOf<Class<*>, Any>()

    fun <T : Any> register(type: Class<T>, component: T) {
        values[type] = component
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: Class<T>): T = values[type] as? T
        ?: error("InjectionValues: ${type.simpleName} not registered")
}
