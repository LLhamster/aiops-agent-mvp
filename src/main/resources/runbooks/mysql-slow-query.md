---
id: RB-MYSQL-SLOW-QUERY
title: MySQL 慢查询处理手册
component: mysql
alertTypes:
  - HIGH_LATENCY
rootCauses:
  - MYSQL_SLOW_QUERY
tags:
  - mysql
  - slow-query
  - latency
---

## 检索描述
适用于数据库查询导致的接口高延迟、MySQL slow query 和索引未命中场景。

## 典型现象
mysql_query span 占据请求大部分耗时，图书搜索接口响应变慢。

## 信号模式
trace 中 mysql_query 高耗时；日志出现 Slow query detected；mysql_query_latency_ms 升高。

## 常见原因
缺少索引、模糊查询范围过大、执行计划退化或数据库负载过高。

## 排查步骤
检查 SQL、执行计划、索引命中情况及数据库连接池水位。

## 临时处理
限制模糊查询范围、增加请求缓存或临时限流。

## 风险操作
禁止在高峰期直接创建大表索引或执行无条件全表更新。

## 人工接管条件
需要在线 DDL、数据修复或数据库扩容时人工接管。

## 最终建议模板
检查 books.title 索引与执行计划，限制模糊查询范围并评估全文索引。
