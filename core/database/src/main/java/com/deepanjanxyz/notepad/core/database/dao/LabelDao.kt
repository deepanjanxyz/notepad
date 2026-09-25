package com.deepanjanxyz.notepad.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.deepanjanxyz.notepad.core.database.entity.LabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels_table ORDER BY name ASC")
    fun getAllLabels(): Flow<List<LabelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: LabelEntity): Long

    @Update
    suspend fun updateLabel(label: LabelEntity): Int

    @Delete
    suspend fun deleteLabel(label: LabelEntity): Int

    @Query("DELETE FROM labels_table WHERE name = :name")
    suspend fun deleteByName(name: String): Int

    @Query("SELECT * FROM labels_table WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getLabelByName(name: String): LabelEntity?

    @Query("UPDATE labels_table SET name = :newName WHERE name = :oldName")
    suspend fun renameLabel(oldName: String, newName: String): Int
}
