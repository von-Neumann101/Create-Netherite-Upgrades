# 下界合金下界合金实施计划

> **For agentic workers:** Use superpowers:executing-plans inline, followed by one fresh-context whole-change review.

**Goal:** 新增用户指定趣味物品，可连续啃食不消耗，超过10秒获得紫色挑战进度。

**Architecture:** 普通自定义Item使用原生32tick EAT动作；NeoForge使用tick事件在剩余1tick时续到33，保持使用状态并循环原生动画。服务端单次使用起点计时，超过200tick授予真实challenge进度。

**Tech Stack:** Minecraft1.21.1 / NeoForge21.1.219 / Create6.0.10-280 / Java21。

**Spec:** 名称“下界合金下界合金”；下界合金锭+下界合金板合成；按住右键一直食用但不消耗；右键超过10s获得紫色“你的牙还好吗？”；Shift原文“你为什么要做这个？”。

- ID `netherite_netherite`，版本0.3.11，配方无序1+1输出1，沿用防火、防爆和创造栏。
- 不添加食物组件，不改变饥饿/饱和/生命值，满饥饿也可使用，主副手均支持。完成路径返回原堆叠。
- 只续期本物品且duration恰为1的使用事件，保留取消或非正duration的停止语义。松开停止循环；新的使用重新计时，不累计多次短按。
- 严格超过10秒，即当前游戏时间减使用起点>200tick。服务端授予 `are_your_teeth_ok` 的impossible criterion；frame为challenge，标题严格使用中文原文，不额外奖励XP。
- 物品Shift概要严格为原文，复用Create原生Shift显示；模型/PNG由指定美术对话提供，旧图不改。禁止RTX5060，不启动客户端。

## 实施与验证

- [x] 真实服务器测试先验证新物品未注册失败。
- [x] 注册物品、无限动画续期和连续服务端计时/挑战进度，接入配方及原生Shift。
- [x] 验证200/201tick边界、松开重置、主副手、计数/组件及饥饿保持，真实advancement与紫色frame。
- [x] 接入新贴图与0.3.11 README/技术文档，完成构建/GameTest/独立审查及包核对后自动commit/push。


## 验证记录

2026-11-04：物品未注册时先失败，实现后基础用例通过；进度测试使用真实ServerPlayer（NeoForge拒绝FakePlayer进度），全部33项GameTest及205项既有回归通过。实际循环验证200/201tick边界、松开/副手重新计时、260tick后持续不消耗/不加食物。独立审查无可操作问题。143个JSON、各102键、202个资源与0.3.11元数据/编译类一致；测试类/QA配方/结构未打包。57张旧PNG不变，新16×16图有透明背景，中文名称/标题/概要原文一致。不启动客户端、不使用RTX5060。
