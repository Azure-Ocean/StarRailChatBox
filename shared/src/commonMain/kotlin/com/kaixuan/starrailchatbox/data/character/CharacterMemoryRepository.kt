package com.kaixuan.starrailchatbox.data.character

import com.kaixuan.starrailchatbox.data.database.dao.CharacterMemoryDao
import com.kaixuan.starrailchatbox.data.database.entity.CharacterMemoryEntity
import com.kaixuan.starrailchatbox.data.database.entity.CharacterMemorySummary
import com.kaixuan.starrailchatbox.data.database.entity.MemoryType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 角色记忆仓库
 * 
 * 提供角色记忆的 CRUD 操作和向量检索功能
 */
interface CharacterMemoryRepository {
    // 查询
    suspend fun getMemories(characterId: String): List<CharacterMemoryEntity>
    suspend fun getMemorySummaries(characterId: String): List<CharacterMemorySummary>
    suspend fun getMemoriesByType(characterId: String, type: MemoryType): List<CharacterMemoryEntity>
    suspend fun getMemoriesByTag(characterId: String, tag: String): List<CharacterMemoryEntity>
    suspend fun getTopMemories(characterId: String, limit: Int = 10): List<CharacterMemoryEntity>
    suspend fun getMemoryById(id: Long): CharacterMemoryEntity?
    suspend fun searchMemories(characterId: String, query: String): List<CharacterMemoryEntity>
    
    // 写入
    suspend fun addMemory(memory: CharacterMemoryEntity): Long
    suspend fun updateMemory(memory: CharacterMemoryEntity)
    suspend fun deleteMemory(id: Long)
    suspend fun deleteAllMemories(characterId: String)
    
    // 向量检索
    suspend fun retrieveRelevantMemories(
        characterId: String,
        query: String,
        topK: Int = 3,
        threshold: Float = 0.7f,
    ): List<CharacterMemoryEntity>
    
    // 统计
    suspend fun getMemoryCount(characterId: String): Int
}

/**
 * 默认角色记忆仓库实现
 */
class DefaultCharacterMemoryRepository(
    private val memoryDao: CharacterMemoryDao,
    private val embeddingService: EmbeddingService? = null,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : CharacterMemoryRepository {
    
    override suspend fun getMemories(characterId: String): List<CharacterMemoryEntity> {
        return withContext(Dispatchers.IO) {
            memoryDao.getMemoriesByCharacter(characterId)
        }
    }
    
    override suspend fun getMemorySummaries(characterId: String): List<CharacterMemorySummary> {
        return withContext(Dispatchers.IO) {
            memoryDao.getMemorySummaries(characterId)
        }
    }
    
    override suspend fun getMemoriesByType(
        characterId: String,
        type: MemoryType
    ): List<CharacterMemoryEntity> {
        return withContext(Dispatchers.IO) {
            memoryDao.getMemoriesByType(characterId, type.value)
        }
    }
    
    override suspend fun getMemoriesByTag(
        characterId: String,
        tag: String
    ): List<CharacterMemoryEntity> {
        return withContext(Dispatchers.IO) {
            memoryDao.getMemoriesByTag(characterId, tag)
        }
    }
    
    override suspend fun getTopMemories(
        characterId: String,
        limit: Int
    ): List<CharacterMemoryEntity> {
        return withContext(Dispatchers.IO) {
            memoryDao.getTopMemories(characterId, limit)
        }
    }
    
    override suspend fun getMemoryById(id: Long): CharacterMemoryEntity? {
        return withContext(Dispatchers.IO) {
            memoryDao.getMemoryById(id)
        }
    }
    
    override suspend fun searchMemories(
        characterId: String,
        query: String
    ): List<CharacterMemoryEntity> {
        return withContext(Dispatchers.IO) {
            memoryDao.searchMemories(characterId, query)
        }
    }
    
    override suspend fun addMemory(memory: CharacterMemoryEntity): Long {
        return withContext(Dispatchers.IO) {
            val memoryWithEmbedding = if (embeddingService != null && memory.embedding == null) {
                try {
                    val embedding = embeddingService.embed(memory.content)
                    memory.copy(embedding = json.encodeToString(embedding))
                } catch (e: Exception) {
                    Napier.e("Failed to generate embedding", e)
                    memory
                }
            } else {
                memory
            }
            memoryDao.insertMemory(memoryWithEmbedding)
        }
    }
    
    override suspend fun updateMemory(memory: CharacterMemoryEntity) {
        withContext(Dispatchers.IO) {
            memoryDao.updateMemory(memory)
        }
    }
    
    override suspend fun deleteMemory(id: Long) {
        withContext(Dispatchers.IO) {
            memoryDao.deleteMemoryById(id)
        }
    }
    
    override suspend fun deleteAllMemories(characterId: String) {
        withContext(Dispatchers.IO) {
            memoryDao.deleteAllMemories(characterId)
        }
    }
    
    override suspend fun retrieveRelevantMemories(
        characterId: String,
        query: String,
        topK: Int,
        threshold: Float,
    ): List<CharacterMemoryEntity> {
        if (embeddingService == null) {
            Napier.w("Embedding service not available, falling back to keyword search")
            return searchMemories(characterId, query).take(topK)
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val queryEmbedding = embeddingService.embed(query)
                val allMemories = memoryDao.getMemoriesWithEmbedding(characterId)
                
                val memoriesWithScore = allMemories.mapNotNull { memory ->
                    val memoryEmbedding = try {
                        json.decodeFromString<FloatArray>(memory.embedding!!)
                    } catch (e: Exception) {
                        return@mapNotNull null
                    }
                    
                    val score = cosineSimilarity(queryEmbedding, memoryEmbedding)
                    memory to score
                }
                
                memoriesWithScore
                    .filter { it.second >= threshold }
                    .sortedByDescending { it.second }
                    .take(topK)
                    .map { (memory, score) ->
                        // 记录访问
                        memoryDao.recordAccess(memory.id)
                        memory
                    }
            } catch (e: Exception) {
                Napier.e("Vector retrieval failed, falling back to keyword search", e)
                searchMemories(characterId, query).take(topK)
            }
        }
    }
    
    override suspend fun getMemoryCount(characterId: String): Int {
        return withContext(Dispatchers.IO) {
            memoryDao.getMemoryCount(characterId)
        }
    }
    
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vectors must have the same size" }
        
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        return if (normA == 0.0f || normB == 0.0f) {
            0.0f
        } else {
            dotProduct / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
        }
    }
}

/**
 * 嵌入向量服务接口
 */
interface EmbeddingService {
    /**
     * 将文本转换为嵌入向量
     */
    suspend fun embed(text: String): FloatArray
    
    /**
     * 获取向量维度
     */
    fun dimension(): Int
}

/**
 * 内存嵌入服务（用于测试或小规模场景）
 * 
 * 使用简单的 TF-IDF 或词袋模型生成向量
 * 不适合生产环境，仅用于开发和测试
 */
class SimpleEmbeddingService : EmbeddingService {
    private val vocabulary = mutableMapOf<String, Int>()
    private var nextIndex = 0
    
    override suspend fun embed(text: String): FloatArray {
        val words = tokenize(text)
        val vector = FloatArray(DIMENSION)
        
        for (word in words) {
            val index = vocabulary.getOrPut(word) { nextIndex++ % DIMENSION }
            vector[index] += 1.0f
        }
        
        // 归一化
        val norm = kotlin.math.sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        if (norm > 0) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        
        return vector
    }
    
    override fun dimension(): Int = DIMENSION
    
    private fun tokenize(text: String): List<String> {
        // 简单的中英文分词
        return text
            .replace(Regex("[^\\w\\u4e00-\\u9fff]"), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
    }
    
    companion object {
        private const val DIMENSION = 768
    }
}

/**
 * 嵌入向量序列化器
 */
@Serializable
data class EmbeddingData(
    val vector: FloatArray,
    val model: String,
    val dimension: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingData) return false
        return vector.contentEquals(other.vector) && model == other.model && dimension == other.dimension
    }
    
    override fun hashCode(): Int {
        var result = vector.contentHashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + dimension
        return result
    }
}
