# Create Netherite - NeoForge 1.21.1 技术交接文档

本文档描述 `1.21.1-NeoForge` 分支上的 NeoForge 1.21.1 版本实现，面向后续接手开发的 AI 或开发者。不要把本文档中的 NeoForge 1.21.1 API 直接照搬到 Forge 1.20.1 分支。

## 维护方向

自 2026-10-29 起，`1.21.1-NeoForge` 为主要维护分支，后续功能、问题修复和依赖更新以此为主。Minecraft 1.20.1 Forge 的 `main` 分支暂停更新，保留现有实现和[历史交接文档](forge-1.20.1.md)，暂停期间不再同步新增内容。

## 版本与分支

- 分支：`1.21.1-NeoForge`
- 源码核对日期：`2026-10-30`
- 源码基线提交：`236da18f137489703b0a9918749ef4dd4f7ec044`（本次后续修改新增下界合金动力搅拌机）
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

- 2026-10-29 当前源码包含 `src/main/java` 的 52 个 Java 文件；核对了新增鼓风机与过滤器的注册、配置、Mixin、菜单、配方和资源路径，81 个资源 JSON 的语法解析通过。
- 2026-10-29 锅炉等级调整已通过 `.\gradlew.bat --offline --no-configuration-cache build`，其中 `check` 阶段执行了 15 项锅炉逻辑回归检查，覆盖超过 18 级的尺寸/供水、引擎效率、尺寸/供水/热量不足、无水、被动加热、18 级提示及原版锅炉限制。
- 同日防爆调整通过相同构建命令及现有 15 项锅炉回归检查；另外核对了 NeoForge 伤害判定和爆炸受影响方块列表的调用路径。尚未进行游戏内爆破实测。
- 同日掉落物防火调整通过相同构建命令及现有 15 项锅炉回归检查；核对了八种物品的注册属性及原版 `ItemEntity` / `ItemStack` 防火路径。尚未进行游戏内火焰、岩浆实测。
- 同日鼓风机与过滤器通过 `.\gradlew.bat --offline --no-configuration-cache --no-daemon --console=plain build`，执行 15 项锅炉检查与 15 项过滤器检查，共 30 项通过；后者覆盖匹配规则优先、装有过滤器时未匹配物品回退当前模式、跨区移动、满区保留和数据往返。独立源码审查未发现重要问题。
- 当前 `0.2.4` 已用 `runClient` 启动并进入新建的独立创造测试世界，未发现模组加载失败或 Mixin 应用失败。资源下载未全部完成，因此此次启动跳过 `downloadAssets` 并复用已下载资源，主菜单背景出现原版纹理缺失；未发现新增模型加载错误。界面操作因同时输入而停止，尚未完成两个菜单、鼠标拖拽、数字键以及真实气流加工的游戏内验证；未启动独立服务端。
- 构建仍报告现有 API 和 Gradle 弃用提示；本次离线构建关闭配置缓存，以避开缓存序列化时对未缓存渲染依赖的解析。
- 旧交接记录中的构建成功、客户端资源重载成功，对应 Create Netherite `0.1.0`、NeoForge `21.1.242`。当时未发现 Mixin 应用失败、模组加载失败或缺失模型错误；该记录仅是历史验证，不能代替当前 `0.2.4` 的构建和运行验证。
- 2026-10-30 锅炉门槛与引擎输出调整通过 `.\gradlew.bat --offline --no-configuration-cache --no-daemon --console=plain build`：29 项锅炉检查和 15 项过滤器检查共 44 项通过。新增锅炉用例先在旧实现上失败，修改后通过；引擎 256 倍容量供应器及最终网络输出公式已对照锁定版本的 `SteamEngineBlockEntity` / `PoweredShaftBlockEntity` 源码核对。本次未重新启动客户端或服务端，也未进行游戏内输出实测。
- 同日四烈焰人燃烧室通过完整离线构建：29 项锅炉、59 项燃烧室、15 项过滤器共 103 项检查通过；81 个资源 JSON 解析通过，当前包含 59 个 Java 源文件。新增用例先复现数量/燃料功能缺失，以及生存单件、创造模式副本和蓝图数量保留问题，修复后通过。独立审查发现的问题均已处理，并补齐机械手识别；贴图及封面没有变更。
- 最终版本客户端已完成模组加载和资源重载，未发现本模组注册或 Mixin 应用错误。此次仍跳过未完成的原版资源下载，日志有原版主菜单纹理缺失警告。没有进入世界手动验证实体捕获、机械手喂料或多头渲染，测试结束后停止了客户端。
- 同日空容器补充通过完整离线构建：29 项锅炉、94 项燃烧室、15 项过滤器共 138 项检查通过，84 个资源 JSON 解析通过。新增检查先复现 0 被当作 1 的问题，再验证 0→4 连续捕获、空堆叠首捕、创造模式首捕、空热量/保存、无 BE 空状态识别及蓝图匹配。两个真实空配方的 result 经 ItemStack CODEC 解码验证组件和数量，独立环境仅把结果物品 ID 替换为已注册原版物品；不等于在世界中实际合成。独立审查未发现重要问题，本次未重新启动客户端或进行游戏内操作实测。
- 同日下界合金机械手通过完整离线构建：新增 23 项机械手检查，加上 29 项锅炉、94 项燃烧室、15 项过滤器，共 161 项通过。动作、应力和距离的新倍率先在原版行为基线上失败，随后实现通过；97 个资源 JSON 解析通过，当前包含 67 个 Java 源文件。模型 parent 与本模组纹理引用均已核对存在。
- 机械手版本客户端已完成模组加载、资源重载与图集构建，未发现注册、Mixin 应用或新增模型错误；随后停止测试客户端。此次仍跳过尚未完整下载的原版资源，主菜单背景纹理有缺失警告。尚未进入世界实测远距选点、实际搬运、红石和两种渲染路径中的动作，也未启动独立服务端。
- 机械手独立只读审查未发现重要且可证明的问题；核对了原生选点/放置回包链路、相对坐标保存与反序列化前校验、原版行为隔离、动态应力，以及 renderer / visual 的实际方法描述符与构造器参数位置。
- 机械手专用贴图由既有美术对话以 `e5275c5` 单独提交；本任务接入静态、动态与物品模型引用。最终 PNG 尺寸为原生 32×32，alpha 通道与原图一致，接入后 97 个 JSON 与模型/纹理路径检查通过。
- 包含最终 PNG 的版本再次通过完整构建和 161 项回归；核对 jar 中机械手类、Mixin、配方、全部模型与贴图均已打包，资源内容与源文件一致。最终资源版本客户端再次完成加载、资源重载及图集构建，未发现模组、Mixin 或模型加载错误，测试后停止；游戏内操作验证范围仍如上所述。
- 同日下界合金动力搅拌机通过完整离线构建：新增 11 项批量控制检查，加上现有 161 项，共 172 项通过。当前 72 个 Java 源文件、103 个资源 JSON；JSON 解析与 Create 模型 parent 路径检查通过。独立只读审查未发现重要问题，并据原生侧向输出缓存语义修正文档。本次检查尚不等同于世界中真实物品/流体配方加工。
- 搅拌机专用 `base_side.png` 与 `head.png` 由既有美术对话以 `aaf6c8a` 单独提交、推送，本任务只接入模型引用。两张最终 PNG 均为原生 16×16，alpha 通道与原图一致；103 个 JSON、模型 parent 和本模组纹理路径最终检查通过。
- 最终含贴图版本通过 `jar` 打包，核对其中搅拌机类、两个客户端 Mixin、全部新资源与源文件一致。随后 `runClient -x downloadAssets` 完成模组加载、资源重载与图集构建，未发现模组注册、Mixin 或模型加载错误，测试后停止客户端。此次仍有未下载的原版主菜单资源缺失警告；未进入世界实测混合、酿造、加热、真实流体或批量侧向输出，也未启动独立服务端。

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
- `createnetherite:netherite_mechanical_mixer`
- `createnetherite:netherite_mechanical_arm`
- `createnetherite:netherite_blaze_burner`
- `createnetherite:netherite_encased_fan`
- `createnetherite:netherite_filter`
- `createnetherite:netherite_sheet`

创造模式标签页：

- 注册位置：`BoosterCreativeModeTabs`
- 标签页 ID：`createnetherite:createnetherite`
- 图标：强力机械泵
- 包含：上述十种方块的物品、下界合金过滤器和下界合金板，共十二种注册物品；空燃烧室是同一物品 ID 的组件变体

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
  - `BoosterMenuTypes`
- 客户端侧通过 `FMLEnvironment.dist == Dist.CLIENT` 调用 `BoosterClient.register(...)`
- 在 `FMLCommonSetupEvent` 中用 `BlockStressValues.IMPACTS.register(...)` 注册强力机械泵基础应力冲击
- 在 `FMLCommonSetupEvent` 中以 supplier 注册下界合金蒸汽引擎的原版 256 倍基础容量和最高 64 RPM
- 升降机滑轮、动力冲压机、动力搅拌机的基础应力 impact 通过 supplier 读取 Create 对应原版方块的值
- 机械手的基础应力 impact 通过 supplier 读取原版机械手当前配置值并乘以 1.5
- 将下界合金保险库关联到 `AllMountedStorageTypes.VAULT`，复用 Create 的运动结构物品存储类型
- 为下界合金燃烧室注册按烈焰人数量放大正热量的 `BoilerHeater` 和 `NetheriteBlazeBurnerMovementBehaviour`
- 下界合金鼓风机的基础应力 impact 使用 Create 原版鼓风机的值，风力倍率不改变动能网络 RPM
- 向 `NeoForge.EVENT_BUS` 注册 `BoosterExplosionProtection` 的方块爆炸及实体无敌判定监听器

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
- `DeferredBlock<NetheriteMechanicalMixerBlock> NETHERITE_MECHANICAL_MIXER`
- `DeferredBlock<NetheriteMechanicalArmBlock> NETHERITE_MECHANICAL_ARM`
- `DeferredBlock<NetheriteBlazeBurnerBlock> NETHERITE_BLAZE_BURNER`
- `DeferredBlock<NetheriteEncasedFanBlock> NETHERITE_ENCASED_FAN`

属性大多复制 Create 对应方块。十种方块的基础爆炸抗性统一为 `1200.0F`：储罐和蒸汽引擎继承下界合金块属性，其余方块显式设置。强力泵基于铜块属性。

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
- `DeferredItem<Item> NETHERITE_MECHANICAL_MIXER`：原生 `AssemblyOperatorBlockItem`，支持点击工作盆顶面时自动留出一格间距放置
- `DeferredItem<Item> NETHERITE_MECHANICAL_ARM`：复用 Create `ArmItem`，保留原生选点和放置请求
- `DeferredItem<Item> NETHERITE_BLAZE_BURNER`：使用 `NetheriteBlazeBurnerItem`，默认 1 只，可逐只捕获至 4 只
- `DeferredItem<Item> NETHERITE_ENCASED_FAN`：普通 `BlockItem`
- `DeferredItem<Item> NETHERITE_FILTER`：`NetheriteFilterItem`，继承 Create `FilterItem`

上述十二种物品的 `Item.Properties` 均设置 `.fireResistant()`，使用原版下界合金物品的火焰与岩浆伤害保护。

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
- `NETHERITE_MECHANICAL_MIXER` 绑定 `NetheriteMechanicalMixerBlockEntity`
- `NETHERITE_MECHANICAL_ARM` 绑定 `NetheriteMechanicalArmBlockEntity`
- `NETHERITE_BLAZE_BURNER` 的注册泛型保留 `BlazeBurnerBlockEntity`，实际工厂创建 `NetheriteBlazeBurnerBlockEntity`
- `NETHERITE_ENCASED_FAN` 绑定 `NetheriteEncasedFanBlockEntity`

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
| `netheriteFanProcessingSpeedMultiplier` | `4` | `1..64` | 每 tick 推进的原版风机加工步骤数 |
| `netheriteFanWindSpeedMultiplier` | `2.0` | `1.0..16.0` | 气流运动计算中的风速倍率 |

已有世界会保留自己的 `serverconfig/createnetherite-server.toml`。修改默认值不会覆盖已有世界配置；修改世界配置后重启世界或服务器，使已有动能、管网、储罐和升降机按新值重新计算。

保险库的 16 倍槽位、升降机的 2 倍转速系数、机械手的 3 倍动作/范围与 1.5 倍基础应力，以及燃烧室最多 4 只、每只 1 倍热量 / 0.5 倍耗料，目前是代码规则，不是配置项。机械手范围和应力的原始值读取 Create 对应配置。

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
- `getMaxHeatLevelForBoilerSize(int boilerSize)` 以储罐方块总数判定门槛：不足 `45` 时返回 `boilerSize / 4`（整数除法）；达到 `3×3×5 = 45` 个方块后，返回 `max(18, min(供水等级, activeHeat))`。尺寸等级先达到原版满尺寸等级 18，之后随供水和热量的较低值上涨，不使用无穷大或极大整数作为显示等级。
- `getMaxHeatLevelForWaterSupply()` 返回 `ceil(waterSupply) / 12`（整数除法），供水等级随平均注水速度增加；保留原有向上取整规则和每级 12 mB/t 的需求，Create 原版为每级 10 mB/t。
- 实际锅炉等级仍由 Create 取尺寸、供水和热量等级的最低值，用于引擎效率、护目镜显示和读档后的仪表计算。因此达到 45 个方块后，实际等级就是 `min(供水等级, 热量等级)`。例如 45 个储罐方块、240 mB/t 供水、20 级热量可达到 20 级；供水和热量均提升到 100 级时，无须再扩大锅炉。缩小到 44 个方块后恢复尺寸等级 11 的限制。无水、无热量和被动加热保留原版行为。
- `Booster.commonSetup` 为下界合金蒸汽引擎注册的容量供应器动态读取 `BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get()) * 256`，使单个引擎的输出上限为当前原版引擎配置的 256 倍；最高转速仍为 64 RPM。
- `NetheriteSteamEngineBlock` 和对应方块实体只允许连接下界合金流体储罐。
- `BoilerDataMixin` 只让下界合金储罐识别下界合金蒸汽引擎，并修正护目镜中的应力与耗水显示。
- `SteamEngineBlockEntityMixin` 让 Create 的活塞运动与目标角度逻辑识别自定义蒸汽引擎。
- 普通 Create 储罐仍只识别普通蒸汽引擎，两种锅炉不能混搭。

Create 默认原版引擎注册容量值为 `1024`，下界合金引擎为 `262144`。`PoweredShaftBlockEntity` 将注册容量乘以引擎效率，再除以转速档位系数作为网络基础容量，同时以 `16 × 档位系数` RPM 发电，所以单个引擎的网络输出为 `16 × 注册容量 × 引擎效率`。效率为 1 时，原版输出 `16384 SU`，下界合金引擎输出 `4194304 SU`，恰为 256 倍；不要直接把注册值乘以 64 当作最终输出。锅炉状态不足或连接引擎过多时，Create 仍按效率降低实际输出。修改原版蒸汽引擎的应力配置后，此倍率随之生效。

护目镜显示修正位于 `BoilerDataMixin.addToGoggleTooltip`：针对 `NetheriteBoilerData` 替换容量查询的引擎方块，并把每级耗水常量从 `10.0` 改为 `12`。

`NetheriteBoilerData.getHeatLevelTextComponent()` 将父类在 18 级返回的 `create.boiler.max_lvl` 提示替换为数值等级 `create.boiler.lvl`，其余等级、被动加热和闲置提示继续使用父类结果。Create 普通锅炉保留原版 18 级限制和提示。

### 锅炉回归检查

检查入口：`src/boilerTest/java/io/github/boosterproject/booster/content/fluids/tank/BoilerLevelTest.java`。

```text
.\gradlew.bat boilerRegressionTest
```

该任务运行实际的 Create `BoilerData` 和当前 `NetheriteBoilerData`，无需启动游戏或额外测试库，并已接入 `check` / `build`。检查覆盖 44/45 个方块边界、尺寸随水量/热量变化、100 级锅炉效率、缩小后的限制、无水/无热量、被动加热、18 级提示及原版锅炉行为；不代替世界中的热源扫描、管道注水和多方块行为验证。

## 防爆实现

文件：`content/BoosterExplosionProtection.java`。当前覆盖本模组全部十种方块与十二种物品，包括强力机械泵、下界合金机械手、动力搅拌机、下界合金过滤器和下界合金板。

- `Booster` 构造时通过 `NeoForge.EVENT_BUS.addListener(...)` 注册两个监听器。
- `ExplosionEvent.Detonate`：从 `getAffectedBlocks()` 中排除注册命名空间为 `createnetherite` 的方块位置，保留放置后的方块及其 BE 数据。储罐流体和保险库存货随方块保留。
- `EntityInvulnerabilityCheckEvent`：仅当实体为 `ItemEntity`、物品属于 `createnetherite`，且伤害源带有 `DamageTypeTags.IS_EXPLOSION` 时，调用 `setInvulnerable(true)`。因此下界合金板和所有方块掉落物都受到保护。
- 保护范围按注册命名空间识别，沿用现有 Create 方块与物品类，不新增 Mixin。普通 Create 和 Minecraft 内容不在此保护范围内。

## 掉落物防火实现

文件：`registry/BoosterItems.java`。全部十二种物品的注册属性均调用 `Item.Properties.fireResistant()`，包括强力机械泵、下界合金机械手、动力搅拌机、下界合金过滤器和下界合金板。

该原版属性设置 `DataComponents.FIRE_RESISTANT`：`ItemEntity.fireImmune()` 据此返回免疫，`ItemStack.canBeHurtBy(...)` 也会拒绝带有 `DamageTypeTags.IS_FIRE` 的伤害。因此掉落物不会被火焰或岩浆烧毁。防爆仍由独立的 `BoosterExplosionProtection` 事件监听器处理。

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

## 下界合金动力搅拌机实现

文件：`content/kinetics/mixer/NetheriteMechanicalMixerBlock.java`、`NetheriteMechanicalMixerBlockEntity.java`、`NetheriteMixerBatching.java`。

- 方块与 BE 继承 Create 原生类型，物品使用原生 `AssemblyOperatorBlockItem` 保留工作盆上方自动间隔放置。复用原生配方筛选、搅拌计时、存档、流体运动、声音、粒子和最低转速要求。不覆盖 `getSpeed()`，不额外乘动作或等待时间倍率。
- 与当前下界合金冲压机一致，加速来自批量处理：原生搅拌周期结束时，重写 `applyBasinRecipe()`，循环处理当前配方，不在同一批中自动切换其他配方。吞吐收益随可处理数量而变，不是固定的动作速度倍率。
- 每轮重新调用原生 `matchBasinRecipe(currentRecipe)`，实际调用 `BasinRecipe.apply`；任一返回 false 即停止。原生路径负责过滤器、热量、物品/流体消耗、配方结果与可接受输出检查，实际 apply 失败不会在该批中反复重试。
- 输出行为仍为 Create：向下输出时检查盆内输出空间；侧向输出可先加入原生 `spoutputBuffer`，接收容器已满不代表本批立即停止。不会额外改写侧向缓存或传输规则。
- 每次成功应用保留原生配方进度触发，批量成功后同步输入流体并通知工作盆内容变化。原生 tick 在应用前已把 `runningTicks` 从 20 推到 21，本批不调用会将其重置为 20 的 `continueWithPreviousRecipe()`，因而完成后正常收回搅拌头。
- `BlockStressValues.IMPACTS` 的 supplier 动态读取普通搅拌机当前基础值，倍率为 1，与下界合金冲压机的相对应力倍率一致；默认是 4 SU/RPM。不能将普通冲压机的绝对应力数值直接用于搅拌机。

客户端复用 `MechanicalMixerRenderer` / `MixerVisual` 并注册原生 `cutoutMipped` 方块渲染层。`BoosterPartialModels.NETHERITE_MIXER_HEAD` 提前注册专用头部分件，两个客户端 Mixin 仅对新 BE 的原生搅拌头替换模型；齿轮、杆件和动画继续使用原生资源与逻辑。Visual 构造器注入为 static handler，并仅捕获 `MechanicalMixerBlockEntity` 构造参数，兼容父类构造前的分件调用。机身与物品模型继承原生几何，机身侧面与头部使用既有美术对话制作的新 PNG，顶部使用原版下界合金块纹理。

`mixerRegressionTest` 使用现有轻量源集并接入 `check` / `build`。11 项检查覆盖批量处理、材料消耗、匹配条件失效及实际应用失败停止；先在单次处理基线上失败，批量控制实现后通过。此测试验证批量控制，不代替真实工作盆的物品/流体、加热或侧向输出实测。

## 下界合金机械手实现

文件：`content/kinetics/mechanicalArm/NetheriteArmBehavior.java`、`NetheriteMechanicalArmBlock.java`、`NetheriteMechanicalArmBlockEntity.java`。

- 方块和 BE 继承 Create `ArmBlock` / `ArmBlockEntity`，物品直接使用 `ArmItem`；保留输入/输出选点、分配模式、红石锁定、搬运和燃烧室喂料、物品持有及 NBT 保存。
- `NetheriteMechanicalArmMixin` 仅针对新 BE，把 `tickMovementProgress` 中的 `min(256, abs(getSpeed()))` 改为 `3 * min(256, abs(getSpeed()))`，然后仍由原版除以 1024 推进动作。最高有效动作速度为 768，实际动能转速不变。没有覆盖 `getSpeed()`，所以不会连带放大应力或改变动能网络连接。
- 应力供应器读取 `BlockStressValues.getImpact(AllBlocks.MECHANICAL_ARM.get()) * 1.5`；默认原版基础值为 2，因此新机械手为 3 SU/RPM。实际应力由 Create 按输入 RPM 计算。
- 工作半径为 `ArmBlockEntity.getRange() * 3`，默认 5→15；使用原版 `BlockPos.closerThan` 球形严格边界（轴向偏移 14 可达，15 不可达）。读取 Create 的配置，不新增重复范围配置。计算上限为 `Integer.MAX_VALUE - 1`，避免原生加载检查的 `range + 1` 溢出。
- 同一 BE Mixin 扩大 `initInteractionPoints` 的已加载区域检查，并在调用 `ArmInteractionPoint.deserialize`、访问目标方块之前，读取 NBT 的相对 `Pos` 拒绝超距目标。保留原生区块加载检查，不强制加载远处区块。BE 的渲染包围盒同步扩大。
- 客户端 `NetheriteArmSelectionMixin` 扩展原生手持物识别；`flushSettings` 根据已放置的新方块扩大选点距离，沿用 Create 放置请求与回包，无新增网络消息。服务器原生放置包接受 `ArmBlockEntity` 子类，之后由上述初始化路径校验相对坐标。
- 速度倍率描述的是每 tick 动作进度。到达目标后的阶段切换、物品转移和等待规则仍为原版，不能据此承诺所有流水线吞吐量恰好为 3 倍。

客户端复用 `ArmRenderer` / `ArmVisual`，`NetheriteArmModels` 提前注册八个专用分件。`NetheriteArmRendererMixin` 按方块类型替换 `CachedBuffers.partial` 的分件；`NetheriteArmVisualMixin` 按 BE 类型替换构造与护目镜更新时 `Models.partial` 的模型。构造器中首个分件位于父类构造调用前，注入使用 static handler 和仅捕获构造参数的 `@Local(argsOnly = true)`，不访问尚未初始化的 `this`。

模型继承锁定版原生机械手几何，底座金属块替换为下界合金块；原生机械手图集对应的纹理槽统一引用 `textures/block/netherite_mechanical_arm/arm.png`。专用 PNG 由用户指定的美术对话基于 Create 原图制作，功能任务仅接入引用；齿轮继续使用原生飞轮材质，护目镜和其他非机身纹理保留。

`armRegressionTest` 已接入 `check` / `build`，覆盖动作倍率、正反转、速度上限、应力配置、范围配置、球形边界和序列化相对目标。它验证计算与 NBT 边界，游戏内选点、搬运和运动中的外观需单独实测。

## 下界合金烈焰人燃烧室实现

文件：`content/processing/burner/NetheriteBlazeBurner{Block,BlockEntity,Item,Data,MovementBehaviour}.java`。

- 方块和 BE 分别继承 Create `BlazeBurnerBlock` / `BlazeBurnerBlockEntity`，沿用自己的 BE 注册 ID。数量范围为 `0..4`，显式 `BlazeCount=0` 表示空；旧物品、旧存档中缺少该字段时仍视为 1 只。空和非空共用 `createnetherite:netherite_blaze_burner` 物品 ID，通过组件区分，不增加注册物品 ID。
- 物品是自定义 `BlockItem`。捕获沿用 `create:blaze_burner_capturable` 实体标签；手持右键实体时只在服务端完成捕获。单件直接修改原手持对象，以兼容 `Player.interactOn` 在旧对象变空时清空手部的处理；创造模式下该 API 传入的是副本，因此显式使用 `player.getItemInHand(hand)` 的真实物品。堆叠物品则拆出一份交付背包，其余数量数据不变。满 4 只时拒绝捕获，不移除实体。此功能针对实际实体，不从刷怪笼直接增加数量。
- 物品数量保存在 `DataComponents.CUSTOM_DATA.BlazeCount`，修改时保留其他物品数据。放置时写入 BE；掉落表照常生成自己的物品，再由方块 `getDrops` 附加数量。中键复制保留数量；拆除只保留烈焰人，不保留可重复利用的燃料。`writeSafe` 向蓝图仅额外保存数量，`BlazeCountRequirement.matches` 同时比较物品种类和数量，避免蓝图炮消耗数量不符的物品造成烈焰人丢失或复制。
- 放置 0 只的物品使用 `HeatLevel.NONE`，沿用原版不创建 BE 的规则；`getBlazeCount(state, be)` 优先识别空状态，因此空容器的掉落、复制和蓝图物料需求都保留显式 0，不会因没有 BE 而回退成 1。空状态不接受燃料，且屏蔽父类会把笼体替换为普通 Create 方块的打火石操作。移动结构在空状态下直接跳过头部渲染，不读取可能为空的 BE 数据。
- BE NBT 保存 `BlazeCount` 与 `FuelHalfTick`（`0..1`）。每个服务端 tick 增加 `BlazeCount` 个半燃料 tick，整数部分用于扣除原版燃烧时间，余数保存到下一次。因此 1～4 只的耗料倍率依次为 `0.5 / 1 / 1.5 / 2`，包括半 tick 进度的存档往返。
- 父类 `tick` 每 tick 只调用一次：在调用前调整燃烧时间，以抵消/补足父类自带的一次扣减；特殊燃料用完时未付清的一次消耗从原版正常热量余热中扣除。客户端与创造燃料不走耗料调整。燃料识别、数据映射、容器返回、模拟插入、手动/机械手喂料、普通/超级加热及烈焰蛋糕余热切换继续由原版处理。
- `BoosterArmInteractionPointTypes` 使用 DeferredRegister 向 Create 的 `ARM_INTERACTION_POINT_TYPE` 注册 `createnetherite:netherite_blaze_burner`，继承原生 `BlazeBurnerType`，仅扩展方块识别；`BlazeBurnerPoint` 继续调用原生 `tryInsert`，处理机械手的模拟插入与燃料容器返回。没有覆盖普通燃烧室的交互类型。
- `Booster.commonSetup` 注册 `NetheriteBlazeBurnerBlock::getBoilerHeat`：空容器始终返回 `NO_HEAT=-1`；非空时原版正热量乘数量，`NO_HEAT=-1` 和 `PASSIVE_HEAT=0` 原样返回。普通加热每只 1 级、超级加热每只 2 级，满载分别提供 4 / 8 级。该倍率用于锅炉，工作盆仍使用 Create 原生加热状态。
- `NetheriteBlazeBurnerRenderer` 按数量排列原版烈焰人模型；1 只时保持原版大小，2～4 只时以半尺寸排列。该 BE 使用常规 renderer 而非原版单头 Flywheel visual。客户端 `NetheriteBlazeBurnerAnimationMixin` 只让该子类继续更新原生头部动画，普通燃烧室不变。移动结构通过自定义 movement behaviour 保留同样的数量渲染与原版列车行为。没有修改贴图。
- 物品说明和工程师护目镜显示捕获数量、热量和耗料倍率。
- `NetheriteBlazeBurnerItem.getName` 在 0 只时显示空容器名称。`BoosterClient` 在客户端初始化队列注册 `createnetherite:empty` 物品属性，空状态选用 `models/item/empty_netherite_blaze_burner.json`；该模型复用原版空笼体几何和已有下界合金笼体贴图。创造栏同时提供 0 只与默认 1 只的变体。

`burnerRegressionTest` 使用实际 Minecraft 物品、原版热状态函数及自定义 BE 检查数量、单份捕获数据、元数据保护、倍率和存档；已接入 `check` / `build`。测试状态定义使用已注册原版方块作为所有者，避免向冻结的注册表写入测试方块；不替代世界中的捕获交互、自动喂料和渲染验证。

## 下界合金鼓风机与过滤器

主要文件：`content/kinetics/fan/FanProcessingMode`、`NetheriteEncasedFanBlock`、`NetheriteEncasedFanBlockEntity`、`NetheriteAirCurrent`、`content/logistics/filter/NetheriteFilterData`、`NetheriteFilterItem`。

### 加工选择与速度

四种方式依次为水洗、岩浆熔炼、灵魂火缠魂和火焰烟熏，对应 `AllFanProcessingTypes.SPLASHING` / `BLASTING` / `HAUNTING` / `SMOKING`。默认水洗，模式可以随时切换，风口前不需要实体介质。优先规则是：

```text
匹配过滤器中的物品种类 -> 该区域指定方式
未匹配或未装过滤器 -> 鼓风机所选统一方式
```

BE 替换 `airCurrent` 为 `NetheriteAirCurrent`。它复用 Create 的碰撞与距离计算，`getTypeAt` 在有效风距内返回所选方式，覆盖短小的 `tickAffectedHandlers` 为传送带/置物台中的每件物品分别选择方式。地面掉落物由 `NetheriteAirCurrentMixin` 在原版 `tickAffectedEntities` 中完成相同选择。

两条路径都调用实际的 `FanProcessing.applyProcessing`，保留配方、堆叠数量导致的耗时、产出和副产物处理。每 tick 最多推进配置次数（默认 4），转换或销毁后立即退出，避免同一轮重复处理产出。没有动力时沿用原版停止行为。普通 Create 风机不受这些修改影响。

风速倍率只修改新风机在气流实体运动计算中读取的 `IAirCurrentSource.getSpeed()`，默认乘 2；不覆盖 BE 的动能 `getSpeed()`，不改变网络转速、应力换算或原版最大风距。实际运动仍遵循 Create 的加速度限制。

### 过滤器数据与两个菜单

- 四个区域各有 9 格，共 36 个物品种类样本。`NetheriteFilterData` 将物品 ID 存入 `DataComponents.CUSTOM_DATA` 的 `FanRules` 字符串列表，索引除以 9 得到区域。不保存原物品数量或嵌套数据；保留其他 CUSTOM_DATA 字段。
- 鼠标设置及数字键设置共用 `assign` / `assignToGroup`：同种物品只保留一个区域规则，跨区重新分配会移动，满区拒绝且不删除旧规则。未匹配回退统一方式是用户明确要求。
- `NetheriteFilterMenu` 继承 Create `HeldItemGhostItemMenu`。主手持过滤器右键配置，玩家槽 0..35、样本槽 36..71，主手过滤器锁定。样本不能被拾取为真实物品。
- `NetheriteFilterScreen` 支持一次按下背包物品、拖到样本、松开：使用原版 PICKUP 消息设置样本，并把原堆叠放回源槽；已有光标物品不消耗。样本左键设置、右键或空手清除。
- 悬停物品按 1..4 使用原版 SWAP 点击消息表达分组，服务端从被指向的槽读取样本，不信任客户端提交的物品内容。其他热栏交换操作被过滤器菜单拦截。
- `NetheriteFanMenu` 提供一个真实过滤器槽，限制一个下界合金过滤器；用 `DataSlot` 同步模式、原版 `clickMenuButton` 消息选择 0..3。服务端核对玩家、距离和 BE 身份，过滤器存在时仍允许切换模式。
- `BoosterMenuTypes` 注册两个 MenuType，客户端在 `RegisterMenuScreensEvent` 注册 `NetheriteFilterScreen` 与 `NetheriteFanScreen`；没有新增自定义网络 payload。

鼓风机将模式名称保存在 NBT `Mode`，过滤器库存保存在 `Filter`；客户端同步使用同一读写路径。BE `destroy()` 在服务器侧取出并掉落过滤器，保留其规则。`NetheriteFilterItem` 在普通 Create 物流过滤场景中把全部区域样本作为物品白名单。

### 合成、外观与检查

一个下界合金板与一个 `create:encased_fan` / `create:filter` 分别无序合成一个升级品。风机复用原版几何和动态风叶，以原版下界合金纹理替换机壳；过滤器使用 JSON 原生模型构造下界合金框与四块颜色区分的面板，无额外位图依赖。

`filterRegressionTest` 复用现有轻量 Java 检查源集，在初始化的 Minecraft 物品环境中验证样本不消耗、规则优先/回退、跨区移动、满区保留、无效槽拒绝和 CUSTOM_DATA 往返。Minecraft 资源通过 `createMinecraftArtifacts.resourcesArtifact` 任务输出 Provider 加入 classpath，不硬编码本机缓存路径。

```text
.\gradlew.bat filterRegressionTest
```

该任务已接入 `check` / `build`。目前它检查物品规则，不等价于完整游戏内界面和气流加工验证。

## 客户端渲染与模型

入口：`client/BoosterClient.java`

- 注册 BE renderer：
  - 强力泵：`PowerfulMechanicalPumpRenderer`
  - 下界合金储罐：`NetheriteFluidTankRenderer`
  - 下界合金蒸汽引擎：复用 Create `SteamEngineRenderer`
  - 下界合金升降机滑轮：复用 Create `ElevatorPulleyRenderer`
  - 下界合金动力冲压机：复用 Create `MechanicalPressRenderer`
  - 下界合金动力搅拌机：复用 Create `MechanicalMixerRenderer`，新 BE 替换专用头部分件
  - 下界合金机械手：复用 Create `ArmRenderer`，仅新方块替换专用分件
  - 下界合金烈焰人燃烧室：`NetheriteBlazeBurnerRenderer` 复用原生模型并按数量排列
  - 下界合金鼓风机：复用 Create `EncasedFanRenderer`
- 使用 `CreateClient.MODEL_SWAPPER.getCustomBlockModels()`：
  - 强力泵：`PipeAttachmentModel::withAO`
  - 储罐：`NetheriteFluidTankModel::standard`
  - 保险库：`CTModel` + `NetheriteItemVaultCTBehaviour`
- 储罐渲染层：`ItemBlockRenderTypes.setRenderLayer(..., RenderType.cutoutMipped())`
- Flywheel visual：强力泵使用 `PowerfulMechanicalPumpVisual`；蒸汽引擎、升降机、冲压机、搅拌机、机械手和鼓风机分别复用 `SteamEngineVisual`、`ElevatorPulleyVisual`、`PressVisual`、`MixerVisual`、`ArmVisual`、`FanVisual`。搅拌机与机械手替换专用分件，燃烧室采用常规 renderer 以显示实际数量。

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
- `assets/createnetherite/blockstates/netherite_mechanical_mixer.json`
- `assets/createnetherite/blockstates/netherite_mechanical_arm.json`
- `assets/createnetherite/blockstates/netherite_blaze_burner.json`
- `assets/createnetherite/models/block/powerful_mechanical_pump/block.json`
- `assets/createnetherite/models/block/powerful_mechanical_pump/cog.json`
- `assets/createnetherite/textures/block/powerful_mechanical_pump/pump.png`
- `assets/createnetherite/models/block/netherite_fluid_tank/*.json`
- `assets/createnetherite/textures/block/netherite_fluid_tank/*.png`
- `assets/createnetherite/models/block/netherite_item_vault.json`
- `assets/createnetherite/textures/block/item_vault/*.png`
- `assets/createnetherite/models/block/netherite_boiler/*.json`
- `assets/createnetherite/models/block/netherite_mechanical_arm/*.json`：静态底座及八个动态分件
- `assets/createnetherite/textures/block/netherite_mechanical_arm/arm.png`
- `assets/createnetherite/models/block/netherite_mechanical_mixer/{block,head}.json`
- `assets/createnetherite/textures/block/netherite_mechanical_mixer/{base_side,head}.png`
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
- 下界合金动力搅拌机：一个下界合金板 + 一个 `create:mechanical_mixer`，无序合成 1 个
- 下界合金机械手：一个下界合金板 + 一个 `create:mechanical_arm`，无序合成 1 个
- 下界合金烈焰人燃烧室：一个下界合金板 + 一个 `create:blaze_burner`，无序合成 1 个；另有两个板 + 三个原版燃烧室批量合成 3 个的有序配方
- 空的下界合金烈焰人燃烧室：对应使用 `create:empty_blaze_burner` 的单个与批量升级配方，输出同一物品 ID 并在 `result.components.minecraft:custom_data` 中写入 `BlazeCount: 0`。原有装填配方仍缺省为 1 只。
- 下界合金鼓风机：一个下界合金板 + 一个 `create:encased_fan`，无序合成 1 个
- 下界合金过滤器：一个下界合金板 + 一个 `create:filter`，无序合成 1 个
- 下界合金板：Create pressing，输入 `c:ingots/netherite`，输出 `createnetherite:netherite_sheet`

本分支补充了：

```text
data/c/tags/item/ingots/netherite.json
```

内容指向：

```text
minecraft:netherite_ingot
```

下界合金板同时加入 `c:plates`、`c:plates/netherite`。十种方块各有 `loot_table/blocks/` 掉落表，并全部加入 `minecraft:mineable/pickaxe`；除强力机械泵外，其余九种方块加入 `minecraft:needs_diamond_tool`。

## 当前 Mixin 清单

以 `src/main/resources/createnetherite.mixins.json` 为准：10 个通用 Mixin，7 个客户端 Mixin。配置使用 `required: true`、`JAVA_21` 和 `defaultRequire: 1`。

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
| `NetheriteAirCurrentMixin` | `AirCurrent.tickAffectedEntities` | 新风机掉落物加工规则、加工步进和气流风速 |
| `NetheriteMechanicalArmMixin` | `ArmBlockEntity.tickMovementProgress` / `initInteractionPoints` | 动作倍率、加载范围和目标反序列化前的距离校验 |
| `FluidTankRendererMixin`（客户端） | `FluidTankRenderer.renderAsBoiler` | 替换锅炉仪表模型 |
| `NetheriteBlazeBurnerAnimationMixin`（客户端） | `BlazeBurnerBlockEntity.shouldTickAnimation` | 自定义多头 renderer 继续使用原生动画 |
| `NetheriteArmSelectionMixin`（客户端） | `ArmInteractionPointHandler.tick` / `flushSettings` | 识别新机械手物品并扩大选点范围 |
| `NetheriteArmRendererMixin`（客户端） | `ArmRenderer.renderArm` / `getRotatedModel` | 替换非 Flywheel 分件 |
| `NetheriteArmVisualMixin`（客户端） | `ArmVisual.<init>` / `update` | 替换 Flywheel 分件并支持护目镜切换 |
| `NetheriteMixerRendererMixin`（客户端） | `MechanicalMixerRenderer.renderSafe` | 替换新搅拌机的非 Flywheel 头部分件 |
| `NetheriteMixerVisualMixin`（客户端） | `MixerVisual.<init>` | 替换新搅拌机的 Flywheel 头部分件 |

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
