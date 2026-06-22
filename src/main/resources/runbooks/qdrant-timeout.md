---
id: RB-QDRANT-TIMEOUT
title: Qdrant 查询超时处理手册
component: qdrant
alertTypes:
  - HIGH_LATENCY
rootCauses:
  - QDRANT_TIMEOUT
tags:
  - qdrant
  - vector-search
  - timeout
---

## 检索描述
适用于向量检索接口高延迟、Qdrant query timeout、vector search 超时场景。

## 典型现象
AI 接口延迟升高，qdrant_search span 明显慢于 llm_generate，检索请求可能超时。

## 信号模式
trace 出现 qdrant_search 高耗时或 ERROR；qdrant_search_latency_ms 升高；日志包含 Qdrant query timeout。

## 常见原因
Collection 负载过高、索引未就绪、网络抖动或查询 topK 过大。

## 排查步骤
检查最慢 qdrant_search span、collection 状态、节点资源和查询参数。

## 临时处理
临时降低 topK，并启用关键词检索 fallback。

## 风险操作
禁止在未确认副本健康时重建或删除 collection。

## 人工接管条件
Collection 不可用、数据副本异常或需要执行索引重建时人工接管。

## 最终建议模板
降低 topK，启用关键词检索 fallback，并检查 Qdrant collection 与节点资源状态。
