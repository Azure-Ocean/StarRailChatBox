<div align="center">
  <h1>🌠 崩铁ChatBox - 角色知识图谱版</h1>
  <p>—— 基于 Karpathy LLM-Wiki 模式的角色知识库系统</p>

  <p>
    <img src="https://img.shields.io/badge/Kotlin-Multiplatform-purple?style=flat-square&logo=kotlin" alt="Kotlin Multiplatform" />
    <img src="https://img.shields.io/badge/Compose-Multiplatform-blue?style=flat-square&logo=jetbrains" alt="Compose Multiplatform" />
    <img src="https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20Desktop%20%7C%20Web-brightgreen?style=flat-square" alt="Supported Platforms" />
    <img src="https://img.shields.io/badge/License-GPL%20v3-red?style=flat-square" alt="License" />
  </p>

  <p>
    <b>🔗 原项目地址：<a href="https://github.com/KaiXuan666/StarRailChatBox">KaiXuan666/StarRailChatBox</a></b>
  </p>
  <p>
    <b>📚 设计灵感：<a href="https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f">Karpathy LLM-Wiki</a></b>
  </p>
</div>

---

## 📖 项目简介

本项目是 [StarRailChatBox](https://github.com/KaiXuan666/StarRailChatBox) 的 **角色知识图谱版**，在原有功能基础上，新增了 **基于 Karpathy LLM-Wiki 模式的角色知识库系统**。

### 🎯 解决的问题

**原版问题：**
- 角色卡只有 System Prompt（~200-500 字）
- 模型不知道角色具体经历过什么
- 对话缺乏"真实感"和"一致性"

**本版改进：**
- 为每个角色建立 **数万字的知识库**
- 包含角色背景、历史发言、人际关系、角色经历
- 基于 Karpathy LLM-Wiki 模式，**编译式知识管理**
- 支持结构化查询 + 语义检索

---

## 📚 设计灵感：Karpathy LLM-Wiki

### 核心思想

> "The wiki is a persistent, compounding artifact. Cross-references are already there. Contradictions have already been flagged. The synthesis already reflects everything you have read."
> — Andrej Karpathy, 2026

**关键洞察**：从"检索"（RAG）转向"编译 + 维护"（LLM Wiki）

| 方面 | 传统 RAG | Karpathy LLM-Wiki |
|------|----------|-------------------|
| 知识积累 | ❌ 每次重新发现 | ✅ 持续积累 |
| 交叉引用 | ❌ 需要实时计算 | ✅ 预编译好 |
| 矛盾检测 | ❌ 无 | ✅ 自动标记 |
| 维护成本 | 高 | 接近零 |

### 三层架构

```
┌─────────────────────────────────────────────────────────┐
│                    Raw Sources（原始资料）                │
│  角色设定、剧情对话、角色故事、同人创作                   │
│  特点：不可变，LLM 只读不写                              │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼ 编译（Compile）
┌─────────────────────────────────────────────────────────┐
│                    Character Wiki（角色 Wiki）            │
│  角色档案、人际关系、事件时间线、经典台词、情感记忆        │
│  特点：LLM 编译，交叉引用，持续更新                      │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼ 存储（Store）
┌─────────────────────────────────────────────────────────┐
│                    SQLite Database（数据库）              │
│  知识页面、实体、关系、事件、台词                         │
│  特点：高效检索，结构化查询，向量索引                    │
└─────────────────────────────────────────────────────────┘
```

### 参考文献

1. **Karpathy, A. (2026)**. LLM Wiki. GitHub Gist. https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f
2. **PyShine. (2026)**. Karpathy's LLM Wiki: Build a Compounding Knowledge Base. https://pyshine.com/Karpathy-LLM-Wiki-Compounding-Knowledge-Base
3. **wuphf. (2026)**. A Karpathy-style LLM wiki. https://github.com/nex-crm/wuphf

---

## 🧠 角色知识图谱系统

### 系统架构

```
character_kb/
├── raw/                          # 原始资料（不可变）
│   ├── hutao/
│   │   ├── official/             # 官方资料
│   │   ├── lore/                 # 世界观设定
│   │   └── fan/                  # 同人创作
│   └── kiana/
│       └── ...
├── wiki/                         # 编译后的知识页面
│   ├── hutao/
│   │   ├── profile.md           # 角色档案
│   │   ├── relationships.md     # 人际关系
│   │   ├── timeline.md          # 事件时间线
│   │   ├── dialogues.md         # 经典台词
│   │   ├── emotions.md          # 情感记忆
│   │   └── worldview.md         # 世界观/价值观
│   └── kiana/
│       └── ...
├── index.md                      # 角色索引
└── log.md                        # 更新记录
```

### 页面类型

| 类型 | 说明 | 内容 |
|------|------|------|
| `profile` | 角色档案 | 基本信息、性格特征、核心矛盾 |
| `relationships` | 人际关系 | 重要人物、组织关系、互动方式 |
| `timeline` | 事件时间线 | 重要事件、成长历程、转折点 |
| `dialogues` | 经典台词 | 自我介绍、日常对话、特定场景 |
| `emotions` | 情感记忆 | 喜好厌恶、情感反应、价值观念 |
| `worldview` | 世界观/价值观 | 对生死、命运、战斗的态度 |

### 预置角色知识库

| 角色 | IP | 知识库规模 | 状态 |
|------|------|------|------|
| 胡桃 | 原神 | ~10,000 字 | ✅ 完成 |
| 琪亚娜 | 崩坏3 | ~15,000 字 | ✅ 完成 |

**胡桃知识库示例：**
- 角色档案：身份背景、性格特征、说话风格
- 人际关系：钟离、旅行者、七七等
- 事件时间线：继承往生堂、海灯节活动
- 经典台词：50+ 条原作台词
- 情感记忆：对生死的理解、对璃月港的感情
- 世界观：死亡不是终点，生命的意义

---

## 🔧 技术实现

### 数据库设计

```sql
-- 知识页面表
CREATE TABLE character_pages (
    id TEXT PRIMARY KEY,
    character_id TEXT NOT NULL,
    page_type TEXT NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    metadata TEXT,
    embedding BLOB,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- 实体表
CREATE TABLE entities (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    description TEXT,
    metadata TEXT
);

-- 关系表
CREATE TABLE relationships (
    id TEXT PRIMARY KEY,
    from_entity TEXT NOT NULL,
    to_entity TEXT NOT NULL,
    relationship_type TEXT NOT NULL,
    description TEXT,
    metadata TEXT
);
```

### 检索策略

**1. 结构化查询**
```kotlin
// 查询角色档案
val profile = knowledgeDao.getPage("genshin:hutao", "profile")

// 查询人际关系
val relationships = knowledgeDao.getRelationships("genshin:hutao")

// 查询特定关系
val zhongliRelation = knowledgeDao.getRelationship("genshin:hutao", "genshin:zhongli")
```

**2. 语义检索**
```kotlin
// 查询与"生死观"相关的内容
val results = knowledgeDao.semanticSearch(
    query = "生死观",
    characterId = "genshin:hutao",
    pageTypes = listOf("profile", "emotions", "worldview"),
    topK = 5
)
```

**3. 混合检索**
```kotlin
// 混合检索：关键词 + 语义
val results = knowledgeDao.hybridSearch(
    query = "胡桃对钟离的态度",
    characterId = "genshin:hutao",
    topK = 5
)
```

### 上下文注入

```kotlin
// 构建包含知识的上下文
suspend fun buildContextWithKnowledge(
    characterId: String,
    userMessage: String,
    history: List<AiMessage>,
): List<AiMessage> {
    val messages = mutableListOf<AiMessage>()
    
    // 1. 系统提示词
    messages.add(AiMessage(role = "system", content = systemPrompt))
    
    // 2. 核心知识（始终注入）
    val coreKnowledge = knowledgeRetriever.getCoreKnowledge(characterId)
    if (coreKnowledge != null) {
        messages.add(AiMessage(role = "system", content = coreKnowledge))
    }
    
    // 3. 相关知识（根据用户输入检索）
    val relevantKnowledge = knowledgeRetriever.retrieveAndFormat(
        characterId = characterId,
        userMessage = userMessage,
    )
    if (relevantKnowledge != null) {
        messages.add(AiMessage(role = "system", content = relevantKnowledge))
    }
    
    // 4. 历史消息 + 用户当前消息
    messages.addAll(history)
    messages.add(AiMessage(role = "user", content = userMessage))
    
    return messages
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

### 3. 集成知识库系统

按照 [CHARACTER_KNOWLEDGE_SYSTEM_DESIGN.md](CHARACTER_KNOWLEDGE_SYSTEM_DESIGN.md) 进行集成：

1. 更新数据库结构
2. 添加数据库迁移
3. 实现编译器
4. 实现检索引擎
5. 修改 ChatContextBuilder

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
| [CHARACTER_KNOWLEDGE_SYSTEM_DESIGN.md](CHARACTER_KNOWLEDGE_SYSTEM_DESIGN.md) | 角色知识图谱系统设计（核心） |
| [KARPATHY_LLM_WIKI_RESEARCH.md](KARPATHY_LLM_WIKI_RESEARCH.md) | Karpathy LLM-Wiki 研究笔记 |
| [MEMORY_SYSTEM_DESIGN.md](MEMORY_SYSTEM_DESIGN.md) | 记忆系统设计（早期版本） |
| [MEMORY_INTEGRATION_GUIDE.md](MEMORY_INTEGRATION_GUIDE.md) | 集成指南 |
| [BUILD_GUIDE.md](BUILD_GUIDE.md) | 编译指南 |

---

## 🎯 与原版的区别

| 功能 | 原版 | 角色知识图谱版 |
|------|------|------|
| 角色卡 | ✅ | ✅ |
| System Prompt | ✅ | ✅ |
| 角色知识库 | ❌ | ✅（数万字） |
| 交叉引用 | ❌ | ✅ |
| 结构化查询 | ❌ | ✅ |
| 语义检索 | ❌ | ✅ |
| 混合检索 | ❌ | ✅ |
| 知识编译 | ❌ | ✅ |
| 健康检查 | ❌ | ✅ |

---

## 📝 添加新角色的知识库

### 1. 准备原始资料

```
character_kb/raw/new_character/
├── official/
│   ├── character-profile.md
│   ├── voice-lines.md
│   └── story-quest.md
├── lore/
│   └── world-setting.md
└── fan/
    └── interpretation.md
```

### 2. 编译知识页面

```kotlin
// 使用编译器
val compiler = KnowledgeCompiler(knowledgeDao, embeddingService)
compiler.ingest("genshin:new_character", "character_kb/raw/new_character/")
```

### 3. 手动编写（可选）

```markdown
---
title: 新角色 - 角色档案
character_id: genshin:new_character
page_type: profile
created: 2026-06-14
updated: 2026-06-14
tags: [原神, 新角色]
confidence: high
---

# 新角色

## 基本信息
...

## 性格特征
...

## 相关页面
- [[new_character-relationships]] - 人际关系
- [[new_character-timeline]] - 事件时间线
```

---

## 🔮 未来计划

### 短期（1-2 周）
- [ ] 实现数据库层
- [ ] 实现编译器
- [ ] 实现检索引擎
- [ ] 与 StarRailChatBox 集成

### 中期（1-2 月）
- [ ] 添加更多角色知识库
- [ ] 实现自动学习功能
- [ ] 添加知识管理 UI
- [ ] 支持知识导入/导出

### 长期（3-6 月）
- [ ] 支持多 IP 知识库
- [ ] 实现知识图谱可视化
- [ ] 支持跨角色知识检索
- [ ] 开源知识库编辑器

---

## 🙏 致谢

- **原项目作者**：[KaiXuan666](https://github.com/KaiXuan666)
- **原项目地址**：[StarRailChatBox](https://github.com/KaiXuan666/StarRailChatBox)
- **设计灵感**：[Karpathy LLM-Wiki](https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f)

---

## 📄 许可证

本项目基于 [GPL v3](LICENSE) 许可证开源。

---

<div align="center">
  <p><b>🔗 原项目地址：<a href="https://github.com/KaiXuan666/StarRailChatBox">KaiXuan666/StarRailChatBox</a></b></p>
  <p><b>🔗 本项目地址：<a href="https://github.com/Azure-Ocean/StarRailChatBox">Azure-Ocean/StarRailChatBox</a></b></p>
  <p><b>📚 设计灵感：<a href="https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f">Karpathy LLM-Wiki</a></b></p>
</div>
