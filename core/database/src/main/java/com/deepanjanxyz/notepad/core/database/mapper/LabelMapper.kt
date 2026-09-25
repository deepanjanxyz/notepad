package com.deepanjanxyz.notepad.core.database.mapper

import com.deepanjanxyz.notepad.core.database.entity.LabelEntity
import com.deepanjanxyz.notepad.core.model.Label

object LabelMapper {
    fun toDomain(entity: LabelEntity): Label = Label(id = entity.id, name = entity.name)

    fun toEntity(domain: Label): LabelEntity = LabelEntity(id = domain.id, name = domain.name)

    fun toDomainList(entities: List<LabelEntity>): List<Label> = entities.map(::toDomain)
}
