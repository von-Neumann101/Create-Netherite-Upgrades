# 下界合金便捷应力装置 Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan in the current checkout.

**Goal:** 按已确认规则实现可重复使用、逐物品独立冷却的临时 Create 动力源。

**Architecture:** 使用原生 PlacementHelpers 指示接口、方向性 GeneratingKineticBlockEntity 接入正常动力网络。物品 UUID 对应 SavedData 内的部署记录，方块保存本次部署标识；绝对游戏时间负责超时和冷却，区块卸载不会延长寿命。

**Tech Stack:** Minecraft 1.21.1 / NeoForge 21.1.219 / Create 6.0.10 / Java 21。

**Spec:** 本次用户要求及确认：2048SU、64RPM、运行120秒、消失后冷却120秒；每个物品独立，右键空气收回手持物品的装置；暂不添加配方。

## Global Constraints

- 仅维护 1.21.1-NeoForge；沿用现有注册、Shift 概要、防火和防爆规则。
- 物品不消耗、不堆叠；临时方块不掉落第二件物品。
- 状态随存档保存；未加载装置不强制加载区块，重载时过期装置在发电前移除。
- 不启动客户端；贴图交给用户指定的现有贴图对话。
- 完整更新递增至0.3.29，更新README和技术文档，验证后自动commit、push。

## Review Focus

- 两件物品同时部署，回收其中一件不会影响另一件。
- 超时、提前回收或方块破坏都只开始一次冷却。
- 不合适的接口、被占用位置和冒险模式不能产生装置或消耗物品。
- 卸载/重载和存档往返保留生命周期；旧部署不影响新部署。
- 真实动力网络得到2048SU，过载按Create规则停机。

## Task 1: 部署、动力与生命周期

**Files:** 新增 content/kinetics/portable 下的 Data、Item、Block、BlockEntity；修改注册器和启动事件。

**Interfaces:** `PortableStressData` 按物品UUID记录部署和冷却；`PortableStressDeviceItem` 统一放置/回收并同步物品快照；方块只允许其部署记录有效时发电。

- [x] 先新增服务器测试，验证缺少新物品时失败。
- [x] 实现原生接口预览、保留物品的放置、64RPM/2048SU动力源及独立生命周期。
- [x] 验证独立部署/回收/冷却、实际动力传递、自动超时、保存加载、非法放置及无重复掉落。

## Task 2: 展示与发布

**Files:** 客户端半轴渲染、方块/物品JSON、贴图、语言文件、README、docs/neoforge-1.21.1.md、gradle.properties。

- [x] 接入指定对话制作的三张贴图；添加Create Shift概要和逐物品状态条。
- [x] 将暂缓配方的新物品排除于“所有物品已有配方”的测试，并断言当前没有配方。
- [x] 执行构建、服务器测试、JSON/语言/资源打包检查和代码审阅。
- [x] 同步0.3.29版本及文档，提交并推送本次改动。

## 完成记录

- 版本0.3.29；完整构建与205项既有回归通过。
- 有JEI176项、无JEI172项服务器测试通过；新增14项。
- 区块不强制加载；卸载立即原生断电，SavedData绝对tick继续计时。
- 放置事务、保护事件与完整网络重载问题已通过失败复现后修复，最终只读审查无待修问题。
- 304项资源、241个类、215个JSON、84张PNG、两种语言各225键的发布检查通过。
- 未启动客户端；发布按AGENTS.md自动提交及推送，提交号以本计划所在Git提交为准。
