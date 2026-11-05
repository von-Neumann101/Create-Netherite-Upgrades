# 下界合金转换机实施计划

> **For agentic workers:** Use superpowers:executing-plans inline, then request a fresh-context whole-change review.

**Goal:** 新增固定2048SU/5秒的转换机、专属精华搅拌及全部14类机器转换配方。

**Architecture:** 两个Create标准ProcessingRecipe类型、NeoForge标准流体；转换机继承KineticBlockEntity并安装BeltProcessingBehaviour，单个板槽与精华槽提供物品/流体能力，真实网络以2048/abs(theoreticalRPM)基础impact维持固定2048SU，原生过载停止。100tick逐个转换，资源在完成时扣除。

**Tech Stack:** Minecraft1.21.1 / NeoForge21.1.219 / Create6.0.10-280 / Java21。

**Spec:** `docs/superpowers/specs/2026-11-05-netherite-converter-design.md`

- [x] 写GameTest注册/实际配方基线，确认缺失功能时失败。
- [x] 注册精华FluidType/source/flowing/block/bucket、两种配方类型与15条转换/1条搅拌/转换机合成配方；下界合金搅拌机独有recipe cache和静态过滤。
- [x] 实现方块/BE、固定SU、连续100tick与断电重置、板次数、精华扣除、输出组件及真实传送带/置物台交互。
- [x] 接入注册、客户端静态板显示与流体、护目镜/Shift、创造栏、工具/掉落标签、防火防爆；集成指定美术对话的7张新PNG。
- [x] 验证16/256RPM均2048SU且100tick、容量不足/断电/资源不足不消耗，16次板材、组件与保存掉落、真实传送带、专属工作盆与普通搅拌机拒绝、配方网络往返。
- [x] 完整离线build及服务端回归、独立审查、资源与jar核对；同步README/技术文档和0.3.14。按项目约定自动提交推送。

Review focus：网络变化时SU不漂移；原版搅拌机不匹配精华；停止或无原料不扣资源；空/满燃烧室正确；有内容的机器组件不丢失；板次数不能通过取出/掉落/重载重置。旧PNG及无关工作保持，禁用客户端/RTX5060。

实现决策：计时移到BE.tick内扫描原生处理库存，BeltProcessingBehaviour仅锁定输入；这样停带时也不会漏掉断电清零，处理物品取自当前库存而非旧引用。零转速基础impact保持2048，实际负载仍为0，同时让低容量手持工具按既有规则拒绝该机器。

验收：完整构建205项回归、全部45项GameTest通过。搅拌夹具改用真实齿轮；独立审查指出的精华图集登记遗漏已补两条single并复核。168JSON、两语各137键、236生产资源、152类均核对；58旧PNG不变，7新增PNG规格正确。最终JAR元数据0.3.14，无QA内容；未启动客户端或使用RTX5060。
