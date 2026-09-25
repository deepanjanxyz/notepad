package com.deepanjanxyz.notepad.core.database.mapper

import com.deepanjanxyz.notepad.core.database.entity.NoteEntity
import com.deepanjanxyz.notepad.core.model.Note

object NoteMapper {
    fun toDomain(entity: NoteEntity): Note = entity.toDomain()

    fun toEntity(domain: Note): NoteEntity = NoteEntity.fromDomain(domain)

    fun toDomainList(entities: List<NoteEntity>): List<Note> = entities.map(::toDomain)
}
