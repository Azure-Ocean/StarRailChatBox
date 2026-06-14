<div align="center">
  <h1>🌠 崩铁ChatBox - 记忆增强版</h1>
  <p>—— 基于原作资料的 AI 角色记忆系统</p>

  <p>
    <img src="https://img.shields.io/badge/Kotlin-Multiplatform-purple?style=flat-square&logo=kotlin" alt="Kotlin Multiplatform" />
    <img src="https://img.shields.io/badge/Compose-Multiplatform-blue?style=flat-square&logo=jetbrains" alt="Compose Multiplatform" />
    <img src="https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20Desktop%20%7C%20Web-brightgreen?style=flat-square" alt="Supported Platforms" />
    <img src="https://img.shields.io/badge/License-GPL%20v3-red?style=flat-square" alt="License" />
  </p>

  <p>
    <b>🔗 原项目地址：<a href="https://github.com/KaiXuan666/StarRailChatBox">KaiXuan666/StarRailChatBox</a></b>
  </p>
</div>

---

## 📖 项目简介

本项目是 [StarRailChatBox](https://github.com/KaiXuan666/StarRailChatBox) 的 **记忆增强版**，在原有功能基础上，新增了 **角色记忆数据库系统**。

### 🎯 解决的问题

**原版问题：**
- 角色卡只有 System Prompt 约束风格
- 模型不知道角色具体经历过什么
- 对话缺乏"真实感"和"一致性"

**本版改进：**
- 为每个角色建立向量化记忆数据库
- 对话时自动检索相关记忆
- 让模型基于"真实记忆"回复，而不是"想象"

---

## 🧠 角色记忆系统

### 核心架构

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

### 记忆类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `fact` | 事实记忆 | 身份背景、能力设定、人际关系 |
| `event` | 经历记忆 | 重要事件、战斗经历、成长历程 |
| `dialogue` | 对话记忆 | 经典台词、说话风格、特定场景 |
| `emotion` | 情感记忆 | 喜好厌恶、情感反应、价值观念 |

### 预置角色记忆

本项目已为以下角色预置了记忆数据：

| 角色 | IP | 记忆数量 | 状态 |
|------|------|------|------|
| 胡桃 | 原神 | 12 条 | ✅ 完成 |
| 琪亚娜 | 崩坏3 | 15 条 | ✅ 完成 |

**胡桃记忆示例：**
```json
{
  "type": "dialogue",
  "content": "哟！这不是旅行者嘛！今天怎么有空来找本堂主玩？",
  "context": "初次见面",
  "importance": 0.8,
  "tags": ["旅行者", "初次见面", "活泼"]
}
```

**琪亚娜记忆示例：**
```json
{
  "type": "emotion",
  "content": "芽衣是我最重要的存在。从小一起长大，一起训练，一起战斗。我对芽衣有着特殊的感情，这种感情超越了友情，超越了亲情。",
  "context": "对芽衣的感情",
  "importance": 0.9,
  "tags": ["芽衣", "重要", "感情"]
}
```

---

## 📦 技术实现

### 新增文件

```
shared/src/
├── commonMain/kotlin/.../data/ai/memory/
│   └── MemoryRetriever.kt          # 记忆检索器
├── commonMain/kotlin/.../data/character/
│   ├── CharacterMemoryRepository.kt # 记忆仓库
│   └── memory/
│       └── PresetCharacterMemories.kt # 预置记忆数据
└── roomMain/kotlin/.../data/database/
    ├── dao/
    │   └── CharacterMemoryDao.kt    # 数据访问对象
    └── entity/
        └── CharacterMemoryEntity.kt # 记忆实体
```

### 核心组件

**1. CharacterMemoryEntity** — 记忆实体
```kotlin
@Entity(tableName = "character_memory")
data class CharacterMemoryEntity(
    val id: Long = 0,
    val characterId: String,
    val memoryType: String,  // "fact", "event", "dialogue", "emotion"
    val content: String,
    val context: String?,
    val tags: String?,
    val importance: Float,
    val embedding: String?,  // 向量嵌入
    // ...
)
```

**2. CharacterMemoryRepository** — 记忆仓库
```kotlin
interface CharacterMemoryRepository {
    suspend fun getMemories(characterId: String): List<CharacterMemoryEntity>
    suspend fun addMemory(memory: CharacterMemoryEntity): Long
    suspend fun retrieveRelevantMemories(
        characterId: String,
        query: String,
        topK: Int = 3,
        threshold: Float = 0.7f,
    ): List<CharacterMemoryEntity>
}
```

**3. MemoryRetriever** — 记忆检索器
```kotlin
class MemoryRetriever(
    private val memoryRepository: CharacterMemoryRepository,
) {
    suspend fun retrieveAndFormat(
        characterId: String,
        userMessage: String,
        topK: Int = 3,
    ): String? {
        val memories = memoryRepository.retrieveRelevantMemories(...)
        return formatMemories(memories)
    }
}
```

---

## 🚀 快速开始

### 1. 克隆仓库

```bash
git clone https://github.com/Azure-Ocean/StarRailChatBox.git
cd StarRailChatBox
git checkout feature/character-memory
```

### 2. 环境准备

- JDK 17
- Android SDK 34
- Android NDK 25
- CMake 3.22.1

详细说明见 [BUILD_GUIDE.md](BUILD_GUIDE.md)

### 3. 集成记忆系统

按照 [MEMORY_INTEGRATION_GUIDE.md](MEMORY_INTEGRATION_GUIDE.md) 进行集成：

1. 更新数据库结构
2. 添加数据库迁移
3. 注册依赖
4. 修改 ChatContextBuilder

### 4. 编译运行

```bash
# Android
./gradlew :androidApp:assembleDebug

# Desktop
./gradlew :desktopApp:run
```

---

## 📚 文档

| 文档 | 说明 |
|------|------|
| [MEMORY_SYSTEM_DESIGN.md](MEMORY_SYSTEM_DESIGN.md) | 记忆系统设计文档 |
| [MEMORY_INTEGRATION_GUIDE.md](MEMORY_INTEGRATION_GUIDE.md) | 集成指南 |
| [BUILD_GUIDE.md](BUILD_GUIDE.md) | 编译指南 |

---

## 🎯 与原版的区别

| 功能 | 原版 | 记忆增强版 |
|------|------|------|
| 角色卡 | ✅ | ✅ |
| System Prompt | ✅ | ✅ |
| 角色记忆数据库 | ❌ | ✅ |
| 向量化检索 | ❌ | ✅ |
| 预置记忆数据 | ❌ | ✅ |
| 动态记忆注入 | ❌ | ✅ |

---

## 📝 添加新角色的记忆

### 方法一：代码方式

```kotlin
val memories = listOf(
    CharacterMemoryEntity(
        characterId = "genshin:zhongli",
        memoryType = MemoryType.FACT.value,
        content = "我是岩王帝君，璃月的创建者。",
        context = "身份介绍",
        importance = 0.9f,
        tags = """["身份","岩王帝君","璃月"]""",
        source = "原神角色设定",
    ),
    // 更多记忆...
)

for (memory in memories) {
    memoryRepository.addMemory(memory)
}
```

### 方法二：JSON 导入

```json
{
  "character_id": "genshin:zhongli",
  "memories": [
    {
      "type": "fact",
      "content": "我是岩王帝君，璃月的创建者。",
      "context": "身份介绍",
      "importance": 0.9,
      "tags": ["身份", "岩王帝君", "璃月"],
      "source": "原神角色设定"
    }
  ]
}
```

---

## 🔮 未来计划

- [ ] 接入更好的 Embedding API（OpenAI、通义千问）
- [ ] 添加记忆管理 UI
- [ ] 实现自动学习功能
- [ ] 支持更多角色的记忆数据
- [ ] 记忆导入/导出功能

---

## 🙏 致谢

- **原项目作者**：[KaiXuan666](https://github.com/KaiXuan666)
- **原项目地址**：[StarRailChatBox](https://github.com/KaiXuan666/StarRailChatBox)

---

## 📄 许可证

本项目基于 [GPL v3](LICENSE) 许可证开源。

---

<div align="center">
  <p><b>🔗 原项目地址：<a href="https://github.com/KaiXuan666/StarRailChatBox">KaiXuan666/StarRailChatBox</a></b></p>
  <p><b>🔗 本项目地址：<a href="https://github.com/Azure-Ocean/StarRailChatBox">Azure-Ocean/StarRailChatBox</a></b></p>
</div>
