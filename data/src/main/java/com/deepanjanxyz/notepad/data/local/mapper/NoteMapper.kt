package com.deepanjanxyz.notepad.data.local.mapper

import com.deepanjanxyz.notepad.data.local.entity.NoteEntity
import com.deepanjanxyz.notepad.domain.model.Note

object NoteMapper {
    fun toDomain(entity: NoteEntity): Note = entity.toDomain()

    fun toEntity(domain: Note): NoteEntity = NoteEntity.fromDomain(domain)

    fun toDomainList(entities: List<NoteEntity>): List<Note> = entities.map(::toDomain)
}
