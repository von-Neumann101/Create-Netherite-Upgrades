# Create Netherite - Forge 1.20.1 技术交接文档

本文档描述 `main` 分支上的 Forge 1.20.1 版本实现，面向后续接手开发的 AI 或开发者。不要把本文档中的 Forge API 直接照搬到 1.21.1 NeoForge 分支。

## 版本与分支

- 分支：`main`
- 参考提交：`29110fd5009cee5a333ae86af494056aea74068b`
- Mod id：`createnetherite`
- 主包名：`io.github.boosterproject.booster`
- Minecraft：`1.20.1`
- Forge：`47.1.33`
- Create：`6.0.8-291`
- Java：`17`
- 构建插件：`net.neoforged.moddev.legacyforge` `2.0.107`

## 构建与运行

核心文件：

- `build.gradle`
- `settings.gradle`
- `gradle.properties`
- `src/main/resources/META-INF/mods.toml`
- `src/main/resources/createnetherite.mixins.json`

常用命令：

```text
.\gradlew.bat build
.\gradlew.bat runClient
```

jar 名称由 `base.archivesName = "createnetherite-${minecraft_version}"` 和 `mod_version` 组合生成，预期输出目录为：

```text
build/libs/
```

`mods.toml` 依赖声明：

- Forge：`[47.1.0,47.2.0)`
- Minecraft：`[1.20.1]`
- Create：`[6.0.8,)`

## 模组内容

注册 ID：

- `createnetherite:powerful_mechanical_pump`
- `createnetherite:netherite_fluid_tank`
- `createnetherite:netherite_sheet`

创造模式标签页：

- 注册位置：`BoosterCreativeModeTabs`
- 标签页 ID：`createnetherite:createnetherite`
- 图标：强力机械泵
- 包含：强力机械泵、下界合金流体储罐、下界合金板

## 主类与初始化

主类：`src/main/java/io/github/boosterproject/booster/Booster.java`

职责：

- 固定 `MOD_ID = "createnetherite"`
- 通过 `FMLJavaModLoadingContext.get().getModEventBus()` 获取 Forge mod event bus
- 注册：
  - `BoosterConfigs`
  - `BoosterBlocks`
  - `BoosterItems`
  - `BoosterCreativeModeTabs`
  - `BoosterBlockEntityTypes`
- 客户端侧通过 `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` 调用 `BoosterClient.register(...)`
- 在 `FMLCommonSetupEvent` 中用 `BlockStressValues.IMPACTS.register(...)` 注册强力机械泵基础应力冲击

重要点：

- 不修改 Create 的 `AllBlocks`、`AllBlockEntityTypes`、`AllConfigs`。
- 应力消耗只注册基础 impact，不手动乘 speed。Create 动能网络会按 RPM 计算最终应力。

## 注册系统

Forge 1.20.1 使用 `DeferredRegister` + `RegistryObject`。

### 方块

文件：`BoosterBlocks`

- `DeferredRegister<Block>` 使用 `ForgeRegistries.BLOCKS`
- `POWERFUL_MECHANICAL_PUMP`
  - 类：`PowerfulMechanicalPumpBlock`
  - 基础属性：`BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK).mapColor(MapColor.STONE)`
- `NETHERITE_FLUID_TANK`
  - 类：`NetheriteFluidTankBlock`
  - 基础属性：`BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).noOcclusion().isRedstoneConductor(...)`

### 物品

文件：`BoosterItems`

- `DeferredRegister<Item>` 使用 `ForgeRegistries.ITEMS`
- `POWERFUL_MECHANICAL_PUMP` 使用 `PowerfulMechanicalPumpBlockItem`
- `NETHERITE_FLUID_TANK` 使用 `NetheriteFluidTankBlockItem`
- `NETHERITE_SHEET` 是普通 `Item`

### BlockEntityType

文件：`BoosterBlockEntityTypes`

- `DeferredRegister<BlockEntityType<?>>` 使用 `ForgeRegistries.BLOCK_ENTITY_TYPES`
- `POWERFUL_MECHANICAL_PUMP` 绑定 `PowerfulMechanicalPumpBlockEntity` 和强力泵方块
- `NETHERITE_FLUID_TANK` 绑定 `NetheriteFluidTankBlockEntity` 和下界合金储罐方块

## 强力机械泵实现

### Block

文件：`content/fluids/pump/PowerfulMechanicalPumpBlock.java`

- 继承：`com.simibubi.create.content.fluids.pump.PumpBlock`
- 只覆盖 `getBlockEntityType()`，返回 Booster 自己的 `BlockEntityType`
- 不复制 Create 原版 `PumpBlock` 逻辑
- 因为继承原版 `PumpBlock`，保留原版朝向、轴向、动能连接、放置/破坏行为

### BlockEntity

文件：`PowerfulMechanicalPumpBlockEntity.java`

- 继承：`com.simibubi.create.content.fluids.pump.PumpBlockEntity`
- 构造器传入 Booster 自己的 `BlockEntityType`
- 不覆盖原版压力传播、tick、流向、速度等逻辑
- 压力倍率由 Mixin 在原版写 pressure 的最小点修改

### Item 与 tooltip

文件：`PowerfulMechanicalPumpBlockItem.java`

- 继承 `BlockItem`
- 覆盖 Forge 1.20.1 签名：

```java
appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag)
```

- tooltip key：`tooltip.createnetherite.powerful_mechanical_pump`

## 强力机械泵应力实现

配置项：`BoosterConfigs.SERVER.powerfulPumpStressImpact`

- 默认：`16.0`
- 范围：`Double.MIN_VALUE` 到 `Double.MAX_VALUE`
- 注册位置：`Booster.commonSetup(...)`
- 注册 API：`BlockStressValues.IMPACTS.register(block, supplier)`

理论对比：

- Create 原版机械泵基础 impact 当前按 4.0 设计
- 强力机械泵默认 16.0
- 在相同 RPM 下，最终应力约为原版 4 倍
- 代码中不要把 speed/RPM 手动乘进去

## 强力机械泵压力/吞吐实现

配置项：`BoosterConfigs.SERVER.powerfulPumpPressureMultiplier`

- 默认：`8.0`
- 运行读取：每次通过 `BoosterConfigs.SERVER.powerfulPumpPressureMultiplier.get()` 获取
- 安全上限：`BoosterPumpPressure.MAX_EFFECTIVE_PRESSURE = 4096.0F`
- 非法值处理：NaN、Infinity、非正数回退或夹紧，避免写入异常 pressure
- 调试开关：非 production 且 JVM 参数 `-Dcreatenetherite.debugPumpPressure=true`

核心类：`content/fluids/pump/BoosterPumpPressure.java`

行为：

- 只对 `PowerfulMechanicalPumpBlockEntity` 生效
- 普通 `PumpBlockEntity` 返回原始 pressure
- 客户端非 virtual BE 不单独计算权威压力，服务端为准
- 最终 pressure：`abs(speed) * configuredMultiplier`，再 clamp 到 4096

### Mixin 1：远端管网 pressure

文件：`mixin/PumpBlockEntityMixin.java`

- Target：`com.simibubi.create.content.fluids.pump.PumpBlockEntity`
- Method：`distributePressureTo`
- 注入方式：MixinExtras `@ModifyExpressionValue`
- 注入点：`Math.abs(float)`
- 作用：修改原版 `abs(getSpeed())` 产生的 pressure，再交给原版网络传播逻辑

调用链概念：

```text
PumpBlockEntity tick / speed change
-> distributePressureTo(Direction)
-> Math.abs(getSpeed())
-> BoosterPumpPressure.scalePressure(..., REMOTE_NETWORK)
-> Create 原版 FluidTransportBehaviour.addPressure(...)
-> Create 原版 FluidNetwork 处理传输速度
```

### Mixin 2：泵自身接口 pressure

文件：`mixin/PumpFluidTransferBehaviourMixin.java`

- Target：`com.simibubi.create.content.fluids.pump.PumpBlockEntity$PumpFluidTransferBehaviour`
- Method：`tick`
- 注入方式：MixinExtras `@ModifyExpressionValue`
- 注入点：`Math.abs(float)`
- 作用：修改写入泵自身 `PipeConnection` 的 pressure
- 不改后续 `pressure.set(!pull, 0f)`，所以不改变 Create 原版方向语义

调用链概念：

```text
PumpBlockEntity.PumpFluidTransferBehaviour.tick()
-> Math.abs(getSpeed())
-> BoosterPumpPressure.scalePressure(..., PUMP_INTERFACE)
-> PipeConnection pressure 写入
-> Create 原版流体网络使用该 pressure
```

### Mixin 3：管网重新发现泵

文件：`mixin/FluidPropagatorMixin.java`

- Target：`com.simibubi.create.content.fluids.FluidPropagator`
- Method：`propagateChangedPipe`
- 注入方式：`@Redirect`
- 原目标：`BlockEntry.has(BlockState)`，即 Create 原版 `AllBlocks.MECHANICAL_PUMP.has(targetState)` 类似判断
- 替换逻辑：`entry.has(targetState) || PumpBlock.isPump(targetState)`
- 目的：让继承 `PumpBlock` 的强力机械泵也能被管网变化传播识别

注意：这是针对 Create 6.0.8 源码确认后的硬编码兼容点。升级 Create 后必须重新确认目标方法和 descriptor。

## 下界合金流体储罐实现

### Block

文件：`content/fluids/tank/NetheriteFluidTankBlock.java`

- 继承：`com.simibubi.create.content.fluids.tank.FluidTankBlock`
- 构造：`super(properties, false)`，即非 creative tank 行为
- 覆盖：
  - `getBlockEntityClass()` 返回 `NetheriteFluidTankBlockEntity.class`
  - `getBlockEntityType()` 返回 Booster 自己的储罐 BE type
- 不复制原版储罐逻辑，因此保留原版窗口、扳手、流体交互、比较器、多方块行为

### BlockEntity

文件：`NetheriteFluidTankBlockEntity.java`

核心原因：Create 6.0.8 的 `FluidTankBlockEntity.getCapacityMultiplier()` 是 static，不能通过子类 static 同名方法覆盖父类内部调用。因此必须覆盖实例方法和库存创建/修正点。

覆盖方法：

- `createInventory()`
  - 创建 `SmartFluidTank(getNetheriteCapacityMultiplier(), this::onFluidStackChanged)`
- `applyFluidTankSize(int blocks)`
  - 设置容量为 `blocks * getNetheriteCapacityMultiplier()`
  - 调用 `drainOverflow()`
  - 设置 `forceFluidLevelUpdate = true`
- `read(CompoundTag compound, boolean clientPacket)`
  - 先备份 `TankContent`
  - 调用 `super.read(...)`
  - 如果是 controller，修正容量为 `getTotalTankSize() * getNetheriteCapacityMultiplier()`
  - 重新读回 tank content，裁剪溢出，刷新客户端液面
- `getTankSize(int tank)`
  - 返回单方块容量 multiplier

容量公式：

```text
singleBlockCapacity = FluidTankBlockEntity.getCapacityMultiplier() * netheriteFluidTankCapacityMultiplier
multiBlockCapacity = blockCount * singleBlockCapacity
默认：netheriteFluidTankCapacityMultiplier = 16
```

### BlockItem 与一键铺层

文件：`NetheriteFluidTankBlockItem.java`

职责：

- tooltip 显示倍率
- 覆盖 `place(BlockPlaceContext)`，在成功放置后尝试补齐一整层
- 使用 `ConnectivityHandler.partAt(...)` 找到多方块 controller
- 只在上下方向、非潜行、热栏无对称法杖时触发
- 不足数量或目标层不可替换则放弃
- 放置时临时写入玩家 persistent data：`SilenceTankSound`

物品携带 BlockEntityTag 时：

- 清除 `Luminosity`、`Size`、`Height`、`Controller`、`LastKnownPos`
- 若有 `TankContent`，用 Forge 1.20.1 `FluidStack.loadFluidStackFromNBT(...)` 读取并裁剪到单方块容量

## 客户端渲染与模型

入口：`client/BoosterClient.java`

- 注册 BE renderer：
  - 强力泵：`PowerfulMechanicalPumpRenderer`
  - 下界合金储罐：`NetheriteFluidTankRenderer`
- 使用 `CreateClient.MODEL_SWAPPER.getCustomBlockModels()` 注册自定义模型处理：
  - 强力泵：`PipeAttachmentModel::withAO`
  - 储罐：`NetheriteFluidTankModel::standard`
- 储罐渲染层：`ItemBlockRenderTypes.setRenderLayer(..., RenderType.cutoutMipped())`
- Flywheel visual：`SimpleBlockEntityVisualizer.builder(...).factory(PowerfulMechanicalPumpVisual::new).apply()`

强力泵 cog：

- Partial model：`BoosterPartialModels.POWERFUL_MECHANICAL_PUMP_COG`
- 资源路径：`assets/createnetherite/models/block/powerful_mechanical_pump/cog.json`
- Renderer 在非 Flywheel visualization 时用 `KineticBlockEntityRenderer` 渲染动态齿轮
- 注意不要让静态 block model 和动态 renderer 同时渲染齿轮，否则会出现双重齿轮

储罐模型：

- `NetheriteFluidTankModel extends CTModel`
- 使用 Create 的 `FluidTankCTBehaviour`
- 自定义 `CTSpriteShiftEntry`：`fluid_tank`、`fluid_tank_top`、`fluid_tank_inner`
- 通过 `ConnectivityHandler.isConnected(...)` 剔除内部面
- 储罐液体显示委托 `FluidTankRenderer`，包装类为 `NetheriteFluidTankRenderer`

## 资源与数据包

Forge 1.20.1 数据路径仍为复数：

- `data/createnetherite/recipes/...`
- `data/createnetherite/loot_tables/...`
- `data/forge/tags/items/...`
- `data/minecraft/tags/blocks/...`

主要资源：

- `assets/createnetherite/blockstates/powerful_mechanical_pump.json`
- `assets/createnetherite/blockstates/netherite_fluid_tank.json`
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
- 下界合金板：Create pressing，输入 `forge:ingots/netherite`，输出 `createnetherite:netherite_sheet`

## 已知限制与注意点

- 不支持用下界合金流体储罐参与 Create 锅炉多方块结构。
- 强力机械泵不改变 Create 原版流向语义；正反转仍按原版泵行为处理。
- 压力倍率只提高 Create fluid network 的 pressure，不增加管道最大距离。
- 不要修改 Create 原版源码。
- 不要把 Booster/createnetherite 类放入 `com.simibubi.create` 包。
- 升级 Create 后必须重新检查三个 Mixin target。
- Forge 1.20.1 的 `ResourceLocation` 构造器、tooltip 签名、NBT API 不适用于 NeoForge 1.21.1。