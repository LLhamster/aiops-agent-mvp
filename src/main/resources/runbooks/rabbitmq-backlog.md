---
id: RB-RABBITMQ-BACKLOG
title: RabbitMQ 队列积压处理手册
component: rabbitmq
alertTypes:
  - QUEUE_BACKLOG
rootCauses:
  - CONSUMER_FAILURE
tags:
  - rabbitmq
  - queue
  - backlog
  - consumer
---

## 检索描述
适用于 RabbitMQ queue backlog、消费者故障和消息消费速率持续低于生产速率。

## 典型现象
队列长度持续增长，read task queue 消费停滞或 consumer crash。

## 信号模式
queue_length 升高、consume_rate 低于 produce_rate；日志包含 Consumer crashed 或 connection reset。

## 常见原因
消费者进程崩溃、连接中断、毒消息反复重试或消费端容量不足。

## 排查步骤
检查消费者存活、连接、死信队列、失败消息和生产消费速率差。

## 临时处理
确认消息安全后逐步重启消费者，并受控扩容消费实例。

## 风险操作
禁止未经确认直接清空队列、批量 ack 或无限扩容消费者。

## 人工接管条件
消费者持续崩溃、存在毒消息、需要清理队列或修改消费语义时人工接管。

## 最终建议模板
人工确认失败消息安全性后重启消费者，检查连接与死信队列并逐步扩容消费端。
