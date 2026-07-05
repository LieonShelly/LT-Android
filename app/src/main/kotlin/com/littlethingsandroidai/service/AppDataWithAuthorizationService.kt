package com.littlethingsandroidai.service

import com.littlethingsandroidai.service.auth.repository.AuthRepository
import com.littlethingsandroidai.service.auth.usecase.AuthUseCase
import com.littlethingsandroidai.service.auth.usecase.AuthUseCaseType

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
    val submitAnswerUseCase: PlaceholderUseCase
    val fetchCategoriesUseCase: PlaceholderUseCase
    val fetchTodayQuestionsUseCase: PlaceholderUseCase
}

class AppDataWithAuthorizationService(
    private val authRepository: AuthRepository,
) : AppDataWithAuthorizationServiceful {

    override val authUseCase: AuthUseCaseType by lazy {
        AuthUseCase(repository = authRepository)
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
