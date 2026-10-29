# 简介

**[Create-Netherite-Upgrades](https://github.com/von-Neumann101/Create-Netherite-Upgrades)** 是一个面向 Minecraft 1.21.1 NeoForge 的 Create 附属模组。

当前分支：`1.21.1-NeoForge`

当前模组版本：`0.2.4`

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
- `createnetherite:netherite_blaze_burner`
- `createnetherite:netherite_sheet`

强力机械泵接入 Create 原版流体管网，默认基础应力消耗为 `16.0`、压力倍率为 `8.0`。压力倍率提高吞吐，不增加管道传播距离。

下界合金流体储罐默认拥有普通储罐 `16` 倍的单方块容量，并支持搭配下界合金蒸汽引擎构建锅炉。尺寸等级和供水等级均没有原版的 `18` 级上限：每 `4` 个储罐方块提供 `1` 级尺寸等级，每级供水需求仍为 `12 mB/t`。实际锅炉等级取尺寸、供水和热量等级的最低值。下界合金蒸汽引擎基础应力容量为 `2048`，最高 `64 RPM`。普通与下界合金锅炉分别识别各自的蒸汽引擎。

下界合金物品保险库拥有普通保险库 `16` 倍的槽位数，保留多方块和装配到运动结构上的存储行为。

下界合金升降机滑轮在原版移动速度计算中使用 `2` 倍转速，默认最大运输距离为 `768` 格，实际范围仍受世界最低建造高度限制。

下界合金动力冲压机支持批量冲压，并在工作盆中连续处理当前可匹配的配方；基础应力消耗沿用普通动力冲压机。

下界合金烈焰人燃烧室提供普通烈焰人燃烧室 `3` 倍的锅炉热量，燃料和热状态沿用 Create 行为。

下界合金板由下界合金锭经 Create 冲压制成，用作上述机械的升级材料。

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

注意：已经创建过的世界会保留自己的 `serverconfig/createnetherite-server.toml`。如果修改了默认值，旧世界需要手动修改对应世界目录下的配置文件，并重启世界或服务器。

# 构建

```text
.\gradlew.bat build
```

构建产物：

```text
build/libs/createnetherite-1.21.1-0.2.4.jar
```

# Introduction

**[Create-Netherite-Upgrades](https://github.com/von-Neumann101/Create-Netherite-Upgrades)** is a Create addon for Minecraft 1.21.1 on NeoForge.

Current branch: `1.21.1-NeoForge`

Current mod version: `0.2.4`

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
- `createnetherite:netherite_blaze_burner`
- `createnetherite:netherite_sheet`

The Powerful Mechanical Pump uses Create's native fluid network, with a default base stress impact of `16.0` and a pressure multiplier of `8.0`. The pressure multiplier increases throughput without extending pipe propagation distance.

The Netherite Fluid Tank defaults to `16` times the regular tank's capacity per block and supports boilers paired with Netherite Steam Engines. Size and water supply levels can exceed the vanilla cap of `18`: every `4` tank blocks provide one size level, and each water supply level still requires `12 mB/t`. The actual boiler level is the lowest of its size, water supply, and heat levels. The Netherite Steam Engine has a base stress capacity of `2048` and a maximum speed of `64 RPM`. Regular and netherite boilers each recognize their corresponding steam engines.

The Netherite Item Vault provides `16` times the regular vault's inventory slots while retaining multiblock and mounted storage behavior.

The Netherite Elevator Pulley uses twice the rotational speed in Create's movement speed calculation. Its default maximum travel distance is `768` blocks, also limited by the world's minimum build height.

The Netherite Mechanical Press supports bulk pressing and repeatedly processes the current matching basin recipe. Its base stress impact follows the regular Mechanical Press.

The Netherite Blaze Burner provides `3` times the regular burner's boiler heat while retaining Create's fuel and heat-state behavior.

Netherite Sheets are pressed from Netherite Ingots using Create and serve as upgrade materials for these machines.

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

Existing worlds keep their own `serverconfig/createnetherite-server.toml`. If default values are changed, older worlds need their world-specific config updated manually, then the world or server must be restarted.

# Build

```text
.\gradlew.bat build
```

Build output:

```text
build/libs/createnetherite-1.21.1-0.2.4.jar
```
