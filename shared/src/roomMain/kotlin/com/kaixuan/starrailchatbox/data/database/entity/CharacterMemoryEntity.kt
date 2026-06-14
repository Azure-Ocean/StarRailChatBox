package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 角色记忆实体
 * 
 * 存储角色的各种类型记忆，包括：
 * - fact: 事实记忆（身份、背景、能力等）
 * - event: 经历记忆（重要事件、战斗经历等）
 * - dialogue: 对话记忆（经典台词、说话风格等）
 * - emotion: 情感记忆（喜好厌恶、情感反应等）
 */
@Entity(
    tableName = "character_memory",
    indices = [
        Index(value = ["character_id"]),
        Index(value = ["character_id", "memory_type"]),
        Index(value = ["character_id", "importance"]),
    ]
)
data class CharacterMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "character_id")
    val characterId: String,
    
    @ColumnInfo(name = "memory_type")
    val memoryType: String,
    
    @ColumnInfo(name = "content")
    val content: String,
    
    @ColumnInfo(name = "context")
    val context: String? = null,
    
    @ColumnInfo(name = "tags")
    val tags: String? = null,
    
    @ColumnInfo(name = "importance")
    val importance: Float = 0.5f,
    
    @ColumnInfo(name = "embedding")
    val embedding: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "last_accessed_at")
    val lastAccessedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "access_count")
    val accessCount: Int = 0,
    
    @ColumnInfo(name = "source")
    val source: String? = null,
)

/**
 * 记忆类型枚举
 */
enum class MemoryType(val value: String) {
    FACT("fact"),
    EVENT("event"),
    DIALOGUE("dialogue"),
    EMOTION("emotion"),
}

/**
 * 记忆摘要（用于列表展示）
 */
data class CharacterMemorySummary(
    val id: Long,
    val characterId: String,
    val memoryType: String,
    val contentPreview: String,
    val importance: Float,
    val createdAt: Long,
)
