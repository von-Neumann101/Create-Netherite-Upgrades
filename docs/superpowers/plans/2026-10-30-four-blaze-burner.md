# 四烈焰人燃烧室实现计划

> 按 superpowers:executing-plans 在当前维护分支逐项实现；不增加并行代理。

**目标 / Spec：** 用户已确认：合成保留原来的 1 只，手持燃烧室右键烈焰人继续捕获，最多 4 只；每只提供原版 1 倍热量、消耗 0.5 倍燃料，满载为 4 倍热量 / 2 倍耗料。

**架构：** 自定义物品记录数量；自定义 BE 保存数量和半 tick 燃料进度，沿用 Create 原生燃料识别与正常/超级加热切换，每 tick 只调用一次父类。锅炉按数量乘以正热量，保留被动热与无热状态。拆放、扳手拾取、中键复制和移动结构均保留数量。复用原生烈焰人模型排列 1～4 只，不制作或修改贴图。

**技术栈 / 边界：** Minecraft 1.21.1、NeoForge 21.1.219、Create 6.0.10-280、Java 21。仅维护 NeoForge 分支，不修改贡献者素材，不提交旧崩溃报告；完成检查后自动 commit / push。

## 文件与责任

- `content/processing/burner/NetheriteBlazeBurnerData.java`：BlazeCount 的读取、归一化、物品 CUSTOM_DATA 保存及单份捕获结果。
- `NetheriteBlazeBurnerBlockEntity.java`：数量、半 tick 耗料进度、NBT、护目镜提示；旧存档默认 1 只。
- `NetheriteBlazeBurnerItem.java`：实体捕获、分离堆叠物品、交还物品及说明。
- 现有 `NetheriteBlazeBurnerBlock.java`：放置、掉落、复制保留数量，锅炉正热量乘以数量。
- `client/NetheriteBlazeBurnerRenderer.java`、`NetheriteBlazeBurnerMovementBehaviour.java`：原生模型的数量排列及移动结构渲染。
- 客户端 `NetheriteBlazeBurnerAnimationMixin.java`：让本燃烧室在不启用自身 Flywheel visual 时仍更新原生头部动画；其他燃烧室不变。
- `Booster`、BE/物品注册、`BoosterClient`、Mixin 配置和中英语言：接入新实现。
- `registry/BoosterArmInteractionPointTypes.java`：补齐原版机械手仅识别原生燃烧室的兼容缺口，复用原生喂料 Point。
- `src/boilerTest/.../NetheriteBlazeBurnerTest.java`、`build.gradle`：实际 Minecraft 物品/BE 环境下的数量、保存、堆叠保护、半 tick 进度与热状态回归检查，纳入 build。

## 实现与检查

- [x] 先写数量、单份捕获和耗料回归检查，观察旧行为失败，再实现数据和 BE。
- [x] 接入捕获、热量、放置及掉落；检查服务端才移除实体、满载不消耗实体、未捕获的堆叠物品不改变数量和数据。
- [x] 正常燃料与烈焰蛋糕均按 0.5 / 1 / 1.5 / 2 倍倒计时；模拟插入、燃料容器、创造燃料和余热切换沿用原生行为。
- [x] 数量和半 tick 余量经存档/同步保留；拆除仅保存数量，燃料不转为可复制物品。
- [x] 普通燃烧室无改动；原生 1 / 2 级正热分别乘数量，无燃料不凭空提供主动热量。
- [x] 完成原生模型数量渲染、说明和中英 README / 技术文档。
- [x] 完整构建和现有锅炉/过滤器检查，记录实际运行验证范围；仅提交本次文件并推送。

审查修复记录：单件捕获保留原手持对象，创造模式显式使用真实手部而非 Player API 副本；蓝图 writeSafe 保存数量，物料要求同时匹配物品和烈焰人数量，避免丢失或复制。上述问题均先由新增回归复现，再修复。机械手通过官方自定义交互类型注册接入，没有扩大通用 Mixin 范围。

构建记录：完整 build 通过，29 项锅炉 + 59 项燃烧室 + 15 项过滤器，共 103 项检查通过；81 个资源 JSON 解析通过。最终客户端完成模组加载和资源重载，未见本模组注册/Mixin 应用错误；因未补全原版资源，有原版菜单缺图警告。世界中的实体捕获、自动喂料和渲染尚未手动实测。
