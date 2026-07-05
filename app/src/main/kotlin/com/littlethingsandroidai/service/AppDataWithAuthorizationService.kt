package com.littlethingsandroidai.service

import com.littlethingsandroidai.service.auth.repository.AuthRepository
import com.littlethingsandroidai.service.auth.usecase.AuthUseCase
import com.littlethingsandroidai.service.auth.usecase.AuthUseCaseType
import com.littlethingsandroidai.service.reflection.repository.ReflectionRepository
import com.littlethingsandroidai.service.reflection.usecase.CalendarReflectionsUseCase
import com.littlethingsandroidai.service.reflection.usecase.CalendarReflectionsUseCaseType

interface PlaceholderUseCase {
    suspend fun execute()
}

class NotImplementedPlaceholderUseCase(
    private val useCaseName: String,
) : PlaceholderUseCase {
    override suspend fun execute() {
        throw NotImplementedError("$useCaseName is not implemented yet")
    }
}

interface AppDataWithAuthorizationServiceful {
    val authUseCase: AuthUseCaseType
    val calendarReflectionsUseCase: CalendarReflectionsUseCaseType
    val submitAnswerUseCase: PlaceholderUseCase
    val fetchCategoriesUseCase: PlaceholderUseCase
    val fetchTodayQuestionsUseCase: PlaceholderUseCase
}

class AppDataWithAuthorizationService(
    private val authRepository: AuthRepository,
    private val reflectionRepository: ReflectionRepository,
) : AppDataWithAuthorizationServiceful {

    override val authUseCase: AuthUseCaseType by lazy {
        AuthUseCase(repository = authRepository)
    }

    override val calendarReflectionsUseCase: CalendarReflectionsUseCaseType by lazy {
        CalendarReflectionsUseCase(repository = reflectionRepository)
    }

    override val submitAnswerUseCase: PlaceholderUseCase by lazy {
        NotImplementedPlaceholderUseCase(useCaseName = "SubmitAnswerUseCase")
    }

    override val fetchCategoriesUseCase: PlaceholderUseCase by lazy {
        NotImplementedPlaceholderUseCase(useCaseName = "FetchCategoriesUseCase")
    }

    override val fetchTodayQuestionsUseCase: PlaceholderUseCase by lazy {
        NotImplementedPlaceholderUseCase(useCaseName = "FetchTodayQuestionsUseCase")
    }
}
