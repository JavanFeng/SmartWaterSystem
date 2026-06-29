# SmartWaterSystem
SmartWaterSystem: 基于 Spring AI Alibaba + StateGraph + RAG 检索增强,智慧水务多 Agent 协作平台。支持多意图识别，知识库问答，水质污染分析等。

## 功能概述

- **AI 智能对话**：基于通义千问大模型的多轮流式对话，支持意图识别与任务分发
- **水质实时监控**：监测 pH 值、溶解氧、浊度、温度、电导率、氨氮等核心指标(模拟数据)
- **智能工单处理**：异常告警自动识别，支持工单创建、分配与跟踪
- **污染物溯源分析**：按站点、日期、小时维度进行水质污染分析
- **RAG 知识问答**：基于 Milvus 向量数据库的文档检索增强问答
- **多角色权限**：管理员、分析员、普通用户三级权限控制（static）

## 你将学习一下知识

- 意图识别、任务分发、状态机、RAG 知识问答、多角色权限控制...
- 提示词编写规则（工程）
-  MCP
- 人工对话介入，人工表单介入...
- Rule和LLm结合降低token消耗
- 查询重写,查询扩写提高RAG检索效果
- TOKEN的压缩（业务实体抽取，上下文摘要等），防止上下文爆炸
- ...

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (water-quality.html)               │
│              ┌─────────────┐    ┌─────────────┐              │
│              │  智能对话窗口  │    │ 实时监测面板  │              │
│              └─────────────┘    └─────────────┘              │
└──────────────────────────────┬──────────────────────────────┘
                               │ SSE 流式接口
┌──────────────────────────────▼──────────────────────────────┐
│              smart-water-system (端口 8989)                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Chat API   │  │  Graph 工作流 │  │   RAG / VectorStore │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│                    MCP Client │ ASYNC                        │
└───────────────────────────────┼──────────────────────────────┘
                                │
┌───────────────────────────────▼──────────────────────────────┐
│          smart-water-tools-mcp-server (端口 9998)            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐  │
│  │ 设备历史数据查询  │  │ 断面关联数据查询  │  │ ......  │  │
│  └─────────────────┘  └─────────────────┘  └─────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 基础框架 | Java 17, Spring Boot 3.5.14 |
| AI 框架 | Spring AI 1.1.0, Spring AI Alibaba 1.1.2.2 |
| 大模型 | 阿里通义千问 (DashScope) qwen-plus |
| 向量数据库 | Milvus v3.0-beta |
| 工具协议 | Model Context Protocol (MCP) |
| 前端 | 原生 HTML5 / CSS3 / JavaScript |

## 项目结构

```
SmartWaterSystem/
├── smart-water-system/              # 主应用模块
│   ├── src/main/java/...
│   │   ├── graph/                   # Graph 工作流引擎
│   │   │   ├── dispatcher/          # 意图/任务分发器
│   │   │   └── node/                # 工作流节点
│   │   ├── agent/                   # AI Agent
│   │   │   ├── analysis/            # 分析 Agent
│   │   │   ├── data/                # 数据 Agent
│   │   │   └── work/                # 工单 Agent
│   │   ├── rag/                     # RAG 检索配置
│   │   ├── tool/                    # 本地工具类
│   │   ├── view/                    # Web 接口层
│   │   └── auth/                    # 权限控制
│   └── src/main/resources/
│       ├── static/water-quality.html # 监控前端页面
│       ├── prompts/                  # AI 提示词模板
│       └── application.yml
├── smart-water-tools-mcp-server/    # MCP 工具服务
│   └── src/main/java/.../tool/      # 水务数据工具
├── script/
│   └── docker-compose.yml           # Milvus + MinIO + etcd
└── pom.xml                          # Maven 多模块配置
```

## 快速开始

### 前置依赖

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. 启动基础设施

```bash
cd script
docker-compose up -d
```

启动 Milvus 向量数据库、MinIO 对象存储和 etcd 服务。

### 2. 配置环境变量

```bash
export AI_DASHSCOPE_API_KEY=your-dashscope-api-key
```

或修改 `smart-water-system/src/main/resources/application.yml` 中的 `spring.ai.dashscope.api-key`。

### 3. 编译运行

```bash
# 编译整个项目
mvn clean install

# 启动 MCP 工具服务
cd smart-water-tools-mcp-server
mvn spring-boot:run

# 启动主应用
cd ../smart-water-system
mvn spring-boot:run
```

主应用启动后将自动打开：http://localhost:8989/water-quality.html

### 4. 用户登录

| 用户名 | Token | 角色 |
|--------|-------|------|
| admin | admin | 管理员 |
| user | user | 普通用户 |
| analysis | analysis | 分析员 |

## 核心 API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chat/session/create` | GET | 创建会话 |
| `/api/chat/stream` | POST | 流式对话 (SSE) |
| `/api/chat/history` | GET | 获取历史消息 |
| `/api/chat/orderAssign/resume` | POST | 工单任务确认继续 |
| `/api/chat/analysis/resume` | POST | 分析任务确认继续 |

## 工作流说明

系统采用 Graph 状态机驱动多 Agent 协作：

1. **意图识别**：用户输入首先进入 `IntentNode`，由大模型分析用户意图
2. **任务路由**：`IntentDispatcher` 根据意图类型路由到不同分支
3. **Agent 执行**：
    - **Chat** → 直接对话回复
    - **Q&A** → `QARagRetrieveNode` 检索知识库后回答
    - **工单** → `HandleOrderNode` 调用 MCP 工具创建工单（需人工确认）
    - **分析** → `AnalysisNode` 进行污染物溯源分析（需人工确认参数）
4. **人机协同**：关键操作通过 `human` 事件中断，等待用户确认后继续(表单补充或者对话补充)

## 提示词配置

位于 `smart-water-system/src/main/resources/prompts/`：

- `intent-prompt.md` — 意图识别提示词
- `analysis-intent-prompt.md` — 分析意图识别
- `analysis-instruction.md` — 水质分析指令
- `handle-order-instruction.md` — 工单处理指令
- `tool-instruction.md` — 工具调用指令

## 开发扩展

### 添加新的 MCP 工具

在 `smart-water-tools-mcp-server` 中实现 `IMcpTool` 接口：

```java
@Component
public class MyNewTool implements IMcpTool {
    @Tool(description = "工具描述")
    public MyResult query(...) {
        // 业务逻辑
    }
}
```

## 注意事项

- 向量库初始化会调用 Embedding 接口消耗 Token，可通过 `water.qa.vector.init.enable=false` 关闭
- MCP 客户端配置为 ASYNC 模式，确保与 MCP Server 的异步通信
- 会话历史默认存储在内存中，生产环境建议接入持久化存储
- 为了便于理解，项目中去除了所有数据库等依赖，业务数据都以模拟值为主

## TODO（后续可能会补充）
- [ ] 接入observation
- [ ] 接入 skill 机制
- [ ] 接入 experience 机制
- [ ] ...

## 界面预览
<img width="1882" height="864" alt="image" src="https://github.com/user-attachments/assets/e0136aac-4afe-464e-84c0-2c3c78750fd5" />


