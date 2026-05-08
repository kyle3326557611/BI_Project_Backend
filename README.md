# 📊 Intelligent BI - 基于大模型的智能数据分析平台 (Backend)

## 📖 项目简介
本项目是一个基于 Spring Boot 3 与大语言模型（LLM）构建的智能数据分析平台。
用户只需导入原始业务数据（Excel/CSV）并输入分析诉求，系统即可依托 AI 在后台自动进行数据清洗、分析，并动态生成 Echarts 可视化图表代码与深度分析结论。

系统采用**微服务架构思想**进行了深度优化，通过引入 RabbitMQ 与 JUC 线程池实现了长耗时任务的异步调度与削峰填谷，并基于 Redisson 实现了接口级的分布式防刷限流，保障了系统的极高可用性。

## 🛠️ 核心技术栈
* **底层框架**: Spring Boot 3.x, Spring MVC
* **持久层**: MySQL 8.x, MyBatis-Plus
* **缓存与分布式**: Redis, Redisson
* **消息队列**: RabbitMQ
* **并发编程**: JUC (ThreadPoolExecutor, CompletableFuture)
* **工具与组件**: Hutool, EasyExcel, Knife4j (Swagger 3)

## 🔥 核心架构与技术亮点

### 1. 大模型 Prompt 强约束与物理防御机制
* **痛点**：大模型输出格式极度不稳定，常附带 Markdown 标记（如 ` ```json `）或不可见的前导空格，导致前端 `JSON.parse` 频繁崩溃。
* **解决方案**：在系统级 Prompt 中构建极强的格式约束边界（采用特殊分隔符）；在后端响应层，采用 `indexOf` 物理截取配合正则表达式 `replaceAll`，实现对脏数据的 100% 清洗，保障了前端图表渲染的绝对稳定性。

### 2. JUC 线程池与 CompletableFuture 异步化改造
* **痛点**：AI 生成图表单次调用耗时达 15s+，传统同步架构极易导致 Tomcat 线程池耗尽，引发系统假死与 `504 Gateway Timeout`。
* **解决方案**：废弃 Spring 默认的 `@Async`，自定义 `ThreadPoolExecutor`。利用 `ArrayBlockingQueue` 有界队列防止内存溢出（OOM），配合 `CompletableFuture.runAsync` 实现主线程的毫秒级返回，并利用 `exceptionally` 捕获 AI 调用超时异常，实现死信任务的兜底更新。

### 3. RabbitMQ 异步解耦与削峰填谷
* **痛点**：突发的高并发请求会导致 AI API 被限流，且 JUC 内存队列在服务器宕机时会造成任务永久丢失。
* **解决方案**：引入 RabbitMQ 重构异步逻辑。通过 `DirectExchange` 实现精准路由；利用 MQ 的磁盘持久化特性保证断电不丢单；消费者端开启 **Manual ACK（手动确认）** 机制，确保只有在数据成功落库后才确认消费，实现了分布式架构下的最终一致性。

### 4. Redisson 分布式限流与防刷机制
* **痛点**：大模型 API 按 Token 计费，恶意用户高频点击会导致资金瞬间刷爆及服务器崩溃。
* **解决方案**：引入 Redisson 框架，基于 `RRateLimiter` 实现全局分布式限流。以 `方法名 + 用户ID` 为复合 Key，严格限制单一用户在指定时间窗口内的请求频率。

### 5. 分布式无状态鉴权与安全性加固
* **状态管理**：废弃传统的单机 Tomcat Session，采用 `UUID Token + Redis` 的架构实现用户会话的无状态化管理，完美支持集群水平扩容。并利用 Redis 的 `expire` 指令实现活跃用户的会话自动续签。
* **数据脱敏**：存储层抛弃明文，使用 `MD5 + 随机盐值 (Salt)` 算法单向散列加密核心凭证，彻底阻断彩虹表反向破解。

## ⚙️ 快速启动

1. 在本地启动 MySQL (3306) 与 Redis (6379)。
2. 安装并启动 RabbitMQ，并在管理面板中确认 `bi_exchange` 与 `bi_queue` 的绑定关系（或由程序自动初始化）。
3. 修改 `application.yml` 中的数据库配置与智谱 AI 的 `api-key`。
4. 运行 `MainApplication.java`。
5. 访问 `http://localhost:8080/api/doc.html` 查看 Knife4j 接口文档并进行调试。
