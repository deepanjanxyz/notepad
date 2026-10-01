package com.deepanjanxyz.notepad.data.local.mapper

import com.deepanjanxyz.notepad.data.local.entity.LabelEntity
import com.deepanjanxyz.notepad.domain.model.Label

object LabelMapper {
    fun toDomain(entity: LabelEntity): Label = Label(id = entity.id, name = entity.name)

    fun toEntity(domain: Label): LabelEntity = LabelEntity(id = domain.id, name = domain.name)

    fun toDomainList(entities: List<LabelEntity>): List<Label> = entities.map(::toDomain)
}
