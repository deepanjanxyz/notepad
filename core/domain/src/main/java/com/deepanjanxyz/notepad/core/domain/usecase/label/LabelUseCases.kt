package com.deepanjanxyz.notepad.core.domain.usecase.label

data class LabelUseCases(
    val getLabels: GetLabelsUseCase,
    val addLabel: AddLabelUseCase,
    val renameLabel: RenameLabelUseCase,
    val deleteLabel: DeleteLabelUseCase
)
