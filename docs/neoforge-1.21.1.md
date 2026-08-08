# Create Netherite - NeoForge 1.21.1 技术交接文档

本文档描述 `1.21.1-NeoForge` 分支上的 NeoForge 1.21.1 版本实现，面向后续接手开发的 AI 或开发者。不要把本文档中的 NeoForge 1.21.1 API 直接照搬到 Forge 1.20.1 分支。

## 版本与分支

- 分支：`1.21.1-NeoForge`
- 参考提交：`25d86dfff518e8457ad2facb910c601870836841`
- Mod id：`createnetherite`
- 主包名：`io.github.boosterproject.booster`
- Minecraft：`1.21.1`
- NeoForge：`21.1.242`
- Create：`6.0.10-280`
- Java：`21`
- 构建插件：`net.neoforged.moddev` `2.0.142`

注意：Git 分支名不能包含空格；用户原本想要的 `1.21.1 NeoForge` 实际落地为 `1.21.1-NeoForge`。

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
```

当前已验证：

- `build` 成功。
- `runClient` 能启动到客户端资源重载阶段。
- 日志确认加载：Minecraft 1.21.1、NeoForge 21.1.242、Create 6.0.10、Flywheel 1.0.6、Ponder 1.0.82、Create Netherite 0.1.0。
- 未发现 Mixin apply failure、mod loading failed 或缺失模型错误。

预期 jar：

```text
build/libs/createnetherite-1.21.1-0.1.0.jar
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
- `createnetherite:netherite_sheet`

创造模式标签页：

- 注册位置：`BoosterCreativeModeTabs`
- 标签页 ID：`createnetherite:createnetherite`
- 图标：强力机械泵
- 包含：强力机械泵、下界合金流体储罐、下界合金蒸汽引擎、下界合金板

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

### BlockEntityType

文件：`BoosterBlockEntityTypes`

- `DeferredRegister<BlockEntityType<?>>` 使用 `Registries.BLOCK_ENTITY_TYPE`
- 使用 `DeferredHolder<BlockEntityType<?>, BlockEntityType<T>>`
- `POWERFUL_MECHANICAL_PUMP` 绑定 `PowerfulMechanicalPumpBlockEntity`
- `NETHERITE_FLUID_TANK` 绑定 `NetheriteFluidTankBlockEntity`
- `NETHERITE_STEAM_ENGINE` 绑定 `NetheriteSteamEngineBlockEntity`

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

主要项：

- `powerfulPumpStressImpact`
  - 默认：`16.0`
  - 用途：Create 基础应力冲击
- `powerfulPumpPressureMultiplier`
  - 默认：`8.0`
  - 用途：强力机械泵 pressure 倍率
- `netheriteFluidTankCapacityMultiplier`
  - 默认：`16`
  - 范围：`1..1024`
  - 用途：下界合金流体储罐相对原版单方块容量倍率

注意：已有世界会保留自己的 `serverconfig/createnetherite-server.toml`。

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

安全理由：

- 防止 NaN/Infinity 写入 Create 流体网络
- 防止极端配置导致每 tick drain/fill 过大
- 4096 pressure 对应约 2048 mB/t，已经远高于常规 256 RPM 下 8 倍 pressure

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

### Mixin 3：管网重新发现泵

文件：`mixin/FluidPropagatorMixin.java`

- Target：`com.simibubi.create.content.fluids.FluidPropagator`
- Method：`propagateChangedPipe`
- 注入方式：`@Redirect`
- 目标：`BlockEntry.has(BlockState)`
- 替换逻辑：`entry.has(targetState) || PumpBlock.isPump(targetState)`
- 目的：让继承 `PumpBlock` 的强力机械泵在管道拆装、端点变化、区块重载后能被 Create 管网重新发现

注意：注释仍写着 Create 6.0.8，但该分支已在 Create 6.0.10 NeoForge 环境下成功启动。后续升级 Create 时仍必须重新确认目标方法和 descriptor。

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

- `NetheriteFluidTankBlockEntity` 使用 `NetheriteBoilerData`，保留 Create 原版 1–18 级锅炉规则。
- 每级供水需求为 12 mB/t；Create 原版为 10 mB/t。
- 下界合金蒸汽引擎基础应力容量为 2048 SU，是 Create 原版蒸汽引擎的两倍。
- `NetheriteSteamEngineBlock` 和对应方块实体只允许连接下界合金流体储罐。
- `BoilerDataMixin` 只让下界合金储罐识别下界合金蒸汽引擎，并修正护目镜中的应力与耗水显示。
- `SteamEngineBlockEntityMixin` 让 Create 的活塞运动与目标角度逻辑识别自定义蒸汽引擎。
- 普通 Create 储罐仍只识别普通蒸汽引擎，两种锅炉不能混搭。

## 客户端渲染与模型

入口：`client/BoosterClient.java`

- 注册 BE renderer：
  - 强力泵：`PowerfulMechanicalPumpRenderer`
  - 下界合金储罐：`NetheriteFluidTankRenderer`
  - 下界合金蒸汽引擎：复用 Create `SteamEngineRenderer`
- 使用 `CreateClient.MODEL_SWAPPER.getCustomBlockModels()`：
  - 强力泵：`PipeAttachmentModel::withAO`
  - 储罐：`NetheriteFluidTankModel::standard`
- 储罐渲染层：`ItemBlockRenderTypes.setRenderLayer(..., RenderType.cutoutMipped())`
- Flywheel visual：强力泵使用 `PowerfulMechanicalPumpVisual`，下界合金蒸汽引擎复用 `SteamEngineVisual`

1.21.1 API 差异：

- `ResourceLocation` 使用 `ResourceLocation.fromNamespaceAndPath(...)`
- 不使用 `new ResourceLocation(namespace, path)`
- 客户端事件包名改为 NeoForge：`net.neoforged.neoforge.client.event.EntityRenderersEvent`

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
- `assets/createnetherite/models/block/powerful_mechanical_pump/block.json`
- `assets/createnetherite/models/block/powerful_mechanical_pump/cog.json`
- `assets/createnetherite/textures/block/powerful_mechanical_pump/pump.png`
- `assets/createnetherite/models/block/netherite_fluid_tank/*.json`
- `assets/createnetherite/textures/block/netherite_fluid_tank/*.png`
- `assets/createnetherite/textures/item/netherite_sheet.png`
- `assets/createnetherite/lang/en_us.json`
- `assets/createnetherite/lang/zh_cn.json`

配方：

- 强力机械泵：两个下界合金板 + 三个 `create:mechanical_pump`，输出 3 个
- 下界合金流体储罐：两个下界合金板 + 三个 `create:fluid_tank`，输出 3 个
- 下界合金蒸汽引擎：两个下界合金板 + 三个 `create:steam_engine`，输出 3 个
- 下界合金板：Create pressing，输入 `c:ingots/netherite`，输出 `createnetherite:netherite_sheet`

本分支补充了：

```text
data/c/tags/item/ingots/netherite.json
```

内容指向：

```text
minecraft:netherite_ingot
```

## 与 Forge 1.20.1 分支的关键差异

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

## 已知限制与注意点

- 强力机械泵不改变 Create 原版流向语义；正反转仍按原版泵处理。
- 压力倍率提升吞吐，不提升管道可传播距离。Create 管网传播距离不是由 pressure 决定的。
- 不修改 Create 原版源码。
- 不把类放进 `com.simibubi.create` 包。
- 后续升级 Create 6.0.10 以上时，必须重新检查五组 Mixin target：
  - `PumpBlockEntity.distributePressureTo`
  - `PumpBlockEntity$PumpFluidTransferBehaviour.tick`
  - `FluidPropagator.propagateChangedPipe`
  - `BoilerData.evaluate` / `BoilerData.addToGoggleTooltip`
  - `SteamEngineBlockEntity.tick` / `SteamEngineBlockEntity.getTargetAngle`
- `FluidPropagatorMixin` 注释里提到 Create 6.0.8，是从 Forge 版迁移保留下来的说明；功能在 1.21.1 runClient 中没有触发启动错误，但建议后续清理注释以免误导。
