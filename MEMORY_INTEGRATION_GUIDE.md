# 角色记忆系统实现指南

## 已完成的代码

### 1. 数据库层
- `CharacterMemoryEntity.kt` — 记忆实体定义
- `CharacterMemoryDao.kt` — 数据访问对象

### 2. 业务层
- `CharacterMemoryRepository.kt` — 记忆仓库接口和实现
- `MemoryRetriever.kt` — 记忆检索器
- `PresetCharacterMemories.kt` — 预置记忆数据

## 集成步骤

### 步骤 1: 更新数据库

修改 `StarRailDatabase.kt`，添加新的 Entity：

```kotlin
@Database(
    entities = [
        AgentRoleEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        ChatSummaryEntity::class,
        ModelConfigEntity::class,
        MessageAttachmentEntity::class,
        CharacterMemoryEntity::class,  // 新增
    ],
    version = 6,  // 版本号 +1
    exportSchema = false,
)
@ConstructedBy(StarRailDatabaseConstructor::class)
abstract class StarRailDatabase : RoomDatabase() {
    // ... 现有 DAO ...
    abstract fun characterMemoryDao(): CharacterMemoryDao  // 新增
}
```

### 步骤 2: 添加数据库迁移

```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS character_memory (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                character_id TEXT NOT NULL,
                memory_type TEXT NOT NULL,
                content TEXT NOT NULL,
                context TEXT,
                tags TEXT,
                importance REAL NOT NULL DEFAULT 0.5,
                embedding TEXT,
                created_at INTEGER NOT NULL,
                last_accessed_at INTEGER NOT NULL,
                access_count INTEGER NOT NULL DEFAULT 0,
                source TEXT
            )
        """)
        
        db.execSQL("CREATE INDEX IF NOT EXISTS index_character_memory_character_id ON character_memory(character_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_character_memory_character_id_memory_type ON character_memory(character_id, memory_type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_character_memory_character_id_importance ON character_memory(character_id, importance)")
    }
}
```

### 步骤 3: 注册依赖

修改 `AppModule.kt`：

```kotlin
// 添加记忆仓库
single<CharacterMemoryRepository> {
    DefaultCharacterMemoryRepository(
        memoryDao = get<StarRailDatabase>().characterMemoryDao(),
        embeddingService = SimpleEmbeddingService(),  // 或接入外部 Embedding API
    )
}

// 添加记忆检索器
single { MemoryRetriever(get()) }

// 添加记忆上下文构建器
single { MemoryContextBuilder(get()) }
```

### 步骤 4: 修改 ChatContextBuilder

在 `buildChatContext` 函数中集成记忆检索：

```kotlin
suspend fun buildChatContext(
    characterId: String,
    systemPrompt: String,
    summary: ChatSummary?,
    history: List<StoredChatMessage>,
    currentUserMessage: String,
    maxHistoryMessageCount: Int?,
    memoryContextBuilder: MemoryContextBuilder? = null,  // 新增参数
): List<AiMessage> {
    val messages = mutableListOf<AiMessage>()
    
    // 1. 系统提示词
    messages.add(AiMessage(role = "system", content = systemPrompt))
    
    // 2. 核心记忆（新增）
    if (memoryContextBuilder != null) {
        val coreMemories = memoryContextBuilder.memoryRetriever.getCoreMemories(characterId)
        if (coreMemories != null) {
            messages.add(AiMessage(role = "system", content = coreMemories))
        }
        
        val relevantMemories = memoryContextBuilder.memoryRetriever.retrieveAndFormat(
            characterId = characterId,
            userMessage = currentUserMessage,
        )
        if (relevantMemories != null) {
            messages.add(AiMessage(role = "system", content = relevantMemories))
        }
    }
    
    // 3. 摘要
    summary?.content?.trim()?.takeIf(String::isNotEmpty)?.let {
        messages.add(AiMessage(role = "system", content = "<chat_history_summary>\n$it\n</chat_history_summary>"))
    }
    
    // 4. 历史消息
    // ... 现有代码 ...
    
    // 5. 用户当前消息
    messages.add(AiMessage(role = "user", content = currentUserMessage))
    
    return messages
}
```

### 步骤 5: 初始化预置记忆

在 `DefaultCharacterRepository` 中添加初始化逻辑：

```kotlin
class DefaultCharacterRepository(
    private val storage: CharacterStorage,
    private val memoryRepository: CharacterMemoryRepository,  // 新增
    // ...
) : CharacterRepository {
    
    override suspend fun loadCharacters(): List<Character> {
        storage.initializeDefaults(defaultAssets())
        initializePresetMemories()  // 新增
        return storage.loadCharacters().map(CharacterFiles::toCharacter)
    }
    
    private suspend fun initializePresetMemories() {
        val presetMemories = PresetCharacterMemories.getAllPresetMemories()
        
        for ((characterId, memories) in presetMemories) {
            val existingCount = memoryRepository.getMemoryCount(characterId)
            if (existingCount == 0) {
                // 只在角色没有记忆时初始化
                for (memory in memories) {
                    memoryRepository.addMemory(memory)
                }
                Napier.d { "Initialized ${memories.size} preset memories for $characterId" }
            }
        }
    }
}
```

## 记忆数据格式

### 标签格式

标签使用 JSON 数组格式：

```json
["标签1", "标签2", "标签3"]
```

### 嵌入向量格式

嵌入向量使用 JSON 数组格式：

```json
[0.123, 0.456, 0.789, ...]
```

## 记忆类型说明

| 类型 | 说明 | 示例 |
|------|------|------|
| `fact` | 事实记忆 | 身份背景、能力设定、人际关系 |
| `event` | 经历记忆 | 重要事件、战斗经历、成长历程 |
| `dialogue` | 对话记忆 | 经典台词、说话风格、特定场景 |
| `emotion` | 情感记忆 | 喜好厌恶、情感反应、价值观念 |

## 添加新角色的记忆

### 方法一：代码方式

```kotlin
val memories = listOf(
    CharacterMemoryEntity(
        characterId = "角色ID",
        memoryType = MemoryType.FACT.value,
        content = "记忆内容",
        context = "记忆上下文",
        importance = 0.8f,
        tags = """["标签1","标签2"]""",
        source = "来源",
    ),
    // 更多记忆...
)

for (memory in memories) {
    memoryRepository.addMemory(memory)
}
```

### 方法二：JSON 导入

创建 JSON 文件：

```json
{
  "character_id": "角色ID",
  "memories": [
    {
      "type": "fact",
      "content": "记忆内容",
      "context": "记忆上下文",
      "importance": 0.8,
      "tags": ["标签1", "标签2"],
      "source": "来源"
    }
  ]
}
```

然后导入：

```kotlin
val jsonString = File("memories.json").readText()
val data = Json.decodeFromString<MemoryImportData>(jsonString)

for (memoryData in data.memories) {
    val memory = CharacterMemoryEntity(
        characterId = data.characterId,
        memoryType = memoryData.type,
        content = memoryData.content,
        context = memoryData.context,
        importance = memoryData.importance,
        tags = Json.encodeToString(memoryData.tags),
        source = memoryData.source,
    )
    memoryRepository.addMemory(memory)
}
```

## Embedding 服务集成

### 方案一：本地简单向量（当前实现）

使用 `SimpleEmbeddingService`，基于词袋模型生成向量。

**优点：** 无需外部依赖，离线可用
**缺点：** 语义理解能力有限

### 方案二：接入外部 Embedding API

```kotlin
class RemoteEmbeddingService(
    private val httpClient: HttpClient,
    private val apiUrl: String,
    private val apiKey: String,
) : EmbeddingService {
    override suspend fun embed(text: String): FloatArray {
        val response = httpClient.post("$apiUrl/embeddings") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(EmbeddingRequest(input = text))
        }
        
        val result = response.body<EmbeddingResponse>()
        return result.data.first().embedding.toFloatArray()
    }
    
    override fun dimension(): Int = 1536  // OpenAI 默认维度
}
```

### 方案三：本地 ONNX 模型

```kotlin
class OnnxEmbeddingService(
    private val modelPath: String,
) : EmbeddingService {
    private val env = OrtEnvironment.getEnvironment()
    private val session = env.createSession(modelPath)
    
    override suspend fun embed(text: String): FloatArray {
        val tokens = tokenize(text)
        val inputTensor = OnnxTensor.createTensor(env, arrayOf(tokens))
        
        val results = session.run(mapOf("input_ids" to inputTensor))
        val embeddings = results[0].value as Array<FloatArray>
        
        return embeddings.first()
    }
    
    override fun dimension(): Int = 768  // BERT 默认维度
}
```

## 测试

### 单元测试

```kotlin
class CharacterMemoryRepositoryTest {
    @Test
    fun testAddAndRetrieveMemory() = runTest {
        val repository = DefaultCharacterMemoryRepository(
            memoryDao = fakeMemoryDao,
            embeddingService = SimpleEmbeddingService(),
        )
        
        val memory = CharacterMemoryEntity(
            characterId = "test:character",
            memoryType = MemoryType.FACT.value,
            content = "测试记忆",
            importance = 0.8f,
        )
        
        val id = repository.addMemory(memory)
        val retrieved = repository.getMemoryById(id)
        
        assertNotNull(retrieved)
        assertEquals("测试记忆", retrieved.content)
    }
    
    @Test
    fun testVectorRetrieval() = runTest {
        val repository = DefaultCharacterMemoryRepository(
            memoryDao = fakeMemoryDao,
            embeddingService = SimpleEmbeddingService(),
        )
        
        // 添加多个记忆
        repository.addMemory(CharacterMemoryEntity(
            characterId = "test:character",
            memoryType = MemoryType.FACT.value,
            content = "我喜欢苹果",
        ))
        repository.addMemory(CharacterMemoryEntity(
            characterId = "test:character",
            memoryType = MemoryType.FACT.value,
            content = "今天天气很好",
        ))
        
        // 检索相关记忆
        val results = repository.retrieveRelevantMemories(
            characterId = "test:character",
            query = "苹果好吃吗",
            topK = 1,
        )
        
        assertEquals(1, results.size)
        assertEquals("我喜欢苹果", results.first().content)
    }
}
```

## 下一步

1. **集成到 StarRailChatBox**
   - 按照上述步骤修改代码
   - 测试记忆检索效果

2. **完善 Embedding 服务**
   - 接入更好的 Embedding API
   - 或集成本地 ONNX 模型

3. **添加记忆管理 UI**
   - 查看角色记忆
   - 手动添加/编辑记忆
   - 导入/导出记忆

4. **实现自动学习**
   - 从对话中自动提取记忆
   - 情感分析
   - 动态更新记忆

---

最后更新：2026-06-14
