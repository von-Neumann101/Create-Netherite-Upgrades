# Create Netherite - NeoForge 1.21.1 技术交接文档

本文档描述 `1.21.1-NeoForge` 分支上的 NeoForge 1.21.1 版本实现，面向后续接手开发的 AI 或开发者。不要把本文档中的 NeoForge 1.21.1 API 直接照搬到 Forge 1.20.1 分支。

## 维护方向

自 2026-10-29 起，`1.21.1-NeoForge` 为主要维护分支，后续功能、问题修复和依赖更新以此为主。Minecraft 1.20.1 Forge 的 `main` 分支暂停更新，保留现有实现和[历史交接文档](forge-1.20.1.md)，暂停期间不再同步新增内容。

## 版本与分支

- 分支：`1.21.1-NeoForge`
- 源码核对日期：`2026-10-29`
- 源码基线提交：`81c14f2eceb6aea7ea1289e313da94ddf339555a`（后续工作区修改包含本次锅炉等级调整）
- 模组版本：`0.2.4`
- Mod id：`createnetherite`
- 主包名：`io.github.boosterproject.booster`
- Minecraft：`1.21.1`
- NeoForge：`21.1.219`
- Create：`6.0.10-280`
- Java：`21`
- 构建插件：`net.neoforged.moddev` `2.0.142`
- Gradle Wrapper：`9.2.1`

以上为当前构建配置。模组元数据声明 NeoForge 范围为 `[21.1.219,)`，Create 范围为 `[6.0.10,6.1.0)`；声明范围不等于已逐版完成运行验证。

## 构建与运行

核心文件：

- `build.gradle`
- `settings.gradle`
- `gradle.properties`
- `src/main/templates/META-INF/neoforge.mods.toml`
- `src/main/resources/createnetherite.mixins.json`

常用命令：

```text
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

`runServer` 使用 `run/server` 作为工作目录，并传入 `--nogui`；客户端使用 `run` 目录。

### 验证记录与范围

- 2026-10-29 接手核对覆盖 `src/main/java` 的 39 个 Java 文件、注册、配置、Mixin、配方和资源路径；74 个资源 JSON 的语法解析通过。
- 2026-10-29 锅炉等级调整已通过 `.\gradlew.bat --offline --no-configuration-cache build`，其中 `check` 阶段执行了 15 项锅炉逻辑回归检查，覆盖超过 18 级的尺寸/供水、引擎效率、尺寸/供水/热量不足、无水、被动加热、18 级提示及原版锅炉限制。
- 本次未重新执行 `runClient` 或 `runServer`，未做游戏内行为验证。构建仍报告现有 API 和 Gradle 弃用提示；本次离线构建关闭配置缓存，以避开缓存序列化时对未缓存渲染依赖的解析。
- 旧交接记录中的构建成功、客户端资源重载成功，对应 Create Netherite `0.1.0`、NeoForge `21.1.242`。当时未发现 Mixin 应用失败、模组加载失败或缺失模型错误；该记录仅是历史验证，不能代替当前 `0.2.4` 的构建和运行验证。

按当前配置生成的主 jar：

```text
build/libs/createnetherite-1.21.1-0.2.4.jar
```

## Gradle 与依赖

`build.gradle` 使用 NeoForge ModDevGradle：

- `java-library`
- `maven-publish`
- `net.neoforged.moddev` `2.0.142`
- `idea`

Java 设置：

```text
java.toolchain.languageVersion = 21
options.release = 21
```

核心依赖：

- `implementation("com.simibubi.create:create-${minecraft_version}:${create_version}:slim") { transitive = false }`
- `implementation("net.createmod.ponder:ponder-neoforge:${ponder_version}+mc${minecraft_version}")`
- `compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-${minecraft_version}:${flywheel_version}")`
- `runtimeOnly("dev.engine-room.flywheel:flywheel-neoforge-${minecraft_version}:${flywheel_version}")`
- `implementation("com.tterrag.registrate:Registrate:${registrate_version}")`
- `compileOnly("io.github.llamalad7:mixinextras-common:${mixin_extras_version}")`
- `annotationProcessor "org.spongepowered:mixin:0.8.5:processor"`

当前配套依赖版本：Ponder `1.0.82`、Flywheel `1.0.6`、Registrate `MC1.21-1.3.0+67`、MixinExtras `0.4.1`。Minecraft 映射使用 Parchment `2024.11.17`（Minecraft `1.21.1`）。版本以 `gradle.properties` 为准。

`neoforge.mods.toml` 不是直接放在 resources 下，而是模板：

```text
src/main/templates/META-INF/neoforge.mods.toml
```

`generateModMetadata` 任务会 expand `gradle.properties` 中的版本和 mod 元数据，并输出到：

```text
build/generated/sources/modMetadata
```

jar manifest 写入：

```text
MixinConfigs: createnetherite.mixins.json
```

## 模组内容

注册 ID：

- `createnetherite:powerful_mechanical_pump`
- `createnetherite:netherite_fluid_tank`
- `createnetherite:netherite_steam_engine`
- `createnetherite:netherite_item_vault`
- `createnetherite:netherite_elevator_pulley`
- `createnetherite:netherite_mechanical_press`
- `createnetherite:netherite_blaze_burner`
- `createnetherite:netherite_sheet`

创造模式标签页：

- 注册位置：`BoosterCreativeModeTabs`
- 标签页 ID：`createnetherite:createnetherite`
- 图标：强力机械泵
- 包含：上述七种方块的物品和下界合金板

## 主类与初始化

主类：`src/main/java/io/github/boosterproject/booster/Booster.java`

NeoForge 1.21.1 使用构造器注入：

```java
public Booster(IEventBus modEventBus, ModContainer modContainer)
```

职责：

- 固定 `MOD_ID = "createnetherite"`
- 通过构造参数获取 `IEventBus` 和 `ModContainer`
- 注册：
  - `BoosterConfigs`
  - `BoosterBlocks`
  - `BoosterItems`
  - `BoosterCreativeModeTabs`
  - `BoosterBlockEntityTypes`
- 客户端侧通过 `FMLEnvironment.dist == Dist.CLIENT` 调用 `BoosterClient.register(...)`
- 在 `FMLCommonSetupEvent` 中用 `BlockStressValues.IMPACTS.register(...)` 注册强力机械泵基础应力冲击
- 在 `FMLCommonSetupEvent` 中注册下界合金蒸汽引擎的 2048 SU 基础容量和最高 64 RPM
- 升降机滑轮、动力冲压机的基础应力 impact 通过 supplier 读取 Create 对应原版方块的值
- 将下界合金保险库关联到 `AllMountedStorageTypes.VAULT`，复用 Create 的运动结构物品存储类型
- 为下界合金燃烧室注册 `BoilerHeater`（原版热量乘 3）和 `BlazeBurnerMovementBehaviour`

不要使用 1.20.1 的：

- `FMLJavaModLoadingContext.get()`
- `ModLoadingContext.get()`
- Forge 包名 `net.minecraftforge.*`

## 注册系统

NeoForge 1.21.1 使用 NeoForge DeferredRegister 新类型，不再使用 Forge `RegistryObject`。

### 方块

文件：`BoosterBlocks`

- `DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Booster.MOD_ID)`
- `DeferredBlock<PowerfulMechanicalPumpBlock> POWERFUL_MECHANICAL_PUMP`
- `DeferredBlock<NetheriteFluidTankBlock> NETHERITE_FLUID_TANK`
- `DeferredBlock<NetheriteSteamEngineBlock> NETHERITE_STEAM_ENGINE`
- `DeferredBlock<NetheriteItemVaultBlock> NETHERITE_ITEM_VAULT`
- `DeferredBlock<NetheriteElevatorPulleyBlock> NETHERITE_ELEVATOR_PULLEY`
- `DeferredBlock<NetheriteMechanicalPressBlock> NETHERITE_MECHANICAL_PRESS`
- `DeferredBlock<NetheriteBlazeBurnerBlock> NETHERITE_BLAZE_BURNER`

属性大多复制 Create 对应方块；保险库额外设置爆炸抗性 `1200.0F`。强力泵基于铜块属性，储罐和蒸汽引擎基于下界合金块属性。

1.21.1 API 差异：

- 使用 `BlockBehaviour.Properties.ofFullCopy(...)`
- 不使用 1.20.1 的 `BlockBehaviour.Properties.copy(...)`

### 物品

文件：`BoosterItems`

- `DeferredRegister.Items ITEMS = DeferredRegister.createItems(Booster.MOD_ID)`
- `DeferredItem<Item> POWERFUL_MECHANICAL_PUMP`
- `DeferredItem<Item> NETHERITE_SHEET`
- `DeferredItem<Item> NETHERITE_FLUID_TANK`
- `DeferredItem<Item> NETHERITE_STEAM_ENGINE`
- `DeferredItem<Item> NETHERITE_ITEM_VAULT`：复用 Create `ItemVaultItem`，支持一键铺层
- `DeferredItem<Item> NETHERITE_ELEVATOR_PULLEY`：普通 `BlockItem`
- `DeferredItem<Item> NETHERITE_MECHANICAL_PRESS`：普通 `BlockItem`
- `DeferredItem<Item> NETHERITE_BLAZE_BURNER`：使用 `BlazeBurnerBlockItem.withBlaze(...)`

### BlockEntityType

文件：`BoosterBlockEntityTypes`

- `DeferredRegister<BlockEntityType<?>>` 使用 `Registries.BLOCK_ENTITY_TYPE`
- 使用 `DeferredHolder<BlockEntityType<?>, BlockEntityType<T>>`
- `POWERFUL_MECHANICAL_PUMP` 绑定 `PowerfulMechanicalPumpBlockEntity`
- `NETHERITE_FLUID_TANK` 绑定 `NetheriteFluidTankBlockEntity`
- `NETHERITE_STEAM_ENGINE` 绑定 `NetheriteSteamEngineBlockEntity`
- `NETHERITE_ITEM_VAULT` 绑定 `NetheriteItemVaultBlockEntity`
- `NETHERITE_ELEVATOR_PULLEY` 绑定 `NetheriteElevatorPulleyBlockEntity`
- `NETHERITE_MECHANICAL_PRESS` 绑定 `NetheriteMechanicalPressBlockEntity`
- `NETHERITE_BLAZE_BURNER` 直接使用 Create `BlazeBurnerBlockEntity`，构造时传入自己的 BE type

`BoosterBlockEntityTypes.register(...)` 同时监听 `RegisterCapabilitiesEvent`：

- 为储罐注册 `Capabilities.FluidHandler.BLOCK`，返回 `getFluidCapability()`
- 为保险库注册 `Capabilities.ItemHandler.BLOCK`，返回 `getItemCapability()`

这些能力注册用于外部管道和物流访问自定义库存。

## 配置

文件：`BoosterConfigs`

NeoForge 1.21.1 API：

- `net.neoforged.neoforge.common.ModConfigSpec`
- `net.neoforged.fml.ModContainer`
- `container.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "createnetherite-server.toml")`

配置文件名：

```text
createnetherite-server.toml
```

配置项：

| 名称 | 默认值 | 范围 | 用途 |
| --- | --- | --- | --- |
| `powerfulPumpStressImpact` | `16.0` | `Double.MIN_VALUE..Double.MAX_VALUE` | 强力泵基础应力 impact |
| `powerfulPumpPressureMultiplier` | `8.0` | `Double.MIN_VALUE..Double.MAX_VALUE` | 强力泵 pressure 倍率 |
| `netheriteFluidTankCapacityMultiplier` | `16` | `1..1024` | 储罐相对 Create 单方块容量的倍率 |
| `netheriteElevatorPulleyMaxDistance` | `768` | `1..4096` | 升降机最大运输距离，单位为格 |

已有世界会保留自己的 `serverconfig/createnetherite-server.toml`。修改默认值不会覆盖已有世界配置；修改世界配置后重启世界或服务器，使已有动能、管网、储罐和升降机按新值重新计算。

保险库的 16 倍槽位、升降机的 2 倍转速系数和燃烧室的 3 倍热量目前是代码常量，不是配置项。

## 强力机械泵实现

### Block

文件：`content/fluids/pump/PowerfulMechanicalPumpBlock.java`

- 继承：`com.simibubi.create.content.fluids.pump.PumpBlock`
- 只覆盖 `getBlockEntityType()`，返回 Booster 自己的 BE type
- 不复制 Create 原版 `PumpBlock`
- 保留原版朝向、轴向、动能连接、放置/破坏行为

### BlockEntity

文件：`PowerfulMechanicalPumpBlockEntity.java`

- 继承：`com.simibubi.create.content.fluids.pump.PumpBlockEntity`
- 构造器传入 Booster 自己的 `BlockEntityType`
- 不覆盖原版压力传播、tick、流向、速度逻辑
- 高 pressure 由 Mixin 在最小写入点修改

### Item 与 tooltip

文件：`PowerfulMechanicalPumpBlockItem.java`

NeoForge 1.21.1 tooltip 签名：

```java
appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag)
```

不要使用 1.20.1 的 `Level level` 签名。

## 强力机械泵应力实现

注册位置：`Booster.commonSetup(...)`

```java
BlockStressValues.IMPACTS.register(
    BoosterBlocks.POWERFUL_MECHANICAL_PUMP.get(),
    () -> BoosterConfigs.SERVER.powerfulPumpStressImpact.get()
)
```

要点：

- 默认基础 impact：`16.0`
- 原版机械泵按 4.0 设计时，同 RPM 下强力泵约 4 倍应力
- 不手动乘 speed/RPM
- Create 动能网络负责最终应力计算
- 原版 `create:mechanical_pump` 不受影响

## 强力机械泵压力/吞吐实现

核心类：`content/fluids/pump/BoosterPumpPressure.java`

行为：

- 只对 `PowerfulMechanicalPumpBlockEntity` 生效
- 普通 `PumpBlockEntity` 直接返回原始 pressure
- 服务端为权威端；客户端非 virtual BE 不单独计算 pressure
- multiplier 每次从 `BoosterConfigs.SERVER.powerfulPumpPressureMultiplier.get()` 读取
- 默认 multiplier：`8.0`
- pressure 上限：`4096.0F`

计算路径：原始 pressure 为正的有限值时，乘以配置倍率后取不超过 `4096.0F` 的值。原始值非有限或非正时返回 `0`；倍率非有限或非正时回退到 `1.0`；乘法溢出时按压力上限处理。

安全理由：

- 防止 NaN/Infinity 写入 Create 流体网络
- 防止极端配置导致每 tick drain/fill 过大
- 代码按 Create `pressure / 2` 的传输速度关系选择压力上限；实际吞吐仍取决于管网和端点

调试开关：开发环境下设置 JVM 属性 `-Dcreatenetherite.debugPumpPressure=true`，服务端会记录泵类型、写入位置、转速、倍率及原始/有效压力。生产环境不启用此日志。

### Mixin 1：远端管网 pressure

文件：`mixin/PumpBlockEntityMixin.java`

- Target：`com.simibubi.create.content.fluids.pump.PumpBlockEntity`
- Method：`distributePressureTo`
- 注入方式：MixinExtras `@ModifyExpressionValue`
- 注入点：`Math.abs(float)`
- 作用：把原版 `abs(getSpeed())` 改为强力泵有效 pressure

调用链概念：

```text
PumpBlockEntity.distributePressureTo(Direction)
-> Math.abs(getSpeed())
-> BoosterPumpPressure.scalePressure(..., REMOTE_NETWORK)
-> Create 原版 FluidTransportBehaviour.addPressure(...)
-> Create 原版 FluidNetwork 计算实际 transfer speed
```

### Mixin 2：泵自身接口 pressure

文件：`mixin/PumpFluidTransferBehaviourMixin.java`

- Target：`com.simibubi.create.content.fluids.pump.PumpBlockEntity$PumpFluidTransferBehaviour`
- Method：`tick`
- 注入方式：MixinExtras `@ModifyExpressionValue`
- 注入点：`Math.abs(float)`
- 作用：修改泵自身 `PipeConnection` 写入 pressure
- 不改变原版 `pressure.set(!pull, 0f)`，所以不改变原版方向语义

调用链概念：

```text
PumpBlockEntity.PumpFluidTransferBehaviour.tick()
-> Math.abs(getSpeed())
-> BoosterPumpPressure.scalePressure(..., PUMP_INTERFACE)
-> PipeConnection pressure 写入
-> Create 原版 FluidNetwork 使用该 pressure
```

### 与 Forge 管网补丁的区别

当前 NeoForge 分支没有 `FluidPropagatorMixin.java`，Mixin 配置中也未注册该项。旧文档中描述的 `FluidPropagator.propagateChangedPipe` 重定向属于 Forge 分支，不能作为当前 NeoForge 实现的一部分。当前泵压力通过上述两个 Mixin 修改；管网变化后的行为需要在对应 Create 版本下验证。

## 下界合金流体储罐实现

### Block

文件：`content/fluids/tank/NetheriteFluidTankBlock.java`

- 继承：`com.simibubi.create.content.fluids.tank.FluidTankBlock`
- 构造：`super(properties, false)`
- 覆盖：
  - `getBlockEntityClass()` 返回 `NetheriteFluidTankBlockEntity.class`
  - `getBlockEntityType()` 返回 Booster 自己的储罐 BE type
- 保留原版储罐窗口、扳手、流体交互、比较器、多方块行为

### BlockEntity

文件：`NetheriteFluidTankBlockEntity.java`

核心原因：Create 的 `FluidTankBlockEntity.getCapacityMultiplier()` 是 static，不能通过子类 static 同名方法覆盖父类内部调用。因此必须覆盖实例容量路径。

覆盖方法：

- `createInventory()`
  - 创建 `SmartFluidTank(getNetheriteCapacityMultiplier(), this::onFluidStackChanged)`
- `applyFluidTankSize(int blocks)`
  - 设置容量为 `blocks * getNetheriteCapacityMultiplier()`
  - 裁剪溢出流体
  - 设置 `forceFluidLevelUpdate = true`
- `read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket)`
  - 1.21.1 多了 `HolderLookup.Provider`
  - 调用 `super.read(compound, registries, clientPacket)` 后修正 controller 容量
  - 用 `tankInventory.readFromNBT(registries, tankContent)` 读回流体
- `getTankSize(int tank)`
  - 返回单方块下界合金储罐容量

容量公式：

```text
singleBlockCapacity = FluidTankBlockEntity.getCapacityMultiplier() * netheriteFluidTankCapacityMultiplier
multiBlockCapacity = blockCount * singleBlockCapacity
默认：netheriteFluidTankCapacityMultiplier = 16
```

`safeCapacity(...)` 使用 `long` 中间值，并将结果限制在 `1..Integer.MAX_VALUE`。读取 controller NBT 时先保留 `TankContent`，在父类读取后修正容量、重新读回流体并裁剪溢出；客户端数据包或 `ForceFluidLevel` 标志会触发液面插值重置。

### BlockItem 与一键铺层

文件：`NetheriteFluidTankBlockItem.java`

职责：

- tooltip 显示容量倍率
- 覆盖 `place(BlockPlaceContext)`，成功放置后尝试补齐一整层
- 使用 `ConnectivityHandler.partAt(...)` 找到同类储罐 controller
- 只在上下方向、非潜行、热栏无对称法杖时触发
- 不足数量或目标层不可替换则放弃
- 放置时临时写入玩家 persistent data：`SilenceTankSound`

1.21.1 NBT/API 差异：

- 不使用 `ItemStack.getTagElement("BlockEntityTag")`
- 使用 `DataComponents.BLOCK_ENTITY_DATA`
- 使用 `CustomData.copyTag()` 读取方块实体数据
- 使用 `stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(nbt))` 写回
- `FluidStack` 使用：
  - `FluidStack.parseOptional(server.registryAccess(), tag)`
  - `fluid.saveOptional(server.registryAccess())`

清理字段：

- `Luminosity`
- `Size`
- `Height`
- `Controller`
- `LastKnownPos`

如果物品保存了 `TankContent`，会把流体量裁剪到单方块容量。

## 下界合金锅炉与蒸汽引擎实现

- `NetheriteFluidTankBlockEntity` 使用 `NetheriteBoilerData`，移除尺寸等级和供水等级的 18 级上限。
- `getMaxHeatLevelForBoilerSize(int boilerSize)` 返回 `boilerSize / 4`（整数除法），尺寸等级随储罐方块总数增加。
- `getMaxHeatLevelForWaterSupply()` 返回 `ceil(waterSupply) / 12`（整数除法），供水等级随平均注水速度增加；保留原有向上取整规则和每级 12 mB/t 的需求，Create 原版为每级 10 mB/t。
- 实际锅炉等级仍由 Create 取尺寸、供水和热量等级的最低值，继续用于引擎效率、护目镜显示和读档后的仪表计算。例如 80 个储罐方块、240 mB/t 供水、20 级热量可达到 20 级；尺寸、供水或热量不足仍会降低等级。
- 下界合金蒸汽引擎基础应力容量为 2048 SU，是 Create 原版蒸汽引擎的两倍。
- `NetheriteSteamEngineBlock` 和对应方块实体只允许连接下界合金流体储罐。
- `BoilerDataMixin` 只让下界合金储罐识别下界合金蒸汽引擎，并修正护目镜中的应力与耗水显示。
- `SteamEngineBlockEntityMixin` 让 Create 的活塞运动与目标角度逻辑识别自定义蒸汽引擎。
- 普通 Create 储罐仍只识别普通蒸汽引擎，两种锅炉不能混搭。

应力容量 `2048` 是注册给 Create 的基础值，最终网络供能由 Create 按实际转速和锅炉状态计算，不是整个锅炉固定输出 `2048 SU`。

护目镜显示修正位于 `BoilerDataMixin.addToGoggleTooltip`：针对 `NetheriteBoilerData` 替换容量查询的引擎方块，并把每级耗水常量从 `10.0` 改为 `12`。

`NetheriteBoilerData.getHeatLevelTextComponent()` 将父类在 18 级返回的 `create.boiler.max_lvl` 提示替换为数值等级 `create.boiler.lvl`，其余等级、被动加热和闲置提示继续使用父类结果。Create 普通锅炉保留原版 18 级限制和提示。

### 锅炉回归检查

检查入口：`src/boilerTest/java/io/github/boosterproject/booster/content/fluids/tank/BoilerLevelTest.java`。

```text
.\gradlew.bat boilerRegressionTest
```

该任务运行实际的 Create `BoilerData` 和当前 `NetheriteBoilerData`，无需启动游戏或额外测试库，并已接入 `check` / `build`。它验证计算和等级提示；不代替世界中的热源扫描、管道注水和多方块行为验证。

## 下界合金物品保险库实现

文件：`content/logistics/vault/NetheriteItemVaultBlock.java`、`NetheriteItemVaultBlockEntity.java`

- 方块和 BE 分别继承 Create `ItemVaultBlock`、`ItemVaultBlockEntity`，返回自己的 BE class/type。
- BE 构造后以 `ItemStackHandler` 替换父类库存，槽位数为父类槽位数乘 `CAPACITY_MULTIPLIER = 16`。
- 库存变化时更新比较器，并调用 `level.blockEntityChanged(worldPosition)` 标记内容变化。
- `getItemCapability()` 先调用 `getInvId()`，再取得多方块库存能力。
- `Booster.commonSetup` 注册 `AllMountedStorageTypes.VAULT`，复用运动结构上的保险库存储。

三个 Mixin 补足 Create 对自定义保险库的识别：

- `ItemVaultBlockMixin`：扩展 `isVault` 的方块判断。
- `ItemVaultBlockEntityMixin`：在 `initCapability` 中为下界合金保险库使用自己的 BE type。
- `ItemVaultItemMixin`：在 `tryMultiPlace` 中为下界合金保险库物品使用自己的 BE type。

## 下界合金升降机滑轮实现

文件：`content/contraptions/elevator/NetheriteElevatorPulleyBlock.java`、`NetheriteElevatorPulleyBlockEntity.java`

- 继承 Create 升降机方块和 BE，基础应力 impact 沿用原版。
- `ElevatorPulleyBlockEntityMixin` 在 `getMovementSpeed` 读取 `getSpeed()` 的位置，将下界合金滑轮的转速乘 `SPEED_MULTIPLIER = 2`；最终移动速度继续由 Create 计算。
- BE 覆盖 `getExtensionRange()`；Mixin 同时修正 `assemble` 中的装配距离，使用相同上限：

```text
max(0, min(configuredMaxDistance, pulleyY - 1 - worldMinBuildHeight))
```

默认最大距离 `768`，配置允许 `1..4096`。运输范围受世界最低建造高度限制。

## 下界合金动力冲压机实现

文件：`content/kinetics/press/NetheriteMechanicalPressBlock.java`、`NetheriteMechanicalPressBlockEntity.java`

- 继承 Create 动力冲压机方块和 BE，基础应力 impact 沿用原版。
- `canProcessInBulk()` 固定返回 `true`，启用 Create 的批量处理路径。
- `tryProcessInBasin(boolean simulate)` 先调用父类，再在 `matchBasinRecipe(currentRecipe)` 成立时循环调用 `applyBasinRecipe()`，连续处理当前可匹配的工作盆配方。
- 保留原版配方类型和冲压运动逻辑；代码没有单独增加冲压速度倍率。
- 当前 Forge `main` 的功能清单不包含该冲压机，暂停维护期间不向其同步。

## 下界合金烈焰人燃烧室实现

文件：`content/processing/burner/NetheriteBlazeBurnerBlock.java`

- 继承 Create `BlazeBurnerBlock`，BE 复用 `BlazeBurnerBlockEntity` 并绑定自己的 BE type。
- `HEAT_MULTIPLIER = 3`；`Booster.commonSetup` 向 `BoilerHeater.REGISTRY` 注册原版燃烧室热量乘 3 的供应逻辑。
- 燃料消耗和热状态由 Create 原版 BE 处理；3 倍修正在锅炉热源查询处，不是新的热状态或燃料系统。
- 物品使用 `BlazeBurnerBlockItem.withBlaze(...)`，中键拾取返回自己的燃烧室物品。
- 注册 `BlazeBurnerMovementBehaviour`，客户端复用原版 renderer 和 Flywheel visual。

## 客户端渲染与模型

入口：`client/BoosterClient.java`

- 注册 BE renderer：
  - 强力泵：`PowerfulMechanicalPumpRenderer`
  - 下界合金储罐：`NetheriteFluidTankRenderer`
  - 下界合金蒸汽引擎：复用 Create `SteamEngineRenderer`
  - 下界合金升降机滑轮：复用 Create `ElevatorPulleyRenderer`
  - 下界合金动力冲压机：复用 Create `MechanicalPressRenderer`
  - 下界合金烈焰人燃烧室：复用 Create `BlazeBurnerRenderer`
- 使用 `CreateClient.MODEL_SWAPPER.getCustomBlockModels()`：
  - 强力泵：`PipeAttachmentModel::withAO`
  - 储罐：`NetheriteFluidTankModel::standard`
  - 保险库：`CTModel` + `NetheriteItemVaultCTBehaviour`
- 储罐渲染层：`ItemBlockRenderTypes.setRenderLayer(..., RenderType.cutoutMipped())`
- Flywheel visual：强力泵使用 `PowerfulMechanicalPumpVisual`；蒸汽引擎、升降机、冲压机和燃烧室分别复用 `SteamEngineVisual`、`ElevatorPulleyVisual`、`PressVisual`、`BlazeBurnerVisual`

1.21.1 API 差异：

- `ResourceLocation` 使用 `ResourceLocation.fromNamespaceAndPath(...)`
- 不使用 `new ResourceLocation(namespace, path)`
- renderer 注册事件：`net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers`

强力泵 cog：

- Partial model：`BoosterPartialModels.POWERFUL_MECHANICAL_PUMP_COG`
- 路径：`assets/createnetherite/models/block/powerful_mechanical_pump/cog.json`
- 非 Flywheel visualization 时由 `PowerfulMechanicalPumpRenderer` 渲染动态齿轮
- 注意静态模型不要再包含会与动态 cog 重叠的齿轮

储罐模型：

- `NetheriteFluidTankModel extends CTModel`
- 使用 Create 的 `FluidTankCTBehaviour`
- 自定义 `CTSpriteShiftEntry`：`fluid_tank`、`fluid_tank_top`、`fluid_tank_inner`
- 通过 `ConnectivityHandler.isConnected(...)` 剔除内部面
- 液体显示由 `NetheriteFluidTankRenderer` 包装并委托 Create `FluidTankRenderer`

锅炉仪表：

- `BoosterPartialModels` 注册 `NETHERITE_BOILER_GAUGE` 和 `NETHERITE_BOILER_GAUGE_DIAL`。
- 客户端 `FluidTankRendererMixin` 在 `renderAsBoiler` 的 `CachedBuffers.partial(...)` 调用处，为下界合金储罐换用自定义仪表模型。
- 模型路径为 `models/block/netherite_boiler/gauge.json`、`gauge_dial.json`，共用纹理 `textures/block/netherite_boiler_gauge.png`。

保险库连接纹理由 `NetheriteItemVaultCTBehaviour` 根据轴向、朝向和结构大小选择，使用 `textures/block/item_vault/vault_{top,front,side,bottom}_{small,medium,large}.png`。

## 资源与数据包

Minecraft 1.21.1 数据路径使用 singular registry path：

- `data/createnetherite/recipe/...`
- `data/createnetherite/loot_table/...`
- `data/c/tags/item/...`
- `data/minecraft/tags/block/...`

不要使用 1.20.1 的复数目录：

- `recipes`
- `loot_tables`
- `tags/items`
- `tags/blocks`

主要资源：

- `assets/createnetherite/blockstates/powerful_mechanical_pump.json`
- `assets/createnetherite/blockstates/netherite_fluid_tank.json`
- `assets/createnetherite/blockstates/netherite_steam_engine.json`
- `assets/createnetherite/blockstates/netherite_item_vault.json`
- `assets/createnetherite/blockstates/netherite_elevator_pulley.json`
- `assets/createnetherite/blockstates/netherite_mechanical_press.json`
- `assets/createnetherite/blockstates/netherite_blaze_burner.json`
- `assets/createnetherite/models/block/powerful_mechanical_pump/block.json`
- `assets/createnetherite/models/block/powerful_mechanical_pump/cog.json`
- `assets/createnetherite/textures/block/powerful_mechanical_pump/pump.png`
- `assets/createnetherite/models/block/netherite_fluid_tank/*.json`
- `assets/createnetherite/textures/block/netherite_fluid_tank/*.png`
- `assets/createnetherite/models/block/netherite_item_vault.json`
- `assets/createnetherite/textures/block/item_vault/*.png`
- `assets/createnetherite/models/block/netherite_boiler/*.json`
- `assets/createnetherite/models/block/netherite_{steam_engine,elevator_pulley,mechanical_press,blaze_burner}/block.json`
- `assets/createnetherite/models/item/*.json`
- `assets/createnetherite/textures/item/netherite_sheet.png`
- `assets/createnetherite/lang/en_us.json`
- `assets/createnetherite/lang/zh_cn.json`

配方：

- 强力机械泵：两个下界合金板 + 三个 `create:mechanical_pump`，输出 3 个
- 下界合金流体储罐：两个下界合金板 + 三个 `create:fluid_tank`，输出 3 个
- 下界合金蒸汽引擎：两个下界合金板 + 三个 `create:steam_engine`，输出 3 个
- 下界合金物品保险库：两个下界合金板 + 三个 `create:item_vault`，输出 3 个
- 下界合金升降机滑轮：一个下界合金板 + 一个 `create:elevator_pulley`，无序合成 1 个
- 下界合金动力冲压机：一个下界合金板 + 一个 `create:mechanical_press`，无序合成 1 个
- 下界合金烈焰人燃烧室：一个下界合金板 + 一个 `create:blaze_burner`，无序合成 1 个；另有两个板 + 三个原版燃烧室批量合成 3 个的有序配方
- 下界合金板：Create pressing，输入 `c:ingots/netherite`，输出 `createnetherite:netherite_sheet`

本分支补充了：

```text
data/c/tags/item/ingots/netherite.json
```

内容指向：

```text
minecraft:netherite_ingot
```

下界合金板同时加入 `c:plates`、`c:plates/netherite`。七种方块各有 `loot_table/blocks/` 掉落表，并全部加入 `minecraft:mineable/pickaxe`；除强力机械泵外，其余六种方块加入 `minecraft:needs_diamond_tool`。

## 当前 Mixin 清单

以 `src/main/resources/createnetherite.mixins.json` 为准：8 个通用 Mixin，1 个客户端 Mixin。配置使用 `required: true`、`JAVA_21` 和 `defaultRequire: 1`。

| Mixin | 目标方法 | 作用 |
| --- | --- | --- |
| `PumpBlockEntityMixin` | `PumpBlockEntity.distributePressureTo` | 修改远端管网压力 |
| `PumpFluidTransferBehaviourMixin` | `PumpBlockEntity$PumpFluidTransferBehaviour.tick` | 修改泵接口压力 |
| `BoilerDataMixin` | `BoilerData.evaluate` / `addToGoggleTooltip` | 匹配下界合金引擎，修正容量和耗水显示 |
| `SteamEngineBlockEntityMixin` | `SteamEngineBlockEntity.tick` / `getTargetAngle` | 识别自定义引擎的活塞与目标角度 |
| `ElevatorPulleyBlockEntityMixin` | `ElevatorPulleyBlockEntity.getMovementSpeed` / `assemble` | 调整转速系数和装配距离 |
| `ItemVaultBlockEntityMixin` | `ItemVaultBlockEntity.initCapability` | 选择保险库 BE type |
| `ItemVaultBlockMixin` | `ItemVaultBlock.isVault` | 识别下界合金保险库 |
| `ItemVaultItemMixin` | `ItemVaultItem.tryMultiPlace` | 一键铺层使用对应 BE type |
| `FluidTankRendererMixin`（客户端） | `FluidTankRenderer.renderAsBoiler` | 替换锅炉仪表模型 |

## 与 Forge 1.20.1 分支的关键差异

以下用于辨别版本实现，不表示继续维护或同步 Forge 分支。

- Java 17 -> Java 21
- Forge `net.minecraftforge.*` -> NeoForge `net.neoforged.*`
- `ForgeConfigSpec` -> `ModConfigSpec`
- `RegistryObject` -> `DeferredBlock` / `DeferredItem` / `DeferredHolder`
- `BlockBehaviour.Properties.copy(...)` -> `ofFullCopy(...)`
- `new ResourceLocation(...)` -> `ResourceLocation.fromNamespaceAndPath(...)`
- tooltip `Level level` 参数 -> `Item.TooltipContext context`
- Fluid/NBT API 需要 `HolderLookup.Provider` 或 `registryAccess()`
- `mods.toml` 静态资源 -> `neoforge.mods.toml` 模板生成
- 数据路径由复数目录改为 singular registry path
- Common tags 从 `forge:*` 改为 `c:*`
- 当前 NeoForge 没有 Forge 的 `FluidPropagatorMixin`，并独有下界合金动力冲压机

## 已知限制与注意点

- 强力机械泵不改变 Create 原版流向语义；正反转仍按原版泵处理。
- 压力倍率提升吞吐，不提升管道可传播距离。Create 管网传播距离不是由 pressure 决定的。
- 不修改 Create 原版源码。
- 不把类放进 `com.simibubi.create` 包。
- 后续升级 Create 时，重新核对上表全部 Mixin 的方法、调用 descriptor、常量和局部变量位置；升降机装配补丁使用 `@ModifyVariable`，尤其依赖目标局部变量布局。
- 保持 `createnetherite` 命名空间及现有方块、物品和 BE 注册 ID，避免已有存档失去映射。
- 文档中的当前行为来自源码核对；构建、客户端启动、服务端启动和具体机械行为的验证结果应分别记录。
