# 简介

**[Create-Netherite-Upgrades](https://github.com/von-Neumann101/Create-Netherite-Upgrades)** 是一个面向 Minecraft 1.21.1 NeoForge 的 Create 附属模组。

当前分支：`1.21.1-NeoForge`

当前模组版本：`0.2.5`

# 维护状态

自 2026-10-29 起，主要维护 Minecraft `1.21.1` 的 `1.21.1-NeoForge` 分支。后续功能、问题修复和依赖更新以该分支为主。

Minecraft `1.20.1` Forge 的 `main` 分支暂停更新，保留现有代码和历史技术文档。两个版本目前存在功能差异，暂停期间不再同步新增内容。

技术文档：[NeoForge 1.21.1](docs/neoforge-1.21.1.md) · [Forge 1.20.1（历史参考）](docs/forge-1.20.1.md)

# 模组内容

当前模组新增多个 Create 兼容内容：

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

强力机械泵接入 Create 原版流体管网，默认基础应力消耗为 `16.0`、压力倍率为 `8.0`。压力倍率提高吞吐，不增加管道传播距离。

下界合金流体储罐默认拥有普通储罐 `16` 倍的单方块容量，并支持搭配下界合金蒸汽引擎构建锅炉。不足 `45` 个储罐方块时，每 `4` 个方块提供 `1` 级尺寸等级；达到 `3×3×5`（`45` 个方块）后，尺寸等级至少为 `18`，并随 `min(供水等级, 热量等级)` 继续上涨，不再限制实际锅炉等级。每级供水需求仍为 `12 mB/t`，供水等级没有 `18` 级上限。

单个下界合金蒸汽引擎的输出上限为 Create 原版引擎的 `256` 倍；按 Create 默认配置，满额输出为 `4,194,304 SU`，最高 `64 RPM`。实际输出仍受锅炉状态和连接的引擎数量影响。普通与下界合金锅炉分别识别各自的蒸汽引擎。

下界合金物品保险库拥有普通保险库 `16` 倍的槽位数，保留多方块和装配到运动结构上的存储行为。

下界合金升降机滑轮在原版移动速度计算中使用 `2` 倍转速，默认最大运输距离为 `768` 格，实际范围仍受世界最低建造高度限制。

下界合金动力冲压机支持批量冲压，并在工作盆中连续处理当前可匹配的配方；基础应力消耗沿用普通动力冲压机。

下界合金动力搅拌机采用与下界合金冲压机相同的批量加速方式：一次搅拌周期结束时，连续处理工作盆中当前仍可匹配的配方。搅拌动作和每周期等待时间沿用原版，批量处理的吞吐提升取决于盆内材料数量。应力消耗为普通搅拌机的 `1` 倍（默认基础值 `4 SU/RPM`）；配方、加热、超级加热、无序合成、酿造、过滤和工作盆输出规则沿用 Create。一个下界合金板加一个原版动力搅拌机，无序合成一个升级品。

下界合金机械手的动作进度和动作速度上限均为原版的 `3` 倍，同转速下应力消耗为原版的 `1.5` 倍（默认基础值为 `3 SU/RPM`）。工作半径为 Create 机械手范围配置的 `3` 倍，默认从 `5` 格扩大到 `15` 格；沿用原版球形距离判定，目标距离须小于该半径。选点、分配模式、红石控制与自动喂料沿用原版。一个下界合金板加一个原版机械手，无序合成一个升级品。

下界合金烈焰人燃烧室支持空、满两条升级路径：一个下界合金板加一个原版空燃烧室，合成空的下界合金燃烧室（`0` 只）；使用原版已装烈焰人的燃烧室升级，则保留 `1` 只。也可用两个板加三个对应原版燃烧室批量升级。手持空容器连续右键烈焰人，可以从 `0` 只依次装到 `4` 只。

空容器有独立名称和空笼模型，不消耗燃料、不提供热量，放置和拆下后仍保持为空。物品的数量与倍率可按住 `Shift` 查看；工程师护目镜会显示非空燃烧室的数量与倍率，放置后也会显示对应数量的烈焰人。拆除、扳手拾取、重新放置和存档均保留数量，旧燃烧室默认按 `1` 只处理。

| 烈焰人数量 | 锅炉热量倍率 | 燃料消耗倍率 |
| --- | --- | --- |
| 0（空） | 无热量 | 0× |
| 1 | 1× | 0.5× |
| 2 | 2× | 1× |
| 3 | 3× | 1.5× |
| 4 | 4× | 2× |

燃料种类、普通加热与超级加热状态沿用 Create。自动或手动喂料仍一次接收一个燃料，按上表倍率推进燃烧计时；例如装满 `4` 只后，同一个燃料的持续时间为原版的一半。超级加热同样按数量放大锅炉热量：满载普通加热提供 `4` 级，满载超级加热提供 `8` 级。无燃料时不产生主动热量。

下界合金板由下界合金锭经 Create 冲压制成，用作上述机械的升级材料。

# 下界合金鼓风机与过滤器

下界合金鼓风机可以内置选择水洗、岩浆熔炼、灵魂火缠魂或火焰烟熏，无须在风口前放置水、岩浆等介质。支持地面掉落物、传送带和置物台，沿用 Create 的配方与加工规则。默认加工进度为原版的 `4` 倍，气流计算风速为原版的 `2` 倍，风距和动能网络转速沿用原版规则。

使用方式：

1. 主手持下界合金过滤器并右键，打开四个加工区域，每区可放 `9` 种物品样本。
2. 从背包拖入样本格，或鼠标悬停物品按 `1`～`4` 分别分配水洗、熔炼、缠魂、烟熏。样本不消耗真实物品；右键或空手点击样本格清除。同种物品改放到另一区域时移动原规则，目标区域满时保留原规则。
3. 右键鼓风机，将过滤器放入专用插槽，并选择统一加工模式。**匹配过滤器的物品使用其指定方式，未匹配的物品使用鼓风机当前模式**；装有过滤器时也能随时切换统一模式。

一个下界合金板加一个原版鼓风机或过滤器，分别无序合成一个对应升级品。拆除鼓风机会掉落所装过滤器并保留规则。过滤器也能作为 Create 物流设备的物品白名单使用。

**思索教程：** 在背包中悬停下界合金鼓风机，按住思索键（默认 `W`），可查看“内置加工模式”和“过滤器规则与统一模式”两段用法动画，学习接入动力、选择模式、放入待加工物品和配置过滤器。下界合金过滤器也可打开规则教程。字幕支持中文和英文。

**物品说明：** 已有思索的物品沿用 Create 原生思索入口；其余物品使用原生“按住 Shift 查看概要”。储罐容量倍率与燃烧室当前数量、倍率均在 Shift 展开后显示。

# 防爆与防火

上述全部物品（含强力机械泵和下界合金板）的掉落物均免疫爆炸伤害；十种方块放置后均不被爆炸破坏，储罐与保险库中的内容也因此保留。方块基础爆炸抗性统一为 `1200`。

全部十二种物品也具有下界合金物品的防火属性，掉落在火焰或岩浆中不会被烧毁。

# 环境要求

- Minecraft `1.21.1`
- NeoForge `21.1.219` 或更新版本（当前构建使用 `21.1.219`）
- Create `6.0.10`（当前构建依赖 `6.0.10-280`，声明版本范围为 `[6.0.10,6.1.0)`）
- Java `21`

# 配置

服务端配置文件：

```text
createnetherite-server.toml
```

主要配置项：

- `powerfulPumpStressImpact`：默认 `16.0`，强力泵基础应力消耗
- `powerfulPumpPressureMultiplier`：默认 `8.0`，强力泵压力倍率
- `netheriteFluidTankCapacityMultiplier`：默认 `16`，储罐单方块容量倍率
- `netheriteElevatorPulleyMaxDistance`：默认 `768`，升降机最大运输距离（格）
- `netheriteFanProcessingSpeedMultiplier`：默认 `4`，鼓风机每 tick 的加工进度倍率
- `netheriteFanWindSpeedMultiplier`：默认 `2.0`，鼓风机的气流计算风速倍率

注意：已经创建过的世界会保留自己的 `serverconfig/createnetherite-server.toml`。如果修改了默认值，旧世界需要手动修改对应世界目录下的配置文件，并重启世界或服务器。

# 构建

```text
.\gradlew.bat build
```

构建产物：

```text
build/libs/createnetherite-1.21.1-0.2.5.jar
```

# Introduction

**[Create-Netherite-Upgrades](https://github.com/von-Neumann101/Create-Netherite-Upgrades)** is a Create addon for Minecraft 1.21.1 on NeoForge.

Current branch: `1.21.1-NeoForge`

Current mod version: `0.2.5`

# Maintenance status

Since 2026-10-29, development primarily targets Minecraft `1.21.1` on the `1.21.1-NeoForge` branch. Features, fixes, and dependency updates focus on this branch.

Updates to the Minecraft `1.20.1` Forge `main` branch are paused. Its code and historical technical documentation remain available. The two versions have feature differences, and new content will not be synchronized while Forge updates are paused.

Technical documentation (Chinese): [NeoForge 1.21.1](docs/neoforge-1.21.1.md) · [Forge 1.20.1 (historical reference)](docs/forge-1.20.1.md)

# Content

The mod currently adds several Create-compatible entries:

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

The Powerful Mechanical Pump uses Create's native fluid network, with a default base stress impact of `16.0` and a pressure multiplier of `8.0`. The pressure multiplier increases throughput without extending pipe propagation distance.

The Netherite Fluid Tank defaults to `16` times the regular tank's capacity per block and supports boilers paired with Netherite Steam Engines. Below `45` tank blocks, every `4` blocks provide one size level. At `3×3×5` (`45` blocks) or larger, the size level is at least `18` and rises with `min(water supply level, heat level)`, so size no longer limits the actual boiler level. Each water supply level still requires `12 mB/t`, with no level `18` cap.

Each Netherite Steam Engine has `256` times the output limit of Create's regular engine: `4,194,304 SU` at full output with Create's default configuration, with a maximum speed of `64 RPM`. Actual output still depends on boiler conditions and the number of attached engines. Regular and netherite boilers each recognize their corresponding steam engines.

The Netherite Item Vault provides `16` times the regular vault's inventory slots while retaining multiblock and mounted storage behavior.

The Netherite Elevator Pulley uses twice the rotational speed in Create's movement speed calculation. Its default maximum travel distance is `768` blocks, also limited by the world's minimum build height.

The Netherite Mechanical Press supports bulk pressing and repeatedly processes the current matching basin recipe. Its base stress impact follows the regular Mechanical Press.

The Netherite Mechanical Mixer uses the same batch acceleration as the Netherite Mechanical Press: when a mixing cycle finishes, it repeatedly processes the current recipe while it still matches. Movement and per-cycle waiting time follow Create, so the throughput gain depends on the ingredients available in the basin. Stress consumption matches the regular mixer's (`1×`, default base impact: `4 SU/RPM`). Recipes, heat requirements, shapeless crafting, brewing, filtering, and basin output follow Create's rules. Craft one Netherite Sheet with a regular Mechanical Mixer to obtain the upgrade.

The Netherite Mechanical Arm advances movement at `3` times the normal rate with a `3` times movement speed cap. At the same RPM, it consumes `1.5` times the regular arm's stress (default base impact: `3 SU/RPM`). Its working radius is `3` times Create's mechanical arm range setting, expanding the default from `5` to `15` blocks. Targets must be strictly within this spherical radius, following Create's distance rule. Target selection, distribution modes, redstone control, and automated feeding follow Create. Craft one Netherite Sheet with a regular Mechanical Arm to obtain the upgrade.

The Netherite Blaze Burner has empty and filled upgrade recipes: one Netherite Sheet plus a regular Empty Blaze Burner produces an empty netherite burner (`0` Blazes); upgrading a filled regular burner retains its `1` Blaze. Two sheets and three corresponding regular burners provide the bulk upgrade. Hold an empty burner and repeatedly right-click Blazes to fill it from `0` to `4`.

The empty variant has its own name and cage model, consumes no fuel, supplies no heat, and stays empty through placement and pickup. Hold `Shift` over the item to see its count and rates. Engineer's Goggles show filled burners' counts and rates; placed burners render the corresponding number of Blazes. Breaking, wrench pickup, placement, and saves retain the count. Existing burners default to `1` Blaze.

| Blazes | Boiler heat multiplier | Fuel consumption multiplier |
| --- | --- | --- |
| 0 (empty) | No heat | 0× |
| 1 | 1× | 0.5× |
| 2 | 2× | 1× |
| 3 | 3× | 1.5× |
| 4 | 4× | 2× |

Fuel types and normal/superheated states follow Create. Manual and automated feeding still insert one fuel item at a time; its burn timer advances at the rate shown above. At `4` Blazes, each fuel lasts half as long as in a regular burner. Superheated boiler heat scales too: a full burner supplies `4` heat levels when heated and `8` when superheated. Without fuel, it supplies no active heat.

Netherite Sheets are pressed from Netherite Ingots using Create and serve as upgrade materials for these machines.

# Netherite Fan and Filter

The Netherite Encased Fan provides built-in Washing, Blasting, Haunting, and Smoking modes without requiring water, lava, soul fire, or fire in front of it. It processes dropped items, items on belts, and items on depots using Create's recipes and processing rules. Processing advances at `4` times the normal rate by default, and air-current speed calculations use a `2` times multiplier. Airflow range and kinetic network RPM follow Create's normal rules.

To use it:

1. Hold a Netherite Filter in your main hand and right-click. Its four processing areas each hold `9` item samples.
2. Drag an inventory item into a sample slot, or hover an item and press `1` through `4` to assign Washing, Blasting, Haunting, or Smoking. Samples consume no real items. Right-click or click with an empty cursor to clear a sample. Moving an item kind to another area moves its rule; a full destination area leaves the previous rule unchanged.
3. Right-click the fan, insert the filter into its slot, and select the default processing mode. **Matching items use their filter rule; unmatched items use the fan's selected mode.** The default mode remains selectable while a filter is installed.

Craft one Netherite Sheet with a regular Encased Fan or Filter to obtain the corresponding upgrade. Breaking the fan drops its installed filter with its rules preserved. The filter also works as an item allow list in Create logistics devices.

**Ponder tutorials:** Hover over the Netherite Encased Fan in your inventory and hold the Ponder key (default: `W`) to watch two usage tutorials covering power connections, mode selection, item placement, and filter configuration. The Netherite Filter also opens the filter tutorial. Captions are available in English and Chinese.

**Item descriptions:** Items with Ponder scenes keep Create's native Ponder entry. Other items use the native hold-Shift summary. Tank capacity multipliers and the burner's current count and rates appear when holding Shift.

# Blast and Fire Protection

All items listed above, including the Powerful Mechanical Pump and Netherite Sheet, are immune to explosion damage when dropped. All ten placed blocks are protected from explosion destruction, preserving tank and vault contents. Their base blast resistance is `1200`.

All twelve items also have the fire resistance of netherite items: dropped items cannot be burned by fire or lava.

# Requirements

- Minecraft `1.21.1`
- NeoForge `21.1.219` or newer (current build: `21.1.219`)
- Create `6.0.10` (current build dependency: `6.0.10-280`; declared range: `[6.0.10,6.1.0)`)
- Java `21`

# Configuration

Server config file:

```text
createnetherite-server.toml
```

Main config entries:

- `powerfulPumpStressImpact`: default `16.0`, base stress impact of the powerful pump
- `powerfulPumpPressureMultiplier`: default `8.0`, pressure multiplier of the powerful pump
- `netheriteFluidTankCapacityMultiplier`: default `16`, tank capacity multiplier per block
- `netheriteElevatorPulleyMaxDistance`: default `768`, maximum elevator travel distance in blocks
- `netheriteFanProcessingSpeedMultiplier`: default `4`, fan processing progress multiplier per tick
- `netheriteFanWindSpeedMultiplier`: default `2.0`, fan air-current speed multiplier

Existing worlds keep their own `serverconfig/createnetherite-server.toml`. If default values are changed, older worlds need their world-specific config updated manually, then the world or server must be restarted.

# Build

```text
.\gradlew.bat build
```

Build output:

```text
build/libs/createnetherite-1.21.1-0.2.5.jar
```
