# 下界合金下界合金扳手实施计划

> 执行：本会话持续完成；按独立模块分工并统一构建验证。

**目标：** 添加无合成配方、无耐久损耗的扳手，X 打开八槽轮盘，当前仅允许激活一个模块。
**架构：** 原生物品组件保存真实模块和流体；原生容器同步模块栏，服务器验证能力、菜单和流体操作。八种能力按战斗、气流/弹射、储液/动力/转换耗材分开。
**环境：** Minecraft 1.21.1、NeoForge 21.1.219、Create 6.0.10-280、JDK 21。

## 已确认规格

- 名称为下界合金下界合金扳手，基础攻击伤害为下界合金剑 +2（10），攻速相同（1.6），不可损坏；本次不添加配方。
- X 打开轮盘，存放最多八个指定工作方块，每槽一个；本次仅一个激活位，双激活和升级留待以后。
- 动力泵：攻击 +6、实体攻击距离 +1、击退属性 +2。
- 鼓风机：长按右键气流；按下 Shift 切换吸/吹；实体不能隔墙受影响，按下界合金鼓风机的风速规则。
- 弹射置物台：Shift 预览抛物线，右键发射，最多 64 格；无阻挡落地免摔落伤害，碰撞后恢复正常摔落伤害。
- 烈焰人燃烧室：持有者造成的火焰伤害四倍。
- 动力冲压机：本次实际攻击的总伤害 1.5 倍，定身 20 tick。
- 蒸汽引擎：长按右键给点击的单个工作方块供能，额度等于恰好启动一台下界合金动力泵；不产生动力网络，不耗背罐空气。
- 转换机：右键生物永久给予抗性 I 和盔甲 +3，不叠加；死亡必掉一张原版下界合金升级模板。模板可作为转换机一次性耗材，仍需 250 mB 精华；板材仍可使用 16 次。
- 流体储罐：右键收纳流体源，Shift 打开流体选择轮盘，Shift+右键放出；所有流体合计最多 64000 mB。
- 所有状态随物品保存，服务端负责校验；移除储罐模块保留已有流体。
- 不启动客户端、不使用 RTX 5060。资源由用户指定的贴图对话提供，保留既有贴图。
- 完整更新版本为 0.3.16，更新 README/技术文档，验证后自动 commit/push。

## 文件与公共接口

`content/equipment/wrench/WrenchModule.java` 定义八种枚举 `PUMP,FAN,EJECTOR,BURNER,PRESS,ENGINE,CONVERTER,TANK`，`item()` 和 `fromStack(ItemStack)`。

`WrenchData.java`：`isWrench(ItemStack)`、`modules(ItemStack): List<ItemStack>`、`setModules(ItemStack,List<ItemStack>)`、`activeSlot(ItemStack): int`、`setActiveSlot(ItemStack,int)`、`active(ItemStack): WrenchModule`、`active(Player): WrenchModule`、`has(ItemStack,WrenchModule)`、`fanPull(ItemStack)`、`setFanPull(ItemStack,boolean)`、`fluids(ItemStack): List<FluidStack>`、`setFluids(ItemStack,List<FluidStack>)`、`selectedFluid(ItemStack): int`、`selectFluid(ItemStack,int)`、`totalFluid(ItemStack): int`。流体编码使用 `FluidStack.CODEC` 和当前注册表上下文：`fluids`/`setFluids` 参数另含 `HolderLookup.Provider`。

`NetheriteNetheriteWrenchItem.java`、`WrenchMenu.java`、`WrenchNetworking.java`、`client/WrenchClient.java`、`client/WrenchScreen.java`、`client/WrenchFluidScreen.java` 负责物品、容器、按键和轮盘。

能力接口：`WrenchCombat.register(IEventBus)`；`WrenchAirflow.tick(Player,ItemStack)`；`WrenchLaunch.start(Player)`、`preview(Player): List<Vec3>`、`register(IEventBus)`；`WrenchTank.use(Level,Player,InteractionHand): InteractionResultHolder<ItemStack>`。公共能力调用均先验证主手激活模块。

## 执行与验证

- [x] 先添加注册失败用例，运行确认失败；实现数据、物品、八槽单激活菜单及客户端交互。
- [x] 实现战斗模块，验证攻击数值、主副手区别、实际总伤害、定身恢复、火焰归属、永久标记及一次掉落。
- [x] 实现气流/弹射，验证吸吹方向、墙体阻挡、轨迹范围、真实飞行与碰撞后的摔落行为。
- [x] 实现储液/动力和转换耗材，验证总容量、满容量不吞流体、精确扣液、单机供能及原伸缩机械手回归、模板一次和板材十六次。
- [x] 统一运行 CPU 构建、205项既有回归及69项无界面 GameTests；新增菜单测试覆盖数字键互换、卸下源物品和状态保存。
- [x] 检查两张新贴图、语言键、正式 jar 元数据与 QA 排除，更新版本与文档。
- [x] 独立复核完整改动，修复重生实例继承问题并通过69项世界测试；发布包逐字节检查通过，随后按项目约定提交推送。

## 复核重点

- 八槽容器不得借助数字键移动打开它的扳手或复制模块。
- 各能力只在主手激活相应模块时使用，服务端不信任客户端传入状态。
- 弹射后的摔落保护不能变成持久免摔权限。
- 满储液、受保护方块和不可放置流体不得先修改世界再失败。
- 升级模板只能转化一次；旧板材剩余次数和旧伸缩机械手耗费保持原规则。
