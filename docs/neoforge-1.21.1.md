# Create Netherite - NeoForge 1.21.1 技术交接文档

本文档描述 `1.21.1-NeoForge` 分支上的 NeoForge 1.21.1 版本实现，面向后续接手开发的 AI 或开发者。不要把本文档中的 NeoForge 1.21.1 API 直接照搬到 Forge 1.20.1 分支。

## 维护方向

自 2026-10-29 起，`1.21.1-NeoForge` 为主要维护分支，后续功能、问题修复和依赖更新以此为主。Minecraft 1.20.1 Forge 的 `main` 分支暂停更新，保留现有实现和[历史交接文档](forge-1.20.1.md)，暂停期间不再同步新增内容。

## 版本与分支

- 分支：`1.21.1-NeoForge`
- 源码核对日期：`2026-11-02`
- 源码基线提交：`258581732409c92bd271c783fff83fe993a5a4ca`（后续新增无线终端与管理器；贴图提交 `7326d31`）
- 模组版本：`0.3.13`
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
- 2026-10-31 下界合金鼓风机思索通过完整离线构建和现有 172 项回归。独立审查发现置物台与风口同高会挡风，已把风机/轴抬至 y=2，置物台保留 y=1，修正后重新构建通过。当前 74 个 Java 源文件；103 个 JSON 解析通过，两种语言的 18 条 sharedText 与 3 个标题均齐全，jar 中结构、语言资源和场景类核对通过。
- 在独立 `Netherite Fan QA 2026-10-29` 测试世界中，实际通过原生 `/ponder` 入口打开鼓风机三段教程，检查英文标题/字幕、场景切换、示例物品变化、规则优先与回退及速度对比；过滤器物品入口也成功打开共用教程。未发现场景加载或播放异常。本地测试客户端原版资源未下载完整，语言列表为空，中文仅做字幕资源校对，尚未验证中文界面播放；该次验证不替代真实世界加工实测。
- 同日按用户要求将思索精简为两段用法教程，补上风机下方的安山机壳，并把禁止使用 RTX 5060 写入项目约定。修订版在 `-Djava.awt.headless=true` 下完整构建通过，现有 172 项检查全部通过；核对结构只新增一个支撑块、原有 55 个方块未改，15 条双语字幕和两个标题齐全，最终 jar 与源码资源一致。此次未启动游戏客户端，修订版未做新的播放实测。

- 同日物品提示统一为原生 Ponder / Shift 概要：在 `-Djava.awt.headless=true` 下完整离线构建通过，现有 172 项回归全部通过；103 个资源 JSON 解析通过，十种无思索物品的中英文概要齐全，原有保留翻译与思索字幕未变。核对最终 jar 包含概要修饰器与相同语言资源，已移除旧机械泵物品类。未启动客户端、未使用显卡，尚未实测本次概要界面。

按当前配置生成的主 jar：

```text
build/libs/createnetherite-1.21.1-0.3.13.jar
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
- `createnetherite:netherite_filter`（旧存档兼容，不再合成或在创造栏提供）
- `createnetherite:netherite_wireless_terminal`
- `createnetherite:netherite_infinite_manager`
- `createnetherite:netherite_smart_chute`
- `createnetherite:netherite_weighted_ejector`
- `createnetherite:netherite_extendo_grip`
- `createnetherite:netherite_deployer`
- `createnetherite:netherite_netherite`
- `createnetherite:netherite_sheet`

创造模式标签页：

- 注册位置：`BoosterCreativeModeTabs`
- 标签页 ID：`createnetherite:createnetherite`
- 图标：强力机械泵
- 包含：上述十四种方块的物品、下界合金无限管理器、下界合金伸缩机械手、下界合金下界合金和下界合金板，共十八种在用物品；另保留旧下界合金过滤器注册以兼容存档，注册物品总数为十九。空燃烧室是同一物品 ID 的组件变体

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
- `DeferredBlock<NetheriteSmartChuteBlock> NETHERITE_SMART_CHUTE`
- `DeferredBlock<NetheriteEjectorBlock> NETHERITE_WEIGHTED_EJECTOR`
- `DeferredBlock<NetheriteDeployerBlock> NETHERITE_DEPLOYER`

属性大多复制 Create 对应方块。十四种方块的基础爆炸抗性统一为 `1200.0F`：储罐和蒸汽引擎继承下界合金块属性，其余方块显式设置。强力泵基于铜块属性。

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
- `DeferredItem<Item> NETHERITE_FILTER`：保留 `NetheriteFilterItem` 注册和原规则查看以兼容旧存档，0.3.7起不再合成、列入创造栏或安装到鼓风机
- `DeferredItem<Item> NETHERITE_WIRELESS_TERMINAL`：普通 `BlockItem`
- `DeferredItem<Item> NETHERITE_INFINITE_MANAGER`：`InfiniteManagerItem`，手持右键打开分页管理器
- `DeferredItem<Item> NETHERITE_SMART_CHUTE`：普通防火 `BlockItem`，打开方向规则菜单
- `DeferredItem<Item> NETHERITE_NETHERITE`：`NetheriteNetheriteItem`，不消耗的循环啃食与10秒挑战进度
- `DeferredItem<Item> NETHERITE_DEPLOYER`：原生 `AssemblyOperatorBlockItem`，批量加工机械手
- `DeferredItem<Item> NETHERITE_EXTENDO_GRIP`：继承 `ExtendoGripItem`，200耐久、额外触及、取液与直接手动供能
- `DeferredItem<Item> NETHERITE_WEIGHTED_EJECTOR`：`NetheriteEjectorItem`，Shift右键选点，放置或右键已有升级置物台应用目标

上述十九种物品的 `Item.Properties` 均设置 `.fireResistant()`，使用原版下界合金物品的火焰与岩浆伤害保护。

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
- `NETHERITE_SMART_CHUTE` 绑定 `NetheriteSmartChuteBlockEntity`
- `NETHERITE_WEIGHTED_EJECTOR` 绑定 `NetheriteEjectorBlockEntity`
- `NETHERITE_DEPLOYER` 绑定 `NetheriteDeployerBlockEntity`

`BoosterBlockEntityTypes.register(...)` 同时监听 `RegisterCapabilitiesEvent`：

- 为储罐注册 `Capabilities.FluidHandler.BLOCK`，返回 `getFluidCapability()`
- 为保险库注册 `Capabilities.ItemHandler.BLOCK`，返回 `getItemCapability()`
- 为智能溜槽注册 `Capabilities.ItemHandler.BLOCK`，返回上下行两个真实缓存；不暴露方向样本
- 为弹射置物台注册 `Capabilities.ItemHandler.BLOCK`，返回原生 `DepotBehaviour.itemHandler`

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

强力机械泵使用普通 `BlockItem`，不再通过 `appendHoverText` 默认展开介绍。

客户端 `BoosterTooltips.register()` 使用 Create 6.0.10 的 `TooltipModifier.REGISTRY` 与 `ItemDescription.Modifier`。下界合金鼓风机、弹射置物台和伸缩机械手保留原生 Ponder 入口；其余十六种物品（含旧过滤器）提供中英文 `<物品 description id>.tooltip.summary`，由 Create 生成 Shift 提示、折叠、换行及重点配色。燃烧室捕获用法使用原生 `.condition1` / `.behaviour1`。

储罐的当前配置容量倍率、燃烧室堆叠保存的数量与倍率，只在按住 Shift 时追加到原生概要后。相关代码仅在客户端加载；捕获、放置、燃烧以及工程师护目镜信息保持原有逻辑。过滤器的已配置规则预览沿用 Create `FilterItem` 的原生显示行为。

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

- 容量倍率由客户端原生 Shift 概要显示
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

## 下界合金动力臂实现

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

## 下界合金鼓风机与过滤器（0.3.6及以前，历史实现）

本节保留旧专用过滤器的数据与菜单交接说明。当前鼓风机已改为四个原版过滤器槽，见本文“鼓风机四个原版过滤器槽（0.3.7）”；加工和风速倍率未变。

主要文件：`content/kinetics/fan/FanProcessingMode`、`NetheriteEncasedFanBlock`、`NetheriteEncasedFanBlockEntity`、`NetheriteAirCurrent`、`content/logistics/filter/NetheriteFilterData`、`NetheriteFilterItem`。

### 加工选择与速度

四种方式依次为水洗、岩浆熔炼、灵魂火缠魂和火焰烟熏，对应 `AllFanProcessingTypes.SPLASHING` / `BLASTING` / `HAUNTING` / `SMOKING`。默认水洗，模式可以随时切换，风口前不需要实体介质。优先规则是：

```text
匹配过滤器中的物品种类或嵌套原版过滤规则 -> 该区域指定方式
未匹配或未装过滤器 -> 鼓风机所选统一方式
```

BE 替换 `airCurrent` 为 `NetheriteAirCurrent`。它复用 Create 的碰撞与距离计算，`getTypeAt` 在有效风距内返回所选方式，覆盖短小的 `tickAffectedHandlers` 为传送带/置物台中的每件物品分别选择方式。地面掉落物由 `NetheriteAirCurrentMixin` 在原版 `tickAffectedEntities` 中完成相同选择。

两条路径都调用实际的 `FanProcessing.applyProcessing`，保留配方、堆叠数量导致的耗时、产出和副产物处理。每 tick 最多推进配置次数（默认 4），转换或销毁后立即退出，避免同一轮重复处理产出。没有动力时沿用原版停止行为。普通 Create 风机不受这些修改影响。

风速倍率只修改新风机在气流实体运动计算中读取的 `IAirCurrentSource.getSpeed()`，默认乘 2；不覆盖 BE 的动能 `getSpeed()`，不改变网络转速、应力换算或原版最大风距。实际运动仍遵循 Create 的加速度限制。

### 过滤器数据与两个菜单

- 四个区域各有 9 格，共 36 条规则。`NetheriteFilterData` 保留 `CUSTOM_DATA.FanRules` 的物品 ID 字符串列表及原有区域索引；普通样本仍只按物品种类保存。原版 `FilterItem` 样本的完整组件另外保存在 `DataComponents.CONTAINER` 相同槽位，以保留原版清单及匹配选项。原有过滤器数据直接可读，其他 CUSTOM_DATA 字段保留。
- 鼠标设置及数字键设置共用 `assign` / `assignToGroup`：同种物品只保留一个区域规则，跨区重新分配会移动，满区拒绝且不删除旧规则。未匹配回退统一方式是用户明确要求。
- `NetheriteFilterMenu` 继承 Create `HeldItemGhostItemMenu`。主手持过滤器右键配置，玩家槽 0..35、样本槽 36..71，主手过滤器锁定。样本不能被拾取为真实物品。
- `NetheriteFilterScreen` 支持一次按下背包物品、拖到样本、松开：使用原版 PICKUP 消息设置样本，并把原堆叠放回源槽；已有光标物品不消耗。样本左键设置、右键或空手清除。
- 悬停物品按 1..4 使用原版 SWAP 点击消息表达分组，服务端从被指向的槽读取样本，不信任客户端提交的物品内容。其他热栏交换操作被过滤器菜单拦截。
- `NetheriteFanMenu` 提供一个真实过滤器槽，限制一个下界合金过滤器；用 `DataSlot` 同步模式、原版 `clickMenuButton` 消息选择 0..3。服务端核对玩家、距离和 BE 身份，过滤器存在时仍允许切换模式。
- `BoosterMenuTypes` 注册两个 MenuType，客户端在 `RegisterMenuScreensEvent` 注册 `NetheriteFilterScreen` 与 `NetheriteFanScreen`；没有新增自定义网络 payload。

鼓风机将模式名称保存在 NBT `Mode`，过滤器库存保存在 `Filter`；客户端同步使用同一读写路径。BE `destroy()` 在服务器侧取出并掉落过滤器，保留其规则。`NetheriteFilterItem` 在普通 Create 物流过滤场景中对全部区域规则取并集，嵌套原版过滤器也通过原生匹配器判断。

### 合成、外观与检查

一个下界合金板与一个 `create:encased_fan` / `create:filter` 分别无序合成一个升级品。风机复用原版几何和动态风叶，以原版下界合金纹理替换机壳；过滤器使用 JSON 原生模型构造下界合金框与四块颜色区分的面板，无额外位图依赖。

`filterRegressionTest` 复用现有轻量 Java 检查源集，在初始化的 Minecraft 物品环境中验证样本不消耗、规则优先/回退、跨区移动、满区保留、无效槽拒绝和 CUSTOM_DATA 往返。Minecraft 资源通过 `createMinecraftArtifacts.resourcesArtifact` 任务输出 Provider 加入 classpath，不硬编码本机缓存路径。

```text
.\gradlew.bat filterRegressionTest
```

该任务已接入 `check` / `build`。目前它检查物品规则，不等价于完整游戏内界面和气流加工验证。

## 下界合金鼓风机思索

客户端 setup 中调用 `PonderIndex.addPlugin(new BoosterPonderPlugin())`，使用 Ponder `1.0.82` 的 `PonderPlugin` / `PonderSceneRegistrationHelper`，不使用旧版 `PonderRegistry` API。当前为下界合金鼓风机、弹射置物台和伸缩机械手各注册两段用法动画，不覆盖 Create 的原版教程；0.3.7已移除旧专用过滤器的共用入口。

- `client/ponder/BoosterPonderPlugin.java`：注册三种物品的场景及共享字幕；鼓风机15条、弹射置物台13条、伸缩机械手10条。
- `client/ponder/NetheriteFanScenes.java`：接入动力、选择四种加工模式、放入物品、设置过滤器、规则回退与切换，仅说明用法。
- `assets/createnetherite/ponder/fan/tutorial.nbt`：两段共用的原生 gzip NBT 结构，共 56 个方块。舞台为 7×7，结构高度 3；风机 `(1,2,3)` 向东，正下方 `(1,1,3)` 是安山机壳，轴在其西侧，四个置物台位于 `(3..6,1,3)`。场景先展示安山机壳，再展示风机。风口高于置物台一格，避免挡风。

场景 ID 为 `netherite_fan_modes`、`netherite_fan_filter`。标题键为 `createnetherite.ponder.<scene_id>.header`，字幕键为 `createnetherite.ponder.shared.<key>`，全部提供中英文。复用同一个结构注册多个 storyboard 受到当前 Ponder 注册表支持。

模式演示使用确定性配方：红羊毛水洗为白羊毛、粗铁熔炼为铁锭、沙子缠魂为灵魂沙、生牛肉烟熏为牛排。四个原版过滤表分别记录红羊毛、粗铁、沙子与土豆，牛肉使用统一烟熏模式；统一模式切换为水洗后，未列出的蓝羊毛洗白，粗铁仍按专属熔炼规则加工。

已移除速度对比场景及其注册、字幕和标题，思索不再介绍倍率或配置。与原生 Ponder 场景一致，加工结果由 storyboard 编排；该动画不充当真实世界加工回归测试。未新增或修改任何贴图，也不改变机器加工逻辑。

开发与验证遵守项目 `AGENTS.md`：不得使用 RTX 5060 独立显卡。未确认其他图形设备或软件渲染环境前，不启动游戏客户端；本次修订采用无图形界面的构建和资源检查，不能沿用初版的播放记录声称当前修订版已实测播放。

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


## 下界合金无线红石终端与无限管理器（0.3.1）

- 方块/物品：`netherite_wireless_terminal`；管理器物品：`netherite_infinite_manager`。无频率、无距离或维度限制，不接入 Create 的频率网络。
- `WirelessTerminalData` 存储于主世界 `data/createnetherite_wireless_terminals.dat`。每个终端以 UUID 标识，保存放置者 UUID/玩家名、用户名称、维度、坐标、公开状态及激活期限。查询记录不读取目标 chunk。
- 放置时生成身份并默认私有；名称格式通过双语 `wireless.createnetherite.owned_name` 显示，默认名称也按客户端语言显示。拆除注销；重新放置生成新身份。BE 保存登记备份，重载时以全局记录中的设置为准。
- `WirelessManagerMenu` 每页发送五个可见终端，记录跨页选择。列表仅含自己的和公开的终端。所有请求先验证当前菜单与持有管理器/本地终端交互范围，改名/公开再核对所有者 UUID，激活逐项复查当前可见权限，客户端不能凭空指定其他人的私有终端。
- 玩家右键自己的终端进入同一菜单的单终端配置视图；管理器也允许远程修改自己的终端。名字最多 48 个字符，过滤控制字符。公开状态只授权查看和发送信号，不授权改名或再次公开。
- `WirelessTerminalService.activate` 通过注册的 NeoForge `TicketController` 申请目标 chunk 的 ticking 票据，加载后校验实际 BE 身份，再输出强度 15 的红石信号，并向脚下的实体支撑方块提供强信号。开关和拆除均通知支撑方块的邻居。信号持续 20 tick，加载票据持续 300 tick；重复发送从当前主世界游戏时间重新计时。批量请求在同一服务器操作中处理，期限一致。
- 服务器 `Post` tick 负责 20 tick 归零、300 tick 释放。票据以终端 UUID 独立持有，同 chunk 的多个终端互不提前取消；释放只撤销本控制器自己的票据，不撤销其他来源。暂停服务器会暂停计时。
- 重启时控制器回调先清除本功能磁盘中的旧票据；所有维度完成初始化后，首个服务器 tick 只恢复全局记录中尚未到期的租期。已过期信号在 BE 载入时归零，防止保存的通电状态滞留。
- 客户端使用 `WirelessActionPayload` / `WirelessPagePayload`，均由服务端/客户端主线程处理；公共网络类不依赖图形客户端类。分页保持数据包大小固定，无需把所有私有记录发送到客户端再过滤。
- 模型继承锁定版 Create 的 `redstone_link/receiver` 与 `linked_controller/item`，终端通电/断电切换对应贴图。新增 PNG 由指定贴图对话提供；本功能沿用全部下界合金物品的防火、防爆及原生 Shift 概要。

验证入口：`wirelessRegressionTest` 已接入 `check/build`；独立 `runWirelessGameTestServer` 使用 `src/wirelessGameTest` 与专用 `run/wireless-gametest`，测试代码和结构不进入发布 jar。只运行无图形服务端，不启动游戏客户端或使用 RTX 5060。

构建配置参照 [ModDevGradle 官方说明](https://github.com/neoforged/ModDevGradle#runs)；区块票据与包处理按当前锁定的 NeoForge 21.1.219 源码核对。


2026-11-02 无线功能验证：新增 33 项数据回归先失败再转绿；一次独立审查发现并修正命名输入快捷键及支撑方块强信号问题。实际无图形 GameTest 服务端通过了未加载末地目标激活、私有请求不加载、1 秒输出、15 秒释放、同区块独立续期、再次卸载、真实支撑方块灯电路及拆除不影响其他加载来源。灯的计划刻按原版异步实体载入规则等待执行，不改变终端电平期限。测试服务端已退出，未运行客户端或使用显卡；界面操作尚未实测。

最终 0.3.1 完整离线构建通过：原有 172 项与新增 33 项，共 205 项回归通过。84 个生产 Java 源文件、111 个资源 JSON，双语键及模型引用检查通过；最终 jar 的版本元数据、全部生产资源字节、无线类及测试代码/结构排除检查通过。既有美术未修改，四张新增 PNG 对应贴图提交 `7326d31`。

## 冲压机与搅拌机黄铜边框（0.3.2）

2026-11-02 按用户要求，将下界合金冲压机、动力搅拌机的外框改用锁定版 Create `6.0.10-280` 的 `brass_casing.png` 两像素边框。方块和物品模型同步引用，继承的几何与 UV 不变；主体、木质面板、冲压头、搅拌头、传动轴与齿轮均保留。

- `textures/block/brass_framed_netherite.png`：冲压机外壳及两台机器顶盖共用，112 个边框像素直接复用原版黄铜机壳，内侧 144 个 RGBA 像素保持原下界合金方块材质。
- `textures/block/netherite_mechanical_press/side.png`：适配原版侧面 14 像素高的 UV，104 个边框像素使用黄铜，其他 152 个像素保留。
- `textures/block/netherite_mechanical_mixer/base_side.png`：仅替换 104 个边框像素；152 个非边框 RGBA 像素、透明度和木质面板完全保留。其他 36 张既有 PNG 字节未变。

无图形离线 `jar` 打包通过，111 个资源 JSON 解析及四个模型的父模型、纹理引用检查通过。核对发布 jar 的模组版本为 `0.3.2`，151 个生产资源文件与源码逐字节一致。本次仅修改资源与版本文档，未重新运行功能回归，未启动客户端或使用 RTX 5060。


## 无线终端安装面与方向修复（0.3.3）

`WirelessTerminalBlock` 现在以 `FACE`（`floor / wall / ceiling`）记录安装面，保留原有的水平 `FACING`。方块物品实际点击的面决定安装方式；墙面的朝向为该面的法线，平放与倒挂时按玩家水平朝向的反方向设置，模型提供四种旋转。旧存档缺少 `face` 时使用默认 `floor`，原有水平朝向和终端身份不变。

方块状态覆盖 3 种安装方式、4 个水平方向和两种通电状态，共 24 个组合。贴墙模型继承锁定版 Create 的 `receiver_vertical` / `receiver_vertical_powered`，旋转参数与原版贴墙终端相同；不修改贴图。碰撞与选取形状复用 `AllShapes.REDSTONE_BRIDGE` 对应安装面的形状。结构旋转、镜像同步变换水平朝向。

强红石输出、通断电通知及拆除通知均定位到实际贴附的支撑方块，避免墙上终端仍向脚下输出。1 秒脉冲、15 秒加载、管理器权限和登记规则不变。

新增 `WirelessTerminalPlacementGameTest`：通过真实方块物品点击墙面先复现平放形状；然后覆盖四面墙、四向平放、四向倒挂、通断电保留朝向与身份、状态保存重载、旧格式存档默认平放，以及贴墙支撑方块连接真实红石灯的电路。只运行无图形服务端，未启动客户端或使用 RTX 5060。

2026-11-02 修复验证：实际方块物品贴墙测试先在形状断言失败，修正后两项无图形 GameTest 均通过，覆盖 12 种安装/朝向组合、旧存档和墙面灯电路；完整构建的 205 项回归通过。113 个 JSON 解析通过，24 个模型状态完整，贴墙父模型及旋转与锁定 Create 一致。审查另修正了贴墙模型的 `redstone_bridge_side` 材质覆盖字段，随后重新打包并逐字节核对 0.3.3 jar 全部生产资源；未改动任何 PNG，测试类未打包。未启动客户端，模型未进行实际图形渲染检查。


## 原版过滤器嵌入加工分组（0.3.4）

- 下界合金过滤器分组可放入配置好的 Create 原版过滤器。普通列表、黑名单、匹配数据选项、原版嵌套列表及属性过滤器均保留组件，并使用原版 `FilterItemStack` 判断；鼓风机传入实际世界对象，未匹配物品仍走当前统一模式。
- `FanRules` 继续记录 36 个物品 ID，原版过滤器完整快照额外使用原版 `CONTAINER` 组件，以相同槽号关联。只复制一件作为样本，不消耗源物品。`getComponentType()` 保持 `CUSTOM_DATA`，原版过滤器复制合成可沿用既有检测并一并复制嵌套组件。清空移除 `FanRules` 和本功能的 `CONTAINER`，保留其他数据。
- 普通物品继续按种类去重；原版过滤器按物品及组件去重。不同配置可以分处不同分组，移动相同配置只移走同一条规则。鼠标、1～4 分配和 Shift 快捷放入使用相同判定。
- 重叠规则沿用固定优先级：水洗、熔炼、缠魂、烟熏，同组按槽位，取第一项匹配。过滤器配置是放入时的快照，外部修改原版过滤器后需重新放入。
- `NetheriteFilterItem` 的物流过滤包装使用同一匹配逻辑。原版匹配器会裁剪临时过滤堆栈的部分组件，因此调用时传副本，避免改变保存的快照。
- 新增 `NetheriteNestedFilterGameTest`，复用现有无图形 `runWirelessGameTestServer`：真实原版过滤器、物品/BE 序列化、复制合成、两种原料的真实配方输出、三种菜单输入方式、不同清单去重、黑白名单/数据匹配/原版嵌套/属性规则及清空。测试先复现配置丢失和菜单错误，再在实现后通过；不启动客户端或使用 RTX 5060。

2026-11-02 嵌套过滤验证：新增两项 GameTest 先在“原版清单保留”和“不同配置分组”断言失败，实现后连同原有两项共四项全部通过。验证使用实际 Create 过滤器、风机解析、菜单服务端操作和原版加工 API，并核对粗铁/粗金的真实配方产物；未进行客户端鼠标或真实气流的图形实测。完整离线构建的 205 项回归通过，113 个资源 JSON、双语键及最终 0.3.4 元数据、全部生产资源字节和测试代码排除检查通过。一次独立审查未发现问题，未修改贴图、启动客户端或使用 RTX 5060。

## 独立 GUI 贴图（0.3.5）

`NetheriteFilterScreen`、`NetheriteFanScreen` 和 `WirelessManagerScreen` 使用独立背景 PNG，取代代码中的填色矩形。资源位于源码 `src/main/resources/assets/createnetherite/textures/gui/`，发布 jar 中路径为 `assets/createnetherite/textures/gui/`。资源包可按相同路径覆盖；保持下表尺寸与布局，文字、物品图标和交互区域仍由游戏绘制。

| 文件 | PNG 尺寸 | 内容 |
| --- | --- | --- |
| `netherite_filter.png` | 288×236 | 标题带、四个加工分组、样本槽和玩家背包槽 |
| `netherite_fan.png` | 288×224 | 标题带、过滤器槽与玩家背包槽 |
| `netherite_wireless_manager.png` | 316×236 | 管理器背景和标题带，按钮、输入框与列表仍用原生控件 |
| `netherite_fan_buttons.png` | 248×114 | 四列、三行的加工模式按钮图集，每格 62×38 |

按钮图集列顺序为水洗、熔炼、缠魂、烟熏；行顺序为普通、悬停或键盘聚焦、选中。已选中按钮保持选中行，即使鼠标位于其上。代码按 `u = mode.ordinal() * 62`、`v = state * 38` 取图；文字和加工图标另行覆盖。

所有 `GuiGraphics.blit` 调用均显式传入真实 PNG 宽高，避免原生简写重载默认 256×256 导致超宽背景采样异常。背景按 GUI 坐标 1:1 绘制，整体缩放沿用游戏 GUI 缩放设置。四张图片由指定美术对话制作，本次接入不改动已有美术。

关键槽位的物品区域左上角如下，槽框围绕该 16×16 区域：

- 过滤表样本：`(18 + 66*g + 18*c, 56 + 18*r)`，四组 `g=0..3`，每组三行三列；玩家背包 `(64 + 18*c, 153 + 18*r)`，快捷栏 `y=211`。
- 鼓风机过滤器：`(24, 80)`；玩家背包 `(64 + 18*c, 138 + 18*r)`，快捷栏 `y=196`。四个模式按钮位置为 `(14 + 66*g, 28)`。
- 管理器布局保持原样：五条终端记录、跨页选择、改名与公开、批量激活。没有改变菜单数据或服务端权限处理。

2026-11-03 GUI 接入验证：完整无图形离线构建通过，现有 205 项回归全部通过。贴图到位后再次运行 `jar` 成功；四张 PNG 尺寸及不透明度、12 个按钮图集区域与三态区分核对通过，109 个槽框与菜单坐标逐像素对齐，39 张已有 PNG 与基线字节一致。最终 0.3.5 jar 中 157 个生产资源与源码逐字节一致，界面类和四张 PNG 齐全，测试类与测试结构未打入发布包。一次独立审查未发现问题。未启动客户端或使用 RTX 5060；客户端实际显示、资源重载和鼠标操作尚未实测。


## 下界合金智能溜槽（0.3.6）

- 方块、物品、BE 与菜单 ID 均为 `netherite_smart_chute`。`NetheriteSmartChuteBlock` 继承原版 `SmartChuteBlock`，保留 `POWERED` 及红石计划刻。原版 `ChuteShapes` 仅按注册 ID 特判智能溜槽，新方块覆盖形状与碰撞为相同的整方块，避免读取不存在的斜溜槽属性。
- `NetheriteSmartChuteBlockEntity` 继承原版智能溜槽，复用机身 `FilteringBehaviour`、原版过滤项实际安装/返还、黑白名单、属性/组件匹配与数量设置。方向样本是另行复制的两个 count=1 堆叠，不消耗原件，使用 `FilterItemStack.of(sample.copy()).test(level, target)`，不改变原过滤表的组件。
- `NetheriteSmartChuteMixin` 仅对新 BE 生效：在 `SmartBlockEntity.tick()` 完成行为 tick 后改用双向输送，原版溜槽完整执行原实现。上/下行各一个真实堆叠缓存，入站后等待 5 tick 输出，堵塞保持原件，部分插入仅保留真实余量。原版 `item` 字段不存放这两个缓存，原生机身 renderer 继续呈现过滤槽，护目镜分别显示两个缓存数量。
- 上行样本优先，其次下行样本，最后使用默认方向（初始下行，可选上行）。每段独立配置，进入缓存时确定方向；规则修改只影响新进入物品。相邻溜槽依赖自身主动输出，输入抽取跳过全部 `ChuteBlockEntity`，防止反向抢取或绕过原版红石暂停。新溜槽之间两个通道可以相向交换；原版段仍需其自身的气流条件才能上行。
- 相邻容器使用原生物品能力与 `ItemHelper.extract`，按原生 EXACTLY/UPTO 数量模式抽取；支持原生直接物品输入、相邻掉落物及下方传送物品行为。红石暂停时停止内部输入、运动与输出；外部主动取物能力沿用原版库存语义。
- NBT 的 `DirectionFilters` 保存两个样本，`DefaultDirection` 保存回退方向，`Transport` 与 `TransportTicks` 保存两缓存和剩余等待。保存重载保留原生机身过滤与数量。Shift 空手右键取出两个真实缓存；拆除先掉落真实缓存，再由原生行为返还实际安装的机身过滤器。方向样本始终不掉落。
- `NetheriteSmartChuteMenu` 将玩家背包置于前 36 槽，后两槽为只读样本，复制点击/1、2键输入由服务端从真实来源取样；样本不能克隆、投掷或拖出。服务器验证当前玩家、BE 身份与 8 格距离，模式按钮同样校验。未增加自定义网络包。
- `NetheriteSmartChuteScreen` 复用已有样本拖拽方式；未匹配方向使用原生按钮，当前默认方向按钮显示为禁用态。独立 GUI `textures/gui/netherite_smart_chute.png` 为 230×206，样本内区左上为 `(48,43)`、`(166,43)`，玩家背包从 `(35,123)` 开始，快捷栏 `y=181`，共 38 个槽框。
- 模型继承锁定 Create 的 `smart_chute/block`，覆盖键 `7`、`9` 及 particle；通电模型覆盖关闭的漏斗内部与 side_powered。三张机身 PNG 与一张 GUI 均由指定美术对话提供，43 张既有 PNG 字节未变。下界合金板加原版智能溜槽无序升级；防火、防爆、挖掘标签及原生 Shift 概要齐全。

验证入口仍为 `runWirelessGameTestServer` 的独立无图形源集，测试类和结构不进入发布 jar。`NetheriteSmartChuteGameTest` 覆盖相向柱、原生准入、真实红石暂停、双缓存保存、样本菜单、原版连接、部分余量、拆除计数、黑名单和 EXACTLY/UPTO 数量。独立审查发现原版缓存会被主动反向抽取，新增用例先真实复现，再通过跳过原版溜槽主动抽取修正。禁止使用 RTX 5060，不启动客户端；实际显示和客户端鼠标操作需单独实测。

2026-11-04 最终验证：完整离线构建、205 项原有回归及全部 9 项无图形 GameTest 通过，其中新增 5 项。原版反向抢取用例先失败，修正后连同原有无线、过滤与放置用例一起通过。119 个资源 JSON、中英文各 82 个键、GUI 的 38 个槽框及原生 alpha 检查通过；43 张既有 PNG 和既有翻译不变。核对 0.3.6 jar 中 167 个生产资源逐字节一致，5 个新生产类与 Mixin 齐全，模型父级和实际纹理变量均解析，测试类/结构未打包。修正后的独立审查无未解决问题，全程未启动客户端或使用 RTX 5060。


## 鼓风机四个原版过滤器槽（0.3.7）

本节覆盖上文0.3.4及更早的鼓风机过滤器用法；原有验证记录保留其历史版本。

- `NetheriteEncasedFanBlockEntity` 保存四个真实过滤器槽，顺序为水洗、熔炼、缠魂、烟熏，每槽最多一件。限定 `FilterItem` 且注册命名空间为 `create`，拒绝普通物品、专用下界合金过滤器及其他模组过滤器。Create 普通、属性和包裹过滤器均沿用其原生匹配逻辑。
- `resolveProcessingMode` 按四槽顺序跳过空槽，使用 `FilterItemStack.of(filter.copy()).test(level,target)`，第一个匹配决定加工方式；全部不匹配时使用手动 `processingMode`。黑白名单、组件、原版嵌套及属性条件不复制到其他容器，不修改原过滤器。
- `NetheriteFanMenu` 前四槽为真实过滤器，玩家主背包从槽4开始、快捷栏从槽31开始，总40槽。正常放取、原生数字键交换与Shift转移均按真实物品计数，槽上限为1；快捷放入按第一个空加工槽顺序。所有点击、快捷移动和模式按钮均验证玩家、BE身份与8格距离。
- 模式按钮保持原来的四列位置与按钮图集，只改变未匹配物品的回退方式。四个过滤器内区左上为 `(37+66*i,80)`，与上方按钮逐列对齐；行说明在y70，回退说明在y108，背包位置不变。空槽提示说明固定加工方式，按钮提示说明回退用途。
- 新NBT用 `Filters` 保存四槽；旧 `Filter` 单槽中的原物及完整组件保存在 `LegacyFilter`。首次有效打开新菜单，将原物清出兼容缓存后交还玩家，背包满按原版回包行为掉落；重复打开不重复返还，未打开直接拆除则掉落旧原物。不会复制或生成四张原版过滤表，旧规则不会自动装入新槽。旧机四槽初始为空，按已有选定模式加工，用户需重新安装原版过滤表。
- 旧专用过滤器保留ID、数据和查看菜单以避免旧存档丢物，移出创造栏、删除合成配方与Ponder入口，并使用原生Shift兼容概要。机内四个真实原版过滤器在拆除时分别返还。
- 两段鼓风机思索改为原版过滤表和四槽操作，仍只讲用法，原结构和风机下方安山机壳不变。四槽示例分别过滤红羊毛、粗铁、沙子与土豆；未列入清单的生牛肉展示烟熏回退，随后蓝羊毛展示切换水洗回退。
- 仅修改已有 `textures/gui/netherite_fan.png` 的过滤器行 `x=[14,274),y=[68,102)`，保持288×224及不透明，按钮图集、玩家槽位和其他贴图不变；图片由指定美术对话提供。版本0.3.7，不修改加工/风速倍率。

2026-11-04 四槽验证：两项新增用例先在槽位数量及菜单结构断言失败，实现后完整离线构建、205项原有回归与全部12项无图形GameTest通过（新增3项）。验证四种匹配、回退、重叠优先、原版属性与组件、真实物品转移/满槽/拆除，以及旧过滤器完整配置持久、重复打开仅退还一次、未打开拆除返还原物。独立审查未发现问题。118个JSON、中英文各86个键及15条思索字幕齐全，两段场景和原结构核对通过。四槽GUI坐标匹配，矩形外55672个RGBA像素与其他46张PNG字节不变。最终0.3.7 jar 的166个生产资源与源码逐字节一致，旧配方不存在、兼容类保留、测试代码及结构未打包。未启动客户端或使用RTX5060，实际GUI与鼠标操作、修订思索播放未实测。

## 下界合金弹射置物台（0.3.8）

2026-11-04 新增 `createnetherite:netherite_weighted_ejector`，保持平放，目标支持完整三维相对偏移、斜向及纯竖直方向。

- `NetheriteEjectorBlockEntity` 继承原生 `EjectorBlockEntity`，复用库存、数量设置、红石暂停、碰撞扫描和虚拟飞行队列。`NetheriteEntityLauncher` 复用 Create 的动力学，将水平标量映射到完整XZ向量，并使虚拟物品轨迹准确结束于目标方块中心。实体初速按目标向量旋转，斜向鞘翅部署也按目标向量设置朝向，纯竖直保留原生方块朝向回退。
- 射程跟随 `AllConfigs.server().kinetics.maxEjectorDistance` 的2倍，默认64格。水平按欧氏半径、竖直按绝对高度差分别校验；禁止零偏移和世界边界外目标。存档 `TargetOffset` 保存3个整数，避免重载被原版32格截断；扳手和结构变换同步变换相对目标。
- `getWindUpSpeed()` 返回原生值的8倍。真实动力网络转速和应力冲击沿用原版，未供能不蓄力。同RPM、同距离的蓄力阶段约缩短至1/8，整tick和原版弹射动作仍存在。
- `NetheriteEjectorItem` 的 `CUSTOM_DATA.NetheriteEjectorTarget` 保存维度和绝对坐标。手持Shift右键选点，重复选同一点清除；正常放置时在物品消耗前应用目标，包括手中最后一件。右键已有升级置物台可重设而不消耗物品。原版库存交互及数量调节继续使用原生机制。
- 服务端验证当前维度、射程、世界范围、建造权限、非旁观者、8格编辑距离及 `mayInteract`；拒绝无法表达三维目标的原版两整数设置接口。飞行中拒绝改向和扳手旋转，同目标设置幂等。空闲重设清除 `earlyTarget`、时间与扫描冷却，防止使用旧碰撞库存而停机或误投。
- 拆除时在每件在途物品的实际位置构建掉落实体，保留数量、组件和运动；修正原生实现调用 `setPosRaw` 后碰撞箱仍留在原点的问题。此调整仅覆盖新置物台。
- `NetheriteEjectorTargetHandler` 在客户端后置tick显示选点/扳手轨迹，使用相同三维轨迹计算；仅对新机器抑制原生基数方向预览。静态/物品模型继承原生几何，普通渲染和Flywheel动态上盖通过各自Mixin使用专用材质，原生数量框方向也限定修正新机器。
- 下界合金板加原版弹射置物台无序升级，配方、掉落、创造栏、物品能力、镐/钻石工具标签、防火、防爆与中英文原生Shift概要均已接入。三张新增PNG由指定美术对话提供：casing为32×32，top/inner为16×16，尺寸与逐像素透明度保持原生，木头和轴引用保留原版；46张既有PNG字节未变。

新增5项 `NetheriteEjectorGameTest`：实际斜上方输送及飞行途中保存重载；真实选点/最后一件放置/重设与两倍射程边界；两个创造马达驱动下的64RPM蓄力、原机对照、红石暂停及活体斜向/纯上弹射和鞘翅朝向；障碍物及拆除时真实带组件物品计数；旧碰撞库存装满后重设到新目标的实际输送。测试先复现未注册、在途改向、旧缓存、拆除碰撞箱及鞘翅朝向问题，修正后完整离线 `build runWirelessGameTestServer` 通过：205项原有回归和全部17项GameTest通过。独立源码审查发现的两项问题已修正并复核。未启动客户端或使用RTX5060，客户端Mixin实际加载、预览和两种上盖渲染尚未进行图形实测。

发布核对：125个资源JSON解析通过，中英文各93个语言键，旧86个键的值与原始格式保留。8种平放朝向/含水状态完整，三个模型继承原生几何且全部实际使用的纹理引用可解析。最终0.3.8 jar 的176个生产资源及全部生产类与源码/编译结果逐字节一致，模组元数据版本为0.3.8；测试代码和结构未打包，旧专用过滤器配方仍不存在。jar SHA-256：`833667574298a97283f0ccb54ab4af5095e2a3dd9c023d88d658bd0e5a5a0c71`。

## 下界合金伸缩机械手（0.3.9）

2026-11-04 新增物品 `createnetherite:netherite_extendo_grip`，由下界合金板和原版伸缩机械手无序升级，沿用防火、防爆和原生Shift概要；不新增方块或永久动力源。

- `NetheriteExtendoGripItem` 继承 `ExtendoGripItem`，保留原生200耐久、背罐空气、耐久条和客户端渲染入口。限定 `NetheriteExtendoGripMixin` 让原生单/双持检测、资源扣除和远距实体交互识别新物品；原生3/5格基础上，每只升级品额外增加3格方块触及距离，单持6、混合8、升级双持11。额外属性使用独立ID和临时修饰符，移除工具后清除；原版单持/双持范围不变。
- `NetheriteGripTankMixin` 仅接入生存玩家主手空桶、副手新工具的储罐交互，原版和下界合金储罐均沿用真实流体能力。使用NeoForge `FluidUtil.tryFillContainerAndStow` 抽取1000mB、替换单桶或保存堆叠桶产物；背包满时沿用原生返还/掉落规则。服务器执行实际转移，客户端仅模拟；成功才扣一次工具资源。检查非旁观者、建造权限、实际触及和 `mayInteract`；没有工具、液体不足或权限不允许时不扣资源。此入口只开放空桶取液。
- `HandheldKineticPower` 从主手 `onItemUseFirst` 开始持续使用，读取玩家正在操作的目标，逐tick校验工具、维度、目标BE身份、距离、视线与权限。开始时消耗一次资源，此后每20tick再消耗一次；优先 `BacktankUtil.canAbsorbDamage`，没有空气时扣1点耐久，沿用Create动作消耗配置及创造模式规则。取液和普通远距操作使用相同资源体系。
- 手动供能为16RPM，Shift开始可用-16RPM。容量为 `BlockStressValues.getImpact(POWERFUL_MECHANICAL_PUMP)` 乘绝对RPM，默认16×16=256SU，恰好带动一个强力泵。超过容量的工作方块、发电机和已有原生转速的机器拒绝启动；其他玩家不能抢用正在手动驱动的同一方块。
- 当前选择只驱动点击方块，未把手持工具接成轴网络。`HandheldKineticPowerMixin` 仅在活动目标且原生转速为0时提供临时 `getSpeed()`，工作逻辑和渲染能读取；`getTheoreticalSpeed()`、网络和永久速度字段保持原生。松开立即停止；移开视线、换工具、失去玩家或到期时也停止，2tick租期用于中断回收。临时RPM只写客户端同步包，不写世界存档，重载后没有免费动力。
- `NetheriteGripRenderHandlerMixin` 让原生主副手伸缩动作识别新工具；`NetheriteGripItemRendererMixin` 为四种连杆替换对应PartialModel。物品主体和四种连杆模型继承Create6.0.10原生几何并覆盖纹理键4，木质齿轮、通用端盖和共享手掌保留原版。
- 新增 `textures/item/netherite_extendo_grip.png` 由指定美术对话提供，16×16，UV与所有alpha值保留；68个结构金属像素改为本项目下界合金色调，其余188个RGBA像素保留。49张既有生产PNG字节未变。

验证：新增5项 `NetheriteExtendoGripGameTest`，先复现物品未注册，再验证真实单/混合/双持范围与移除恢复、原生破坏消耗与空气优先；生存空桶从原版水罐取液；下界合金储罐岩浆、堆叠桶、不足1000mB与权限失败不消耗、背罐空气取液；手动强力泵实际输水、持续耐久、松开停机与保存重载无动力；多人抢用拒绝、空气供能、切换/移开停止、反向RPM、真实齿轮和创造马达保持64RPM及配置容量不足拒绝。完整离线构建的205项原有回归及全部22项无图形GameTest通过。模拟玩家测试同步头部视线、马达测试采用真实小齿轮连接，避免将无效测试布置误判为生产缺陷。独立只读审查未发现可操作问题。

发布核对：132个资源JSON，中英文各96个语言键，旧93个值和格式保留。新增物品及4个动态模型均继承原生几何、实际纹理键为4。最终0.3.9 jar 的184个生产资源和全部生产类与源码/编译输出逐字节一致，元数据版本0.3.9，测试代码/结构未打包；旧过滤器配方仍不存在。SHA-256：`fb1421c5ac64e75a05ecd87826e75bed822e45b2432f168c8155b201d1904ec1`。未启动客户端或使用RTX5060，伸缩动画、新材质与长按交互的客户端显示仍待图形实测。


## 下界合金机械手 / Deployer（0.3.10）

2026-11-04 新增 `createnetherite:netherite_deployer`，对应Create官方名称“机械手”。原有搬运方块 `netherite_mechanical_arm` 的中文名称及概要更正为“下界合金动力臂”；原注册ID、倍率、配方和存档不变。

- `NetheriteDeployerBlock` / `NetheriteDeployerBlockEntity` 继承原生实现，保留动作周期、过滤、红石、使用/攻击模式、FakePlayer库存和保存。应力supplier读取原生deployer impact，倍率1，默认4SU/RPM。覆盖移动向量，避免原生按 `AllBlocks.DEPLOYER` 的注册ID检查返回零向量。
- `NetheriteDeployerBatchMixin` 仅拦截新BE的 `BeltDeployerCallbacks.activate`。外层批量记录本次输入堆叠，内部防递归标记允许逐件调用原生activate；每件重新匹配同一recipe ID，材料耗尽、工具损坏或配方不匹配立即停止。批量最多处理输入本身的堆叠上限，产物不进入本轮输入。
- 临时 `TransportedItemStackHandlerBehaviour` 捕获原生逐件产物和余料，按物品/组件及堆叠上限合并产物，最后一次提交原始传送带或置物台handler。原生无产物随机结果的 `convertTo(left)` 仅作为未加工余料，不误当成产物。序列组装逐件保留其原生组件，只推进当前一步；不堆叠过渡物保留各自堆叠上限。
- 材料消耗、工具耐久、keepHeldItem规则、随机产物、容器返还和进度均调用原生逐件路径，不按整堆扣一次材料。置物台缓存满时沿用原生掉落剩余产物的处理。普通世界放置、攻击、挖掘和移动结构行为仍按原生单次动作，不增加交互次数。
- `NetheriteDeployerInventoryAccess` 复用原生惰性 `initHandler` 和 `DeployerItemHandler`，对外注册真实物品能力。专属 `DeployerType` 动力臂交互点只匹配新方块，沿用原生给料/抽取；同时注册 `DeployerMovementBehaviour`、`DeployerMovingInteraction` 和 `create:contraption_controlled` 物品标签，保留运动结构上的使用/攻击切换、玩家交换手持物和控制器识别。
- 下界合金板加原版deployer无序升级；注册掉落、创造栏、镐/钻石工具标签、防火、防爆及中英文原生Shift概要。物品使用原生 `AssemblyOperatorBlockItem`。
- 横向、竖向及物品模型继承原生几何，12种朝向/轴组合沿用原生状态。机身的 `andesite_casing_piston`（方块键5、物品键10）、`piston_bottom`（6）、`piston_inner`（7）、`gearbox_top`、`gearbox`、`andesite_casing_short` 使用指定美术对话提供的六张16×16新增贴图；888个灰色金属像素变更，其他648个RGBA像素及全部alpha保留，51张既有仓库PNG字节不变。动态传动轴、压杆和共享黄铜手掌继续原版，复用 `DeployerRenderer` / `DeployerVisual`。

新增7项真实服务端GameTest：置物台32件整批；材料不足保留余料与原版单件对照；砂纸只剩2次耐久、keepHeld催化剂；4个玻璃瓶容器返回、确定性无产物结果；16件不堆叠序列过渡物分别推进step1并核对缓存及溢出掉落计数；真实动力臂交互点、模拟/实际给料、抽取及库存重载和移动注册；真实三格传送带32件加工。未注册基线先失败，实现后完整离线build与205项原有回归、全部29项GameTest通过。独立审查发现的移动交互和动力臂给料注册遗漏已补齐并复核。序列测试按真实不堆叠产物及原生溢出掉落核对，测试配方的无产物情况通过清空仅QA配方的结果列表确定性复现（Create不接受JSON chance=0）。

发布核对：140个生产资源JSON，中英文各98个语言键；除动力臂中文名称与概要两项外，旧语言值和格式保留。三个模型的原生parent/几何及全部实际使用纹理路径核对通过。最终0.3.10 jar 的198个生产资源与全部生产类均和源码/编译结果逐字节一致，元数据版本0.3.10，QA配方、测试类和结构未打包，旧专用过滤器配方仍不存在。SHA-256：`a0ec64a56a044fd6a44278d9d6880909661c82a325b271da662957015414d373`。未启动客户端或使用RTX5060，新增模型的实际渲染和移动结构内的玩家手动操作仍未进行图形实测。


## 下界合金下界合金（0.3.11）

2026-11-04 新增 `createnetherite:netherite_netherite`，名称、进度标题与概要按用户原文分别为“下界合金下界合金”“你的牙还好吗？”“你为什么要做这个？”。一个下界合金锭加一个下界合金板无序合成一个；沿用防火、防爆与原生Shift概要，加入创造栏。

- `NetheriteNetheriteItem` 是无食物组件的普通物品，主副手均可在满饥饿时开始使用，返回原生 `UseAnim.EAT`、32tick周期。`finishUsingItem` 原样返回堆叠，既不消耗数量/组件，也不添加饥饿、饱和、生命或经验奖励。
- `LivingEntityUseItemEvent.Tick` 监听器只在本物品剩余duration恰为1时设为33；原生随后递减为32，使原版食用动作保持循环，不用极长duration导致动画迟迟不开始，也不结束/重启使用。事件取消或非正duration的停止语义保留，其他食物不变。
- 服务端 `use` 在实际开始使用后记录玩家本次起点，`onUseTick` 仅对当前使用的ServerPlayer计算游戏时间差；严格大于200tick才授予 `are_your_teeth_ok` 的 `chew` criterion。松开清理起点，新的使用覆盖起点，多次短按不累计；授予后清理计时，动画仍持续。
- 真实进度位于 `data/createnetherite/advancement/are_your_teeth_ok.json`，父节点为 `minecraft:nether/root`，`frame=challenge` 对应原生紫色挑战显示，使用 `minecraft:impossible` criterion由服务端主动授予。标题/说明具备中英文，显示toast和原生聊天通告，无额外奖励。
- 指定美术对话新增16×16透明物品贴图，表现锭与薄板复合造型，模型继承 `minecraft:item/generated`；57张旧仓库PNG字节未变。

验证：未注册基线先失败。新增4项GameTest覆盖满饥饿开始/松开/完成路径不消耗，真实ServerPlayer使用循环跨越多个32tick周期，200tick尚未授予而201tick授予，260tick后仍持续且数量/组件/饥饿/饱和/生命不变，150tick松开后副手重启再独立等待201tick，真实配方、challenge类型及原生事件总线的1/0/负值边界和普通苹果不变。NeoForge明确拒绝给FakePlayer授予进度，因此进度验证改用真实ServerPlayer及不联网的测试连接；生产逻辑未为测试放开限制。完整构建的205项原有回归与全部33项服务端GameTest通过，独立只读审查未发现问题。

发布核对：143个生产JSON、中英文各102键，旧98键值和格式保持；用户指定的三段中文逐字一致。最终0.3.11 jar 的202个生产资源和全部生产类与源码/编译输出逐字节一致，模组元数据版本0.3.11，测试类/QA配方/结构未打包，旧专用过滤器配方仍不存在。SHA-256：`d095624fc3a9481924471a74b78b7b72fe3aca5f7bb8b7296f9f8861b676a13d`。未启动客户端或使用RTX5060，手部动作及紫色toast的实际图形显示仍待实测。

## 下界合金弹射置物台思索（0.3.12）

2026-11-04 为 `netherite_weighted_ejector` 新增原生W思索入口，两段动画仅介绍用法，不改变机器逻辑、倍率或贴图。该物品移出纯Shift概要注册清单，原有概要翻译保留兼容。

- `NetheriteEjectorScenes.launch` / `netherite_ejector_launch`：手持机器Shift右键目标、预览斜向高低轨迹、正常放置、连接动力轴、向蓄力完成的平台投入铜锭，由真实飞行队列送到目标置物台。
- `NetheriteEjectorScenes.controls` / `netherite_ejector_controls`：在途物品抵达后重新选点并右键现有机器改向；侧面数量面板设为4，先放2件等待，再补2件触发；从目标置物台取走第一批物品以腾出空间，再演示控制杆通电暂停满4件的平台，断电恢复弹射。
- `BoosterPonderPlugin` 为新物品注册两个 `ejector/tutorial` storyboard，新增13条共享字幕、两个标题，全部具备中英文。原鼓风机注册、场景、15条共享字幕及结构保留。
- `assets/createnetherite/ponder/ejector/tutorial.nbt` 为7×8×7、58方块的原生结构。源台 `(1,2,5)` 面向北，下方为安山机壳，西侧X轴轴杆；两个目标分别为 `(4,3,2)`、`(5,1,4)`，控制杆在 `(1,2,6)` 并有机壳支撑。结构预留完整轨迹上方空间，目标支柱不挡飞行路径。
- 场景通过真实 `setTargetOffset`、原生数量行为的 `setValue(4)` 和控制杆切换后的 `updateSignal()` 控制，使用 `createItemOnBeltLike` 投料并等待实际虚拟飞行，不直接给目标添加产物。轨迹预览采样同一个 `NetheriteEntityLauncher`。数量调整调用行为接口，避免对已放置动力方块重新载入整份NBT时触发Create的移动保护并清除动力信息。
- 锁定Create版本的可合并空置物台首次只接收1件：原生余量计算读取空堆叠的上限1。教程投料先放1件，再放剩余数量，避免Ponder忽略余量导致“2+2”实际只收到3件；不修改原生库存实现。

验证：新增1项 `NetheriteEjectorPonderGameTest`，读取实际生产结构，验证支撑和注册、真实选点/最后一件放置、斜上方输送、空闲改向不消耗物品、两条实际轨迹均位于结构范围内、64RPM持续供能、2件等待/4件触发、腾空目标、控制杆暂停与恢复后的再次实际到达。独立审查指出的空台首次批量投料问题已修正并复核；测试也发现并修正了整份NBT重载导致的停机和接收台未腾空。最终完整离线 `build runWirelessGameTestServer` 通过，205项原有回归及全部34项服务端GameTest通过。

发布核对：143个生产JSON、中英文各117键，旧102键值和原鼓风机场景/结构未改，58张既有仓库PNG字节不变。最终0.3.12 jar 的203个生产资源和138个生产类与源码资源/编译结果逐字节一致，元数据版本0.3.12，新增场景类及结构齐全，测试类/QA配方/结构未打包。SHA-256：`a07f49a4b96a4b7b0c06050644508e80125d780db8b4e281e2b31b143e5ad46b`。未启动客户端或使用RTX5060；上述服务端操作链验证与源码审查不代表已实测Ponder画面、镜头及字幕布局，实际播放仍待图形验证。


## 下界合金伸缩机械手思索（0.3.13）

2026-11-05 为 `netherite_extendo_grip` 新增原生W入口，移出纯Shift概要注册清单，原有概要翻译保留兼容。两段动画仅介绍用户指定的副手取液和主手供能用法，物品能力、倍率、Mixin及贴图不变。

- `NetheriteExtendoGripScenes.bucket` / `netherite_grip_bucket`：生存模式下副手持机械手、主手空桶右键流体储罐；真实储罐库存减少1000mB，显示水桶结果，并说明至少一桶存量和重复取液。取液过程沿用原生Ponder编排，未在虚拟世界伪造玩家交易。
- `NetheriteExtendoGripScenes.power` / `netherite_grip_power`：主手持机械手，选择未接外部动力且未超过容量的工作方块；持续瞄准并长按右键，强力机械泵输水，啮合的小齿轮保持静止。松开、移开视线或换工具停止供能。两段均说明已装备背罐的空气优先，不足时扣机械手耐久。
- 新增 `assets/createnetherite/ponder/extendo_grip/tutorial.nbt`，5×4×5、33方块。源储罐 `(1,2,2)` 含8000mB水，目标储罐 `(3,2,2)` 为空，二者隔着向东的强力机械泵 `(2,2,2)`；北侧小齿轮 `(2,2,1)` 为X轴，四个工作块下方均为安山机壳。储罐独立，不发生多方块合并。
- `BoosterPonderPlugin` 为该物品注册两个共用结构的storyboard，新增10条共享字幕及两个标题，中英文齐全。既有鼓风机和弹射置物台的四段思索及字幕保留。
- 供能动画仅对泵调用 `setKineticSpeed(..., HandheldKineticPower.RPM)`，读取实际16RPM常量，并通过 `propagatePipeChange` 更新压力，流体由Create原生虚拟流体网络输送；100tick后转速归零，泵两口压力在下一个虚拟tick归零。未直接设置产物储罐内容，也未向齿轮写入转速。该场景编排表示主手供能效果，不创建实际玩家、动力网络或额外供能规则。

验证：新增1项 `NetheriteExtendoGripPonderGameTest`，未提供生产结构时先按预期失败。加入结构后读取实际生产NBT，验证源罐8000mB与目标空罐相互独立、生存副手机械手与主手空桶取1000mB并消耗一次资源、主手切换后持续16RPM驱动实际机械泵输水、实际啮合的小齿轮始终不转、持续扣耐久以及松开立即归零。独立审查发现原北侧Z轴杆未与泵连接，已改为X轴小齿轮；新增真实啮合断言先在旧布置失败，修正后通过。供能演示缩短为100tick，避免默认8倍压力先抽空源罐而遮蔽停机效果。既有机械手GameTest继续覆盖背罐空气优先、容量/权限边界等行为。最终完整离线 `build runWirelessGameTestServer` 的205项原有回归和全部35项服务端GameTest通过。

发布核对：143个生产JSON、中英文各129键，旧117键值保留；既有鼓风机/弹射置物台场景和结构未改，58张既有仓库PNG字节不变。最终0.3.13 jar 的204个生产资源与139个生产类和源码资源/编译结果逐字节一致，模组元数据版本0.3.13，测试类/QA配方/结构未打包。SHA-256：`336a8a7153f14e0338a317f7c4bd4b2e3e755443886987f1a9a468c10dfd36ac`。未启动客户端或使用RTX5060；实际思索播放、镜头和字幕布局仍待图形验证，服务端真实操作链验证不等同于Ponder画面实测。
