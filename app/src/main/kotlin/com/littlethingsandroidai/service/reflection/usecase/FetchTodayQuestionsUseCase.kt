package com.littlethingsandroidai.service.reflection.usecase

import com.littlethingsandroidai.domain.calendar.model.Question
import com.littlethingsandroidai.service.reflection.repository.ReflectionRepository

interface FetchTodayQuestionsUseCaseType {
    suspend fun execute(): List<Question>
}

class FetchTodayQuestionsUseCase(
    private val repository: ReflectionRepository,
) : FetchTodayQuestionsUseCaseType {
    override suspend fun execute(): List<Question> = repository.fetchTodayQuestions()
}
