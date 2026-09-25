package com.deepanjanxyz.notepad.core.domain.usecase.label

import com.deepanjanxyz.notepad.core.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class GetLabelsUseCase(private val repository: NoteRepository) {
    operator fun invoke(): Flow<List<String>> = repository.getAllLabels()
}
