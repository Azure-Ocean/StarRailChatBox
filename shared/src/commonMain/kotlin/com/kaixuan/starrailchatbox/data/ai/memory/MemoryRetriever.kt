package com.kaixuan.starrailchatbox.data.ai.memory

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.character.CharacterMemoryRepository
import com.kaixuan.starrailchatbox.data.database.entity.CharacterMemoryEntity
import com.kaixuan.starrailchatbox.data.database.entity.MemoryType
import io.github.aakira.napier.Napier

/**
 * 记忆检索器
 * 
 * 负责从角色记忆库中检索与当前对话相关的记忆
 */
class MemoryRetriever(
    private val memoryRepository: CharacterMemoryRepository,
) {
    /**
     * 检索相关记忆并格式化为上下文
     */
    suspend fun retrieveAndFormat(
        characterId: String,
        userMessage: String,
        topK: Int = 3,
        threshold: Float = 0.6f,
    ): String? {
        return try {
            val memories = memoryRepository.retrieveRelevantMemories(
                characterId = characterId,
                query = userMessage,
                topK = topK,
                threshold = threshold,
            )
            
            if (memories.isEmpty()) {
                Napier.d { "No relevant memories found for query: $userMessage" }
                return null
            }
            
            Napier.d { "Found ${memories.size} relevant memories" }
            formatMemories(memories)
        } catch (e: Exception) {
            Napier.e("Memory retrieval failed", e)
            null
        }
    }
    
    /**
     * 获取角色的核心记忆（始终注入到上下文）
     */
    suspend fun getCoreMemories(
        characterId: String,
        limit: Int = 5,
    ): String? {
        return try {
            val memories = memoryRepository.getTopMemories(characterId, limit)
            
            if (memories.isEmpty()) {
                return null
            }
            
            formatMemories(memories, label = "核心记忆")
        } catch (e: Exception) {
            Napier.e("Failed to get core memories", e)
            null
        }
    }
    
    /**
     * 按类型获取记忆
     */
    suspend fun getMemoriesByType(
        characterId: String,
        type: MemoryType,
        limit: Int = 5,
    ): String? {
        return try {
            val memories = memoryRepository.getMemoriesByType(characterId, type)
                .take(limit)
            
            if (memories.isEmpty()) {
                return null
            }
            
            formatMemories(memories, label = "${type.value}记忆")
        } catch (e: Exception) {
            Napier.e("Failed to get memories by type", e)
            null
        }
    }
    
    private fun formatMemories(
        memories: List<CharacterMemoryEntity>,
        label: String = "相关记忆",
    ): String {
        val memoryStrings = memories.mapIndexed { index, memory ->
            val typeLabel = when (memory.memoryType) {
                MemoryType.FACT.value -> "事实"
                MemoryType.EVENT.value -> "经历"
                MemoryType.DIALOGUE.value -> "对话"
                MemoryType.EMOTION.value -> "情感"
                else -> memory.memoryType
            }
            
            val contextStr = if (!memory.context.isNullOrBlank()) {
                "（${memory.context}）"
            } else {
                ""
            }
            
            "${index + 1}. [$typeLabel] ${memory.content}$contextStr"
        }
        
        return """
            <$label>
            ${memoryStrings.joinToString("\n")}
            </$label>
        """.trimIndent()
    }
}

/**
 * 记忆上下文构建器
 * 
 * 将记忆集成到聊天上下文中
 */
class MemoryContextBuilder(
    private val memoryRetriever: MemoryRetriever,
) {
    /**
     * 构建包含记忆的上下文
     */
    suspend fun buildContextWithMemories(
        characterId: String,
        systemPrompt: String,
        userMessage: String,
        history: List<AiMessage>,
    ): List<AiMessage> {
        val messages = mutableListOf<AiMessage>()
        
        // 1. 系统提示词
        messages.add(AiMessage(role = "system", content = systemPrompt))
        
        // 2. 核心记忆（始终注入）
        val coreMemories = memoryRetriever.getCoreMemories(characterId)
        if (coreMemories != null) {
            messages.add(AiMessage(role = "system", content = coreMemories))
        }
        
        // 3. 相关记忆（根据用户输入检索）
        val relevantMemories = memoryRetriever.retrieveAndFormat(
            characterId = characterId,
            userMessage = userMessage,
        )
        if (relevantMemories != null) {
            messages.add(AiMessage(role = "system", content = relevantMemories))
        }
        
        // 4. 历史消息
        messages.addAll(history)
        
        // 5. 用户当前消息
        messages.add(AiMessage(role = "user", content = userMessage))
        
        return messages
    }
}

/**
 * 记忆注入配置
 */
data class MemoryInjectionConfig(
    val enabled: Boolean = true,
    val coreMemoryLimit: Int = 5,
    val relevantMemoryTopK: Int = 3,
    val relevantMemoryThreshold: Float = 0.6f,
    val injectCoreMemories: Boolean = true,
    val injectRelevantMemories: Boolean = true,
)
