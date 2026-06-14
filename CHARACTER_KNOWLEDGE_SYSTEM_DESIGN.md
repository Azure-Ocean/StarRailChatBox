# 角色知识图谱系统设计

> 基于 Karpathy LLM-Wiki 模式，针对 AI 角色扮演场景优化

## 设计灵感

本设计参考了 Andrej Karpathy 的 LLM-Wiki 模式（2026），该模式提出了一种"编译式"知识管理方法：

> "The wiki is a persistent, compounding artifact. Cross-references are already there. Contradictions have already been flagged. The synthesis already reflects everything you have read."
> — Andrej Karpathy

**参考文献**：
1. Karpathy, A. (2026). LLM Wiki. https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f
2. wuphf. (2026). A Karpathy-style LLM wiki. https://github.com/nex-crm/wuphf

---

## 一、问题分析

### 1.1 原版 StarRailChatBox 的局限

**现状**：
- 角色卡只有 System Prompt（~200-500 字）
- 模型不知道角色具体经历过什么
- 对话缺乏"真实感"和"一致性"

**根本原因**：
- System Prompt 是"表面约束"，不是"真正记忆"
- 模型需要"编译过的知识"，而不是"原始资料"

### 1.2 传统 RAG 的局限

**传统 RAG**：
- 每次查询都从原始文档中检索
- 知识不积累，每次重新发现
- 检索效率低，质量不稳定

**Karpathy 的洞察**：
> "Unlike traditional RAG (which rediscovers knowledge from scratch per query), the wiki compiles knowledge once and keeps it current."

### 1.3 我们的需求

- 支持数万字的角色知识库
- 包含角色背景、历史发言、人际关系、角色经历
- 高效检索（结构化查询 + 语义检索）
- 易于维护和扩展

---

## 二、系统架构

### 2.1 三层架构（参考 Karpathy）

```
┌─────────────────────────────────────────────────────────┐
│                    Raw Sources（原始资料）                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ 角色设定    │  │ 剧情对话    │  │ 角色故事    │     │
│  │ 官方资料    │  │ 游戏语音    │  │ 同人创作    │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
│  特点：不可变，LLM 只读不写                              │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼ 编译（Compile）
┌─────────────────────────────────────────────────────────┐
│                    Character Wiki（角色 Wiki）            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ 角色档案    │  │ 人际关系    │  │ 事件时间线  │     │
│  │ personality │  │ relationships│  │ timeline    │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ 经典台词    │  │ 情感记忆    │  │ 世界观      │     │
│  │ dialogues   │  │ emotions    │  │ worldview   │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
│  特点：LLM 编译，交叉引用，持续更新                      │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼ 存储（Store）
┌─────────────────────────────────────────────────────────┐
│                    SQLite Database（数据库）              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ pages       │  │ entities    │  │ relations   │     │
│  │ 知识页面    │  │ 实体        │  │ 关系        │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
│  特点：高效检索，结构化查询，向量索引                    │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼ 检索（Retrieve）
┌─────────────────────────────────────────────────────────┐
│                    Retrieval Engine（检索引擎）          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ 结构化查询  │  │ 语义检索    │  │ 混合检索    │     │
│  │ SQL         │  │ Vector      │  │ Hybrid      │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
└─────────────────────────────────────────────────────────┘
```

### 2.2 与 Karpathy 的对比

| 方面 | Karpathy LLM-Wiki | 我们的系统 |
|------|-------------------|-----------|
| 领域 | 通用知识库 | 角色知识库 |
| 原始资料 | 文章、论文、播客 | 角色设定、剧情对话、角色故事 |
| Wiki 页面 | 实体、概念、对比 | 角色档案、人际关系、事件时间线 |
| 存储 | 纯 Markdown 文件 | Markdown + SQLite |
| 检索 | index.md + qmd | 结构化查询 + 语义检索 |
| 维护 | LLM 自动维护 | LLM 编译 + 人工审核 |

**我们的改进**：
1. **数据库存储**：支持大规模数据（数万字）
2. **混合检索**：结构化查询 + 语义检索
3. **角色特化**：针对角色扮演场景优化

---

## 三、数据模型

### 3.1 Raw Sources（原始资料）

```
character_kb/
├── raw/
│   ├── hutao/
│   │   ├── official/
│   │   │   ├── character-profile.md    # 官方角色介绍
│   │   │   ├── voice-lines.md          # 角色语音
│   │   │   └── story-quest.md          # 角色任务
│   │   ├── lore/
│   │   │   ├── wangsheng-funeral.md    # 往生堂设定
│   │   │   └── liyue-harbor.md         # 璃月港设定
│   │   └── fan/
│   │       └── fan-interpretation.md   # 同人解读
│   └── kiana/
│       └── ...
└── ...
```

**特点**：
- 不可变（Immutable）
- LLM 只读不写
- 版本控制友好（Git）

### 3.2 Character Wiki（角色 Wiki）

```
character_kb/
├── wiki/
│   ├── hutao/
│   │   ├── profile.md              # 角色档案
│   │   ├── relationships.md        # 人际关系
│   │   ├── timeline.md             # 事件时间线
│   │   ├── dialogues.md            # 经典台词
│   │   ├── emotions.md             # 情感记忆
│   │   └── worldview.md            # 世界观/价值观
│   ├── kiana/
│   │   └── ...
│   └── shared/
│       ├── world-building.md       # 世界观
│       └── factions.md             # 阵营组织
└── ...
```

**页面类型**（参考 Karpathy）：

| 类型 | 说明 | 示例 |
|------|------|------|
| `profile` | 角色档案 | Entity Page |
| `relationships` | 人际关系 | Entity Page |
| `timeline` | 事件时间线 | Concept Page |
| `dialogues` | 经典台词 | Concept Page |
| `emotions` | 情感记忆 | Concept Page |
| `worldview` | 世界观/价值观 | Concept Page |
| `comparison` | 角色对比 | Comparison Page |

### 3.3 Navigation Files（导航文件）

#### index.md（角色索引）

```markdown
# Character Knowledge Base Index

> 角色知识库索引。每个角色页面按类型列出，附一行摘要。
> 最后更新：2026-06-14 | 总页面数：12

## Characters

### 原神
- [[hutao-profile]] - 往生堂第七十七代堂主，活泼开朗的殡仪馆掌门人
- [[hutao-relationships]] - 胡桃的人际关系：钟离、旅行者、七七
- [[hutao-timeline]] - 胡桃的事件时间线：继承往生堂、海灯节活动
- [[hutao-dialogues]] - 胡桃的经典台词：自我介绍、日常对话、关于生死
- [[hutao-emotions]] - 胡桃的情感记忆：对生死的理解、对璃月港的感情
- [[hutao-worldview]] - 胡桃的世界观：死亡不是终点，生命的意义

### 崩坏3
- [[kiana-profile]] - K423、天命女武神、终焉律者
- [[kiana-relationships]] - 琪亚娜的人际关系：芽衣、布洛妮娅、姬子
- [[kiana-timeline]] - 琪亚娜的事件时间线：发现K423身份、成为终焉律者
- [[kiana-dialogues]] - 琪亚娜的经典台词：本小姐、守护宣言
- [[kiana-emotions]] - 琪亚娜的情感记忆：对芽衣的感情、对世界的态度
- [[kiana-worldview]] - 琪亚娜的世界观：守护、牺牲、选择

## Shared
- [[world-building]] - 原神/崩坏3世界观
- [[factions]] - 阵营组织：往生堂、天命
```

#### log.md（更新记录）

```markdown
# Character Knowledge Base Log

> 角色知识库更新记录。只追加。
> 格式：`## [YYYY-MM-DD] action | subject`

## [2026-06-14] create | 知识库初始化
- 创建胡桃知识库（6 个页面）
- 创建琪亚娜知识库（6 个页面）
- 创建共享知识（2 个页面）

## [2026-06-14] ingest | 胡桃角色设定
- 更新 hutao-profile
- 更新 hutao-relationships
- 更新 hutao-dialogues
```

---

## 四、数据库设计

### 4.1 表结构

```sql
-- 知识页面表
CREATE TABLE character_pages (
    id TEXT PRIMARY KEY,                    -- 页面 ID（如 "hutao:profile"）
    character_id TEXT NOT NULL,             -- 角色 ID（如 "genshin:hutao"）
    page_type TEXT NOT NULL,                -- 页面类型（profile, relationships, timeline, dialogue, emotion, worldview）
    title TEXT NOT NULL,                    -- 页面标题
    content TEXT NOT NULL,                  -- Markdown 内容
    metadata TEXT,                          -- JSON 元数据（tags, confidence, sources）
    embedding BLOB,                         -- 向量嵌入（JSON 数组）
    created_at INTEGER NOT NULL,            -- 创建时间
    updated_at INTEGER NOT NULL,            -- 更新时间
    FOREIGN KEY (character_id) REFERENCES characters(id)
);

-- 实体表
CREATE TABLE entities (
    id TEXT PRIMARY KEY,                    -- 实体 ID（如 "hutao", "zhongli"）
    name TEXT NOT NULL,                     -- 实体名称
    type TEXT NOT NULL,                     -- 实体类型（character, location, organization, event）
    description TEXT,                       -- 实体描述
    metadata TEXT,                          -- JSON 元数据
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- 关系表
CREATE TABLE relationships (
    id TEXT PRIMARY KEY,                    -- 关系 ID
    from_entity TEXT NOT NULL,              -- 源实体
    to_entity TEXT NOT NULL,                -- 目标实体
    relationship_type TEXT NOT NULL,        -- 关系类型（friend, enemy, family, colleague, mentor）
    description TEXT,                       -- 关系描述
    metadata TEXT,                          -- JSON 元数据
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (from_entity) REFERENCES entities(id),
    FOREIGN KEY (to_entity) REFERENCES entities(id)
);

-- 事件表
CREATE TABLE events (
    id TEXT PRIMARY KEY,                    -- 事件 ID
    character_id TEXT NOT NULL,             -- 关联角色
    event_name TEXT NOT NULL,               -- 事件名称
    event_time TEXT,                        -- 事件时间（游戏内时间）
    description TEXT NOT NULL,              -- 事件描述
    significance TEXT,                      -- 事件意义
    metadata TEXT,                          -- JSON 元数据
    created_at INTEGER NOT NULL,
    FOREIGN KEY (character_id) REFERENCES characters(id)
);

-- 台词表
CREATE TABLE dialogues (
    id TEXT PRIMARY KEY,                    -- 台词 ID
    character_id TEXT NOT NULL,             -- 关联角色
    scene TEXT,                             -- 场景描述
    content TEXT NOT NULL,                  -- 台词内容
    emotion TEXT,                           -- 情绪标签
    tags TEXT,                              -- JSON 标签数组
    source TEXT,                            -- 来源
    created_at INTEGER NOT NULL,
    FOREIGN KEY (character_id) REFERENCES characters(id)
);

-- 原始资料表
CREATE TABLE raw_sources (
    id TEXT PRIMARY KEY,                    -- 资料 ID
    character_id TEXT NOT NULL,             -- 关联角色
    source_type TEXT NOT NULL,              -- 资料类型（official, lore, fan）
    title TEXT NOT NULL,                    -- 资料标题
    content TEXT NOT NULL,                  -- 资料内容
    sha256 TEXT,                            -- 内容哈希（用于变更检测）
    metadata TEXT,                          -- JSON 元数据
    created_at INTEGER NOT NULL,
    FOREIGN KEY (character_id) REFERENCES characters(id)
);
```

### 4.2 索引设计

```sql
-- 页面索引
CREATE INDEX idx_pages_character ON character_pages(character_id);
CREATE INDEX idx_pages_type ON character_pages(page_type);
CREATE INDEX idx_pages_character_type ON character_pages(character_id, page_type);

-- 实体索引
CREATE INDEX idx_entities_type ON entities(type);

-- 关系索引
CREATE INDEX idx_relationships_from ON relationships(from_entity);
CREATE INDEX idx_relationships_to ON relationships(to_entity);
CREATE INDEX idx_relationships_type ON relationships(relationship_type);

-- 事件索引
CREATE INDEX idx_events_character ON events(character_id);
CREATE INDEX idx_events_time ON events(event_time);

-- 台词索引
CREATE INDEX idx_dialogues_character ON dialogues(character_id);
CREATE INDEX idx_dialogues_emotion ON dialogues(emotion);

-- 原始资料索引
CREATE INDEX idx_raw_character ON raw_sources(character_id);
CREATE INDEX idx_raw_type ON raw_sources(source_type);
```

---

## 五、页面格式规范

### 5.1 Frontmatter（前置元数据）

```yaml
---
title: 胡桃 - 角色档案
character_id: genshin:hutao
page_type: profile
created: 2026-06-14
updated: 2026-06-14
tags: [原神, 往生堂, 璃月港]
confidence: high
sources: [raw/hutao/official/character-profile.md]
---
```

### 5.2 页面结构

```markdown
# 胡桃（Hu Tao）

## 基本信息
- **全名**：胡桃
- **称号**：雪霁梅香
- **身份**：往生堂第七十七代堂主
...

## 性格特征
### 活泼开朗
...

## 核心矛盾
> 活泼开朗到让人忘记她是殡仪馆的堂主

## 相关页面
- [[hutao-relationships]] - 人际关系
- [[hutao-timeline]] - 事件时间线
...
```

### 5.3 交叉引用

**规则**：
- 每个页面至少 2 个出链（`[[wikilinks]]`）
- 使用双向链接（A 链接到 B，B 也链接到 A）
- 定期检查断链

---

## 六、检索策略

### 6.1 结构化查询

```kotlin
// 查询角色档案
val profile = knowledgeDao.getPage("genshin:hutao", "profile")

// 查询人际关系
val relationships = knowledgeDao.getRelationships("genshin:hutao")

// 查询特定关系
val zhongliRelation = knowledgeDao.getRelationship("genshin:hutao", "genshin:zhongli")

// 查询事件时间线
val timeline = knowledgeDao.getPage("genshin:hutao", "timeline")

// 查询特定事件
val events = knowledgeDao.getEvents("genshin:hutao", "海灯节")

// 查询台词
val dialogues = knowledgeDao.getDialogues("genshin:hutao", emotion = "活泼")
```

### 6.2 语义检索

```kotlin
// 查询与"生死观"相关的内容
val results = knowledgeDao.semanticSearch(
    query = "生死观",
    characterId = "genshin:hutao",
    pageTypes = listOf("profile", "emotions", "worldview"),
    topK = 5
)

// 查询与"守护"相关的内容
val results = knowledgeDao.semanticSearch(
    query = "守护重要的人",
    characterId = "honkai3rd:kiana",
    topK = 3
)
```

### 6.3 混合检索

```kotlin
// 混合检索：关键词 + 语义
val results = knowledgeDao.hybridSearch(
    query = "胡桃对钟离的态度",
    characterId = "genshin:hutao",
    topK = 5
)

// 检索策略：
// 1. 关键词匹配（BM25）
// 2. 语义检索（向量相似度）
// 3. 合并结果（去重 + 排序）
```

### 6.4 上下文注入

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
    
    // 4. 历史消息
    messages.addAll(history)
    
    // 5. 用户当前消息
    messages.add(AiMessage(role = "user", content = userMessage))
    
    return messages
}
```

---

## 七、编译流程

### 7.1 Ingest（导入）

```kotlin
class KnowledgeCompiler(
    private val knowledgeDao: KnowledgeDao,
    private val embeddingService: EmbeddingService,
) {
    suspend fun ingest(characterId: String, sourcePath: String) {
        // 1. 读取原始资料
        val source = File(sourcePath).readText()
        val metadata = parseFrontmatter(source)
        val content = removeFrontmatter(source)
        
        // 2. 保存原始资料
        val rawSource = RawSourceEntity(
            id = "$characterId:${File(sourcePath).nameWithoutExtension}",
            characterId = characterId,
            sourceType = metadata["type"] ?: "unknown",
            title = metadata["title"] ?: File(sourcePath).nameWithoutExtension,
            content = content,
            sha256 = sha256(content),
            metadata = Json.encodeToString(metadata),
            createdAt = System.currentTimeMillis(),
        )
        knowledgeDao.insertRawSource(rawSource)
        
        // 3. 提取实体和关系
        val entities = extractEntities(content)
        val relationships = extractRelationships(content)
        
        // 4. 编译知识页面
        val pages = compilePages(characterId, content, entities, relationships)
        
        // 5. 生成向量嵌入
        for (page in pages) {
            val embedding = embeddingService.embed(page.content)
            page.embedding = Json.encodeToString(embedding)
        }
        
        // 6. 写入数据库
        for (page in pages) {
            knowledgeDao.insertPage(page)
        }
        for (entity in entities) {
            knowledgeDao.insertEntity(entity)
        }
        for (relationship in relationships) {
            knowledgeDao.insertRelationship(relationship)
        }
        
        // 7. 更新导航文件
        updateIndex()
        appendLog("ingest", sourcePath)
    }
}
```

### 7.2 Compile（编译）

```kotlin
fun compilePages(
    characterId: String,
    content: String,
    entities: List<Entity>,
    relationships: List<Relationship>,
): List<CharacterPageEntity> {
    val pages = mutableListOf<CharacterPageEntity>()
    
    // 1. 编译角色档案
    pages.add(compileProfile(characterId, content, entities))
    
    // 2. 编译人际关系
    pages.add(compileRelationships(characterId, relationships))
    
    // 3. 编译事件时间线
    pages.add(compileTimeline(characterId, content))
    
    // 4. 编译经典台词
    pages.add(compileDialogues(characterId, content))
    
    // 5. 编译情感记忆
    pages.add(compileEmotions(characterId, content))
    
    // 6. 编译世界观
    pages.add(compileWorldview(characterId, content))
    
    return pages
}
```

### 7.3 Lint（健康检查）

```kotlin
class KnowledgeLinter(
    private val knowledgeDao: KnowledgeDao,
) {
    suspend fun lint(): LintReport {
        val issues = mutableListOf<LintIssue>()
        
        // 1. 检查断链
        issues.addAll(checkBrokenLinks())
        
        // 2. 检查孤儿页面
        issues.addAll(checkOrphanPages())
        
        // 3. 检查缺失交叉引用
        issues.addAll(checkMissingCrossReferences())
        
        // 4. 检查过时内容
        issues.addAll(checkStaleContent())
        
        // 5. 检查矛盾
        issues.addAll(checkContradictions())
        
        return LintReport(issues)
    }
}
```

---

## 八、实施计划

### Phase 1: 数据库设计（1天）
- 设计表结构
- 创建 Entity 和 DAO
- 添加数据库迁移

### Phase 2: 编译器实现（2-3天）
- Markdown 解析器
- 实体提取器
- 关系提取器
- 页面编译器

### Phase 3: 检索引擎实现（2-3天）
- 结构化查询
- 语义检索
- 混合检索策略
- 上下文注入

### Phase 4: 知识库内容（3-5天）
- 编写胡桃知识库
- 编写琪亚娜知识库
- 测试编译和检索

### Phase 5: 集成测试（2-3天）
- 与 StarRailChatBox 集成
- 测试对话效果
- 优化检索策略

---

## 九、参考文献

1. Karpathy, A. (2026). LLM Wiki. GitHub Gist. https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f

2. PyShine. (2026). Karpathy's LLM Wiki: Build a Compounding Knowledge Base With Your AI Agent. https://pyshine.com/Karpathy-LLM-Wiki-Compounding-Knowledge-Base

3. Decode the Future. (2026). LLM Wiki: Karpathy's 3-Layer Pattern That Replaces RAG. https://decodethefuture.org/en/llm-wiki-karpathy-pattern

4. Fulkerson, A. (2026). Karpathy's Pattern for an "LLM Wiki" in Production. https://aaronfulkerson.com/2026/04/12/karpathys-pattern-for-an-llm-wiki-in-production

5. wuphf. (2026). A Karpathy-style LLM wiki your agents maintain. https://github.com/nex-crm/wuphf

---

最后更新：2026-06-14
