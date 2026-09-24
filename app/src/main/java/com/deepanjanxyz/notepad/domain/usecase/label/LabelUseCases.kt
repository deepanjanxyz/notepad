package com.deepanjanxyz.notepad.domain.usecase.label

data class LabelUseCases(
    val getLabels: GetLabelsUseCase,
    val addLabel: AddLabelUseCase,
    val renameLabel: RenameLabelUseCase,
    val deleteLabel: DeleteLabelUseCase
)
