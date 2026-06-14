# StarRailChatBox 角色记忆系统改造

## 项目概述

为 StarRailChatBox 添加角色记忆数据库，让每个角色拥有"真正的记忆"，而不仅仅是 System Prompt 的表面约束。

## 问题分析

**现状：**
- 角色卡只有 System Prompt 约束风格
- 模型不知道角色具体经历过什么
- 对话缺乏"真实感"和"一致性"

**目标：**
- 为每个角色建立向量化记忆数据库
- 对话时自动检索相关记忆
- 让模型基于"真实记忆"回复

## 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                    用户输入                               │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│              记忆检索模块                                 │
│  1. 将用户输入向量化                                      │
│  2. 在角色记忆库中检索相似记忆                            │
│  3. 返回 Top-K 相关记忆                                  │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│              上下文构建                                   │
│  System Prompt + 检索到的记忆 + 历史消息 + 用户输入       │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│              AI 模型                                     │
│  基于"角色记忆"生成回复                                   │
└─────────────────────────────────────────────────────────┘
```

## 实现方案

### 方案一：本地向量数据库（推荐）

使用 SQLite + 向量扩展（sqlite-vss）在本地存储和检索向量。

**优点：**
- 无需外部依赖
- 离线可用
- 隐私安全

**缺点：**
- 需要集成 sqlite-vss
- 向量检索性能可能不如专用数据库

### 方案二：外部向量数据库

使用 ChromaDB、Pinecone、Weaviate 等外部向量数据库。

**优点：**
- 性能好
- 功能丰富

**缺点：**
- 需要外部服务
- 离线不可用
- 隐私风险

### 方案三：内存向量检索（轻量级）

对于小规模记忆（<10000 条），直接在内存中计算余弦相似度。

**优点：**
- 实现简单
- 无需额外依赖

**缺点：**
- 性能随记忆数量下降
- 不适合大规模记忆

**推荐：** 先用方案三（内存向量检索），后续可升级到方案一。

## 数据结构设计

### CharacterMemoryEntity

```kotlin
@Entity(tableName = "character_memory")
data class CharacterMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "character_id")
    val characterId: String,  // 关联角色 ID
    
    @ColumnInfo(name = "memory_type")
    val memoryType: String,  // "fact", "event", "dialogue", "emotion"
    
    @ColumnInfo(name = "content")
    val content: String,  // 记忆内容
    
    @ColumnInfo(name = "context")
    val context: String?,  // 记忆上下文（场景、时间等）
    
    @ColumnInfo(name = "tags")
    val tags: String?,  // 标签（JSON 数组）
    
    @ColumnInfo(name = "importance")
    val importance: Float = 0.5f,  // 重要性评分 0-1
    
    @ColumnInfo(name = "embedding")
    val embedding: String?,  // 向量（JSON 数组）
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "last_accessed_at")
    val lastAccessedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "access_count")
    val accessCount: Int = 0,
)
```

### CharacterMemoryDao

```kotlin
@Dao
interface CharacterMemoryDao {
    @Query("SELECT * FROM character_memory WHERE character_id = :characterId")
    suspend fun getMemoriesByCharacter(characterId: String): List<CharacterMemoryEntity>
    
    @Query("SELECT * FROM character_memory WHERE character_id = :characterId AND memory_type = :type")
    suspend fun getMemoriesByType(characterId: String, type: String): List<CharacterMemoryEntity>
    
    @Query("SELECT * FROM character_memory WHERE character_id = :characterId AND tags LIKE '%' || :tag || '%'")
    suspend fun getMemoriesByTag(characterId: String, tag: String): List<CharacterMemoryEntity>
    
    @Insert
    suspend fun insertMemory(memory: CharacterMemoryEntity): Long
    
    @Update
    suspend fun updateMemory(memory: CharacterMemoryEntity)
    
    @Delete
    suspend fun deleteMemory(memory: CharacterMemoryEntity)
    
    @Query("UPDATE character_memory SET last_accessed_at = :timestamp, access_count = access_count + 1 WHERE id = :id")
    suspend fun recordAccess(id: Long, timestamp: Long = System.currentTimeMillis())
}
```

## 向量化方案

### 使用 MiMo Embedding API

如果 MiMo 提供 Embedding API，可以直接使用。否则需要：

1. **使用开源 Embedding 模型：**
   - text2vec-base-chinese
   - bge-small-zh
   - m3e-base

2. **本地推理：**
   - ONNX Runtime
   - TensorFlow Lite
   - PyTorch Mobile

3. **远程 API：**
   - OpenAI Embedding API
   - 通义千问 Embedding API

### 向量格式

```json
{
  "embedding": [0.123, 0.456, 0.789, ...],  // 768 维或 1536 维
  "model": "text2vec-base-chinese",
  "version": "1.0"
}
```

## 检索算法

### 余弦相似度

```kotlin
fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    var dotProduct = 0.0f
    var normA = 0.0f
    var normB = 0.0f
    for (i in a.indices) {
        dotProduct += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }
    return dotProduct / (sqrt(normA) * sqrt(normB))
}
```

### 检索流程

```kotlin
suspend fun retrieveRelevantMemories(
    characterId: String,
    query: String,
    topK: Int = 3,
    threshold: Float = 0.7f
): List<CharacterMemoryEntity> {
    // 1. 将查询向量化
    val queryEmbedding = embedText(query)
    
    // 2. 获取该角色的所有记忆
    val allMemories = memoryDao.getMemoriesByCharacter(characterId)
    
    // 3. 计算相似度并排序
    val memoriesWithScore = allMemories.mapNotNull { memory ->
        val memoryEmbedding = parseEmbedding(memory.embedding) ?: return@mapNotNull null
        val score = cosineSimilarity(queryEmbedding, memoryEmbedding)
        memory to score
    }
    
    // 4. 返回 Top-K 且超过阈值的记忆
    return memoriesWithScore
        .filter { it.second >= threshold }
        .sortedByDescending { it.second }
        .take(topK)
        .map { it.first }
}
```

## 需要修改的文件

### 新增文件

1. **Entity:**
   - `shared/src/roomMain/kotlin/.../entity/CharacterMemoryEntity.kt`

2. **DAO:**
   - `shared/src/roomMain/kotlin/.../dao/CharacterMemoryDao.kt`

3. **Repository:**
   - `shared/src/commonMain/kotlin/.../character/CharacterMemoryRepository.kt`

4. **Embedding:**
   - `shared/src/commonMain/kotlin/.../ai/embedding/EmbeddingService.kt`

5. **Retrieval:**
   - `shared/src/commonMain/kotlin/.../ai/memory/MemoryRetrieval.kt`

### 修改文件

1. **Database:**
   - `shared/src/roomMain/kotlin/.../StarRailDatabase.kt`
     - 添加 CharacterMemoryEntity
     - 添加 CharacterMemoryDao

2. **Context Builder:**
   - `shared/src/commonMain/kotlin/.../chat/ChatContextBuilder.kt`
     - 集成记忆检索逻辑

3. **DI:**
   - `shared/src/commonMain/kotlin/.../di/AppModule.kt`
     - 注册新的依赖

4. **Character Repository:**
   - `shared/src/commonMain/kotlin/.../character/CharacterRepository.kt`
     - 添加记忆管理接口

## 记忆数据来源

### 1. 从原作提取

**对话记忆：**
- 游戏中的语音台词
- 剧情对话
- 角色故事

**事件记忆：**
- 主线剧情
- 支线任务
- 活动剧情

**情感记忆：**
- 角色对其他角色的态度
- 角色的喜好厌恶
- 角色的价值观

### 2. 从对话中学习

**用户交互：**
- 记录用户与角色的对话
- 提取重要信息
- 动态更新记忆

**情感分析：**
- 分析对话中的情感
- 记录角色的情感反应
- 建立情感记忆

### 3. 人工补充

**角色设定：**
- 官方角色介绍
- 角色故事
- 角色语音

**同人创作：**
- 经典同人场景
- 角色互动
- 情感表达

## 实施步骤

### Phase 1: 基础架构（1-2 天）

1. 创建 Entity 和 DAO
2. 修改数据库结构
3. 实现基础 Repository

### Phase 2: 向量化（2-3 天）

1. 集成 Embedding 服务
2. 实现向量存储
3. 实现向量检索

### Phase 3: 上下文集成（1-2 天）

1. 修改 ChatContextBuilder
2. 集成记忆检索
3. 测试效果

### Phase 4: 记忆管理 UI（2-3 天）

1. 添加记忆管理界面
2. 支持手动添加/编辑记忆
3. 支持导入/导出记忆

### Phase 5: 自动学习（可选，3-5 天）

1. 从对话中自动提取记忆
2. 情感分析
3. 动态更新记忆

## 编译指南

### 环境要求

- JDK 17
- Android SDK 34
- Android NDK 25
- CMake 3.22.1
- Kotlin 1.9.22

### 编译命令

```bash
# Android
./gradlew :androidApp:assembleDebug

# Desktop
./gradlew :desktopApp:run

# 热重载（开发推荐）
./gradlew :desktopApp:hotRun --auto
```

### 常见问题

1. **Gradle 同步失败**
   - 检查网络连接
   - 使用代理或镜像

2. **NDK 版本不匹配**
   - 安装 NDK 25
   - 配置 local.properties

3. **内存不足**
   - 增加 Gradle 内存：`org.gradle.jvmargs=-Xmx4096m`

## 下一步

1. 先实现 Phase 1-3（基础功能）
2. 测试效果
3. 根据反馈决定是否继续 Phase 4-5

---

最后更新：2026-06-14
