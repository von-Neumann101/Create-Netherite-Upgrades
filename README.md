# 简介

**[Create-Netherite-Upgrades](https://github.com/von-Neumann101/Create-Netherite-Upgrades)** 是一个面向 Minecraft 1.21.1 NeoForge 的 Create 附属模组。

当前分支：`1.21.1-NeoForge`

当前模组新增多个 Create 兼容内容：

- `createnetherite:powerful_mechanical_pump`
- `createnetherite:netherite_fluid_tank`
- `createnetherite:netherite_sheet`

强力机械泵继承 Create 原版机械泵的基础行为，接入 Create 原版流体管网。它拥有更高的基础应力消耗，并通过配置倍率提高写入 Create 流体网络的 pressure，从而提升实际流体吞吐量。

下界合金流体储罐是 Create 原版流体储罐的强化版，继承原版流体储罐的行为，主要区别是容量更高。（注意，锅炉这一多方块结构无法使用该方块构建）

下界合金板由 Create 的冲压配方制成，用作强力机械泵和下界合金流体储罐的基础材料。

# 环境要求

- Minecraft `1.21.1`
- NeoForge `21.1.242`
- Create `6.0.10`
- Java `21`

# 配置

服务端配置文件：

```text
createnetherite-server.toml
```

主要配置项：

- `powerfulPumpStressImpact`：默认 `16.0`
- `powerfulPumpPressureMultiplier`：默认 `8.0`
- `netheriteFluidTankCapacityMultiplier`：默认 `16`

注意：已经创建过的世界会保留自己的 `serverconfig/createnetherite-server.toml`。如果修改了默认值，旧世界需要手动修改对应世界目录下的配置文件，并重启世界或服务器。

# 构建

```text
.\gradlew.bat build
```

构建产物：

```text
build/libs/createnetherite-1.21.1-0.1.0.jar
```

# Introduction

**[Create-Netherite-Upgrades](https://github.com/von-Neumann101/Create-Netherite-Upgrades)** is a Create addon for Minecraft 1.21.1 on NeoForge.

Current branch: `1.21.1-NeoForge`

The mod currently adds several Create-compatible entries:

- `createnetherite:powerful_mechanical_pump`
- `createnetherite:netherite_fluid_tank`
- `createnetherite:netherite_sheet`

The Powerful Mechanical Pump inherits the base behavior of Create's Mechanical Pump and remains connected to Create's native fluid pipe network. It has a higher base stress impact and uses a configurable pressure multiplier to increase the pressure written into Create's fluid network, improving practical fluid throughput.

The Netherite Fluid Tank is an upgraded version of Create's Fluid Tank. It inherits the regular tank behavior, with the main difference being higher capacity. Boiler multiblock structures cannot be built with this block.

Netherite Sheets are made with Create's pressing recipe and are used as base materials for the Powerful Mechanical Pump and Netherite Fluid Tank.

# Requirements

- Minecraft `1.21.1`
- NeoForge `21.1.242`
- Create `6.0.10`
- Java `21`

# Configuration

Server config file:

```text
createnetherite-server.toml
```

Main config entries:

- `powerfulPumpStressImpact`: default `16.0`
- `powerfulPumpPressureMultiplier`: default `8.0`
- `netheriteFluidTankCapacityMultiplier`: default `16`

Existing worlds keep their own `serverconfig/createnetherite-server.toml`. If default values are changed, older worlds need their world-specific config updated manually, then the world or server must be restarted.

# Build

```text
.\gradlew.bat build
```

Build output:

```text
build/libs/createnetherite-1.21.1-0.1.0.jar
```
