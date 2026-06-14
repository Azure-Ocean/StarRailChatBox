# Karpathy LLM-Wiki 研究笔记

## 原始资源

- **Karpathy 的 Gist**: https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f
- **发布时间**: 2026 年 4 月
- **Stars**: 5000+（第一周）

## 核心思想

> "The wiki is a persistent, compounding artifact. Cross-references are already there. Contradictions have already been flagged. The synthesis already reflects everything you have read."

**关键洞察**：从"检索"（RAG）转向"编译 + 维护"（LLM Wiki）

- **RAG**: 每次查询都从原始文档中检索，知识不积累
- **LLM Wiki**: 一次性编译，持续维护，知识不断复利

## 三层架构

```
┌─────────────────────────────────────────────────────────┐
│                    Raw Sources（原始资料）                │
│  - 文章、论文、图片、播客、书籍                           │
│  - 不可变（Immutable）                                   │
│  - LLM 只读不写                                         │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                    The Wiki（Wiki 页面）                  │
│  - LLM 生成的 Markdown 文件                              │
│  - 页面类型：摘要、实体、概念、对比、综合                 │
│  - LLM 负责创建、更新、维护交叉引用                      │
│  - 一个资料源可能更新 10-15 个页面                        │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                    The Schema（配置文件）                 │
│  - CLAUDE.md / AGENTS.md                                │
│  - 定义 Wiki 结构、约定和工作流                          │
│  - 人和 LLM 共同演进                                     │
└─────────────────────────────────────────────────────────┘
```

## 页面类型

### 1. Entity Pages（实体页面）
- 一个实体一个页面
- 包含：概述、关键事实、关系、来源引用
- 示例：人物、组织、产品、模型

### 2. Concept Pages（概念页面）
- 一个概念一个页面
- 包含：定义、当前状态、开放问题、相关概念
- 示例：技术概念、理论框架

### 3. Comparison Pages（对比页面）
- 并排对比分析
- 包含：比较维度、对比表格、结论、来源
- 示例：产品对比、方案对比

### 4. Summary Pages（摘要页面）
- 主题摘要
- 包含：关键要点、时间线、相关页面

### 5. Synthesis Pages（综合页面）
- 跨页面的综合分析
- 包含：洞察、趋势、预测

## 导航文件

### index.md（内容目录）
```markdown
# Wiki Index

> 内容目录。每个 Wiki 页面按类型列出，附一行摘要。
> 首先阅读此文件以找到相关页面。
> 最后更新：YYYY-MM-DD | 总页面数：N

## Entities
- [[entity-name]] - 一行摘要

## Concepts
- [[concept-name]] - 一行摘要

## Comparisons
- [[comparison-name]] - 一行摘要
```

### log.md（时间线记录）
```markdown
# Wiki Log

> 所有 Wiki 操作的时间记录。只追加。
> 格式：`## [YYYY-MM-DD] action | subject`
> 操作：ingest, update, query, lint, create, archive, delete

## [2026-04-02] ingest | Article Title
- 创建/更新的文件列表
```

## 三个核心操作

### 1. Ingest（导入）
**流程**：
1. 人将资料放入 `raw/` 目录
2. LLM 读取资料
3. 提取关键信息
4. 更新 Wiki 页面（10-15 个）
5. 更新 `index.md`
6. 追加 `log.md`

**最佳实践**：
- 一次导入一个资料源
- 可以批量导入，但需要更少的监督

### 2. Query（查询）
**流程**：
1. 人提出问题
2. LLM 搜索 `index.md`
3. 深入相关页面
4. 综合答案并引用来源
5. **重要**：好的答案应该作为新页面归档

**关键洞察**：探索和导入的资料一样，都能积累知识

### 3. Lint（健康检查）
**检查内容**：
- 矛盾
- 过时声明
- 孤儿页面（无入链）
- 缺失交叉引用
- 数据缺口

**输出**：
- 修复建议
- 新问题建议
- 新资料源建议

## 人机分工

### 人的工作
- 策展资料源（寻找和选择材料）
- 探索和提问
- 引导分析（告诉 LLM 什么重要）
- 审查更新（阅读结果，提供反馈）

### LLM 的工作
- 总结资料
- 交叉引用和更新页面
- 标记矛盾
- 执行健康检查

**隐喻**：`Obsidian = IDE，LLM = 程序员，Wiki = 代码库`

## 技术实现

### 存储
- **Markdown 文件**：人类可读，版本控制友好
- **Git**：版本管理，协作
- **Obsidian**：阅读界面，图谱视图

### 索引
- **index.md**：内容目录，~100 个资料源，数百页面
- **log.md**：时间线记录，grep 可解析

### 检索
- **qmd**：本地 Markdown 搜索引擎
  - 混合 BM25/向量搜索
  - LLM 重排序
  - CLI 和 MCP 接口

## 规模限制

- **最佳规模**：< 100,000 tokens
- **中等规模**：~100 个资料源，数百页面
- **大规模**：需要额外索引（向量数据库）

## 为什么有效

> "知识库维护的繁琐之处不在于阅读或思考，而在于记账 — 更新交叉引用、保持摘要最新、标记矛盾。人类放弃 wiki 是因为维护负担增长快于价值。LLM 不会厌倦，不会忘记更新交叉引用，一次可以修改 15 个文件。wiki 之所以能保持更新，是因为维护成本接近于零。"

**关键优势**：
- LLM 处理繁琐的记账工作
- 交叉引用自动维护
- 矛盾自动标记
- 维护成本接近零

## 历史渊源

**Vannevar Bush 的 Memex（1945）**
- 私人、策展的知识存储
- 关联路径
- LLM 解决了 Bush 未解决的问题：谁来做维护

## 相关项目

### wuphf
- **GitHub**: https://github.com/nex-crm/wuphf
- **实现**: Markdown + Git + Bleve (BM25) + SQLite
- **特点**:
  - 每个 Agent 有私人笔记本
  - 草稿到 Wiki 的晋升流程
  - 每个实体的事实日志（JSONL）
  - 每日 lint cron

### llm-wiki-karpathy-plugin
- **ClawHub**: clawhub:llm-wiki-karpathy-plugin
- **实现**: CLI + MCP 服务器
- **特点**:
  - 多模态支持（文本、PDF、图片、结构化数据）
  - 清单 schema v2
  - 表示存储
  - 编译就绪性跟踪

## 我们的启示

### 适用于 StarRailChatBox 的设计

1. **三层架构**：
   - Raw: 原作资料（角色设定、剧情对话、角色故事）
   - Wiki: 编译后的知识页面（角色档案、人际关系、事件时间线）
   - Schema: 组织规则

2. **页面类型**：
   - Entity: 角色页面
   - Concept: 设定概念页面
   - Comparison: 角色对比页面
   - Timeline: 事件时间线

3. **导航文件**：
   - index.md: 角色索引
   - log.md: 更新记录

4. **三个操作**：
   - Ingest: 导入原作资料
   - Query: 查询角色知识
   - Lint: 健康检查

5. **人机分工**：
   - 人：策展原作资料，引导分析
   - LLM：编译知识，维护交叉引用

---

参考文献：
1. Karpathy, A. (2026). LLM Wiki. GitHub Gist. https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f
2. PyShine. (2026). Karpathy's LLM Wiki: Build a Compounding Knowledge Base With Your AI Agent. https://pyshine.com/Karpathy-LLM-Wiki-Compounding-Knowledge-Base
3. Decode the Future. (2026). LLM Wiki: Karpathy's 3-Layer Pattern That Replaces RAG. https://decodethefuture.org/en/llm-wiki-karpathy-pattern
4. Fulkerson, A. (2026). Karpathy's Pattern for an "LLM Wiki" in Production. https://aaronfulkerson.com/2026/04/12/karpathys-pattern-for-an-llm-wiki-in-production
5. wuphf. (2026). A Karpathy-style LLM wiki your agents maintain. https://github.com/nex-crm/wuphf
