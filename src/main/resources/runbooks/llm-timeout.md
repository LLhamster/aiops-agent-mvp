---
id: RB-LLM-TIMEOUT
title: LLM Provider 超时处理手册
component: llm
alertTypes:
  - HIGH_LATENCY
rootCauses:
  - LLM_TIMEOUT
tags:
  - llm
  - provider
  - timeout
---

## 检索描述
适用于大模型生成阶段高延迟、LLM provider timeout 或上游模型服务超时。

## 典型现象
llm_generate span 耗时显著升高，而向量检索阶段正常。

## 信号模式
trace 中 llm_generate 为最慢或 ERROR；日志包含 LLM provider timeout；provider latency 升高。

## 常见原因
模型供应商拥塞、客户端超时过短、网络异常或生成 token 数过多。

## 排查步骤
检查 provider 状态、llm_generate span、超时配置和请求 token 规模。

## 临时处理
启用备用模型、缩短上下文或返回降级响应。

## 风险操作
切换模型前需确认输出兼容性与数据合规要求。

## 人工接管条件
供应商持续不可用或涉及跨区域、跨供应商切换时人工接管。

## 最终建议模板
启用备用模型或降级响应，并检查 LLM provider 状态与客户端超时配置。
