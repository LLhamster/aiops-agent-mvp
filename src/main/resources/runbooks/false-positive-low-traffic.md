---
id: RB-FALSE-POSITIVE-LOW-TRAFFIC
title: 低流量错误率误报处理手册
component: book-service
alertTypes:
  - HIGH_ERROR_RATE
rootCauses:
  - FALSE_POSITIVE_LOW_TRAFFIC
tags:
  - low-traffic
  - error-rate
  - false-positive
---

## 检索描述
适用于低流量窗口中少量错误导致错误率告警放大的 false positive 场景。

## 典型现象
错误率看似很高，但 request_count 很小且 error_count 只有少量请求。

## 信号模式
error_rate 升高，同时 request_count 很低；日志仅有零星 404 或业务可接受错误。

## 常见原因
告警只使用比例阈值，未设置最小请求量或错误数门槛。

## 排查步骤
同时核对 request_count、error_count、错误码分布和历史流量基线。

## 临时处理
为低流量窗口增加最小请求量条件，结合错误数判断。

## 风险操作
禁止仅因一次误报永久关闭错误率告警。

## 人工接管条件
错误包含数据损坏、安全风险或持续扩大时人工接管。

## 最终建议模板
按最小请求量设置告警门槛，低流量窗口使用错误数与错误率组合条件。
