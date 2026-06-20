# 简介

**[Create-Netherite-Upgrades](https://github.com/von-Neumann101/Create-Netherite-Upgrades)** 是一个面向 Minecraft Forge 1.20.1 的 Create 附属模组。

当前模组新增多个 Create 兼容方块：

- `createnetherite:powerful_mechanical_pump`
- `createnetherite:netherite_fluid_tank`

强力机械泵继承 Create 原版机械泵的基础行为，接入 Create 原版流体管网。它拥有更高的基础应力消耗，并通过配置倍率提高写入 Create 流体网络的 pressure，从而提升实际流体吞吐量。

下界合金流体储罐是 Create 原版流体储罐的强化版，继承原版流体储罐的行为，主要区别是容量更高。（注意，锅炉这一多方块结构无法使用该方块构建）

# 环境要求

- Minecraft `1.20.1`
- Forge `47.1.x`
- Create `6.0.8`
- Java `17`

# 配置

服务端配置文件：

```text
booster-server.toml
```

主要配置项：

- `powerfulPumpStressImpact`：默认 `16.0`
- `powerfulPumpPressureMultiplier`：默认 `8.0`
- `netheriteFluidTankCapacityMultiplier`：默认 `16.0`

注意：已经创建过的世界会保留自己的 `serverconfig/booster-server.toml`。如果修改了默认值，旧世界需要手动修改对应世界目录下的配置文件，并重启世界或服务器。
