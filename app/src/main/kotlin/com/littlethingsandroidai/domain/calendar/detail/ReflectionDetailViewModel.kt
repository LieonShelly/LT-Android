package com.littlethingsandroidai.domain.calendar.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.littlethingsandroidai.domain.calendar.model.Answer

class ReflectionDetailViewModel(
    val answer: Answer,
) : ViewModel()

class ReflectionDetailViewModelFactory(
    private val answer: Answer,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReflectionDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReflectionDetailViewModel(answer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
