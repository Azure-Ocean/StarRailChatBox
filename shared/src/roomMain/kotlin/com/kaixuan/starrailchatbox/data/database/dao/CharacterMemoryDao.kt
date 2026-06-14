package com.kaixuan.starrailchatbox.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kaixuan.starrailchatbox.data.database.entity.CharacterMemoryEntity
import com.kaixuan.starrailchatbox.data.database.entity.CharacterMemorySummary

@Dao
interface CharacterMemoryDao {
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取角色的所有记忆
     */
    @Query("SELECT * FROM character_memory WHERE character_id = :characterId ORDER BY importance DESC, created_at DESC")
    suspend fun getMemoriesByCharacter(characterId: String): List<CharacterMemoryEntity>
    
    /**
     * 获取角色的记忆摘要（轻量级）
     */
    @Query("""
        SELECT id, character_id, memory_type, 
               substr(content, 1, 100) as content_preview, 
               importance, created_at 
        FROM character_memory 
        WHERE character_id = :characterId 
        ORDER BY importance DESC, created_at DESC
    """)
    suspend fun getMemorySummaries(characterId: String): List<CharacterMemorySummary>
    
    /**
     * 按类型获取记忆
     */
    @Query("SELECT * FROM character_memory WHERE character_id = :characterId AND memory_type = :type ORDER BY importance DESC")
    suspend fun getMemoriesByType(characterId: String, type: String): List<CharacterMemoryEntity>
    
    /**
     * 按标签获取记忆
     */
    @Query("SELECT * FROM character_memory WHERE character_id = :characterId AND tags LIKE '%' || :tag || '%' ORDER BY importance DESC")
    suspend fun getMemoriesByTag(characterId: String, tag: String): List<CharacterMemoryEntity>
    
    /**
     * 获取最重要的记忆
     */
    @Query("SELECT * FROM character_memory WHERE character_id = :characterId ORDER BY importance DESC LIMIT :limit")
    suspend fun getTopMemories(characterId: String, limit: Int = 10): List<CharacterMemoryEntity>
    
    /**
     * 获取最近访问的记忆
     */
    @Query("SELECT * FROM character_memory WHERE character_id = :characterId ORDER BY last_accessed_at DESC LIMIT :limit")
    suspend fun getRecentMemories(characterId: String, limit: Int = 10): List<CharacterMemoryEntity>
    
    /**
     * 根据 ID 获取记忆
     */
    @Query("SELECT * FROM character_memory WHERE id = :id")
    suspend fun getMemoryById(id: Long): CharacterMemoryEntity?
    
    /**
     * 搜索记忆内容
     */
    @Query("SELECT * FROM character_memory WHERE character_id = :characterId AND content LIKE '%' || :query || '%' ORDER BY importance DESC")
    suspend fun searchMemories(characterId: String, query: String): List<CharacterMemoryEntity>
    
    /**
     * 获取记忆总数
     */
    @Query("SELECT COUNT(*) FROM character_memory WHERE character_id = :characterId")
    suspend fun getMemoryCount(characterId: String): Int
    
    /**
     * 获取有嵌入向量的记忆
     */
    @Query("SELECT * FROM character_memory WHERE character_id = :characterId AND embedding IS NOT NULL")
    suspend fun getMemoriesWithEmbedding(characterId: String): List<CharacterMemoryEntity>
    
    // ==================== 写入操作 ====================
    
    /**
     * 插入记忆
     */
    @Insert
    suspend fun insertMemory(memory: CharacterMemoryEntity): Long
    
    /**
     * 批量插入记忆
     */
    @Insert
    suspend fun insertMemories(memories: List<CharacterMemoryEntity>): List<Long>
    
    /**
     * 更新记忆
     */
    @Update
    suspend fun updateMemory(memory: CharacterMemoryEntity)
    
    /**
     * 删除记忆
     */
    @Delete
    suspend fun deleteMemory(memory: CharacterMemoryEntity)
    
    /**
     * 根据 ID 删除记忆
     */
    @Query("DELETE FROM character_memory WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)
    
    /**
     * 删除角色的所有记忆
     */
    @Query("DELETE FROM character_memory WHERE character_id = :characterId")
    suspend fun deleteAllMemories(characterId: String)
    
    /**
     * 记录记忆访问
     */
    @Query("UPDATE character_memory SET last_accessed_at = :timestamp, access_count = access_count + 1 WHERE id = :id")
    suspend fun recordAccess(id: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新记忆嵌入向量
     */
    @Query("UPDATE character_memory SET embedding = :embedding WHERE id = :id")
    suspend fun updateEmbedding(id: Long, embedding: String)
    
    /**
     * 更新记忆重要性
     */
    @Query("UPDATE character_memory SET importance = :importance WHERE id = :id")
    suspend fun updateImportance(id: Long, importance: Float)
    
    // ==================== 统计操作 ====================
    
    /**
     * 获取各类型记忆数量
     */
    @Query("SELECT memory_type, COUNT(*) as count FROM character_memory WHERE character_id = :characterId GROUP BY memory_type")
    suspend fun getMemoryCountByType(characterId: String): List<MemoryTypeCount>
    
    /**
     * 获取平均重要性
     */
    @Query("SELECT AVG(importance) FROM character_memory WHERE character_id = :characterId")
    suspend fun getAverageImportance(characterId: String): Float?
}

/**
 * 记忆类型统计
 */
data class MemoryTypeCount(
    val memoryType: String,
    val count: Int,
)
