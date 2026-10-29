# 下界合金鼓风机与过滤器实现计划

**目标：** 新增内置加工介质的下界合金鼓风机和四区物品规则过滤器。仅维护 Minecraft 1.21.1 / NeoForge 21.1.219 / Create 6.0.10-280。

**用户已明确：** 装过滤器后仍能选择统一加工方式；匹配过滤器的物品使用其规则，未匹配的物品使用鼓风机选择的方式。

## 行为

- 四种方式与数字键顺序：`WASHING` 水洗、`BLASTING` 岩浆熔炼、`HAUNTING` 灵魂火缠魂、`SMOKING` 火焰烟熏。默认水洗，无须前方放置介质。
- 过滤器四区，每区 3×3 样本。样本不消耗物品，只按物品种类匹配。同种物品跨区分配时移动规则；满区保持原规则。支持从背包直接拖到样本格、取物品后点击样本格，或悬停背包/样本物品并按 1～4。
- 鼓风机右键菜单：四种模式始终可选，一个真实过滤器插槽。拆除时掉落所装过滤器并保留规则。
- 复用 Create 的配方及气流碰撞、吹吸、运动、渲染，支持掉落物、传送带和置物台。默认加工进度每 tick 推进 4 次原版步骤，转换或销毁后立即停止；气流计算风速乘 2，不修改动能网络转速。
- 新物品防火、方块抗爆性 1200，继承现有命名空间防爆保护。升级配方分别使用一个下界合金板 + 原版鼓风机/过滤器。

## 共享接口（整合者负责）

`content/kinetics/fan/FanProcessingMode`：上述四个 enum 值；`serializedName(): String`、`displayName(): Component`、`icon(): ItemStack`、`color(): int`、`processingType(): FanProcessingType`、静态 `byIndex(int)` / `byName(String)`。无效值回退水洗；菜单先校验网络输入。

`content/logistics/filter/NetheriteFilterData`：`GROUP_SIZE=9`、`GROUP_COUNT=4`、`SLOT_COUNT=36`；静态方法：

- `inventory(ItemStack filter): ItemStackHandler`，36 格样本。
- `save(ItemStack filter, ItemStackHandler inventory): void`。
- `findMode(ItemStack filter, ItemStack target): FanProcessingMode`，无规则返回 null。
- `resolve(ItemStack filter, ItemStack target, FanProcessingMode fallback): FanProcessingMode`。
- `assign(ItemStackHandler inventory, int slot, ItemStack sample): boolean`，只存种类和数量 1，删除其他同类规则；空样本清空。
- `assignToGroup(ItemStackHandler inventory, FanProcessingMode mode, ItemStack sample): boolean`，使用组内已有同类格或首个空格，满时返回 false 且不改变原有规则。

样本存储：过滤器 `DataComponents.CUSTOM_DATA` 的 `FanRules` 为 36 个字符串的 ListTag，各索引保存物品注册 ID（空格为空字符串）。保存时保留其他 CUSTOM_DATA；全空移除 FanRules。不复制物品的嵌套数据。

## 任务 A：数据、整合与资源（整合者）

- [x] 实现共享数据及规则分配、回退、保存/读取检查。
- [x] 新注册字段：`BoosterBlocks.NETHERITE_ENCASED_FAN`、`BoosterItems.NETHERITE_ENCASED_FAN` / `NETHERITE_FILTER`、`BoosterBlockEntityTypes.NETHERITE_ENCASED_FAN`。
- [x] 配置字段：`netheriteFanProcessingSpeedMultiplier`（IntValue，默认 4，范围 1..64）和 `netheriteFanWindSpeedMultiplier`（DoubleValue，默认 2，范围 1..16）。
- [x] 菜单注册、screens、原版风机应力/renderer/Flywheel visual、Mixin 配置。
- [x] 创造栏、模型、配方、掉落表、工具标签、中英语言和文档。

## 任务 B：鼓风机后台（并行，独占以下文件）

- [x] `content/kinetics/fan/NetheriteEncasedFanBlock.java`
- [x] `content/kinetics/fan/NetheriteEncasedFanBlockEntity.java`
- [x] `content/kinetics/fan/NetheriteAirCurrent.java`
- [x] `mixin/NetheriteAirCurrentMixin.java`

BE 公开接口：`getFilterInventory(): ItemStackHandler`（一格、最多一个、仅下界合金过滤器）、`getProcessingMode(): FanProcessingMode`、`setProcessingMode(FanProcessingMode)`、`resolveProcessingMode(ItemStack)`、`getProcessingSpeedMultiplier(): int`、`getWindSpeedMultiplier(): float`。实现 MenuProvider，构造 `new NetheriteFanMenu(id, inv, this)`；打开菜单发送 BlockPos。保存 Mode 和过滤器库存，修改时同步，拆除时正确掉落真实过滤器。

新 AirCurrent 的 getTypeAt 返回选定方式并保留距离边界；覆盖短小的 tickAffectedHandlers，为每件传送物品选规则并加速，保留原版产出。掉落物保留原版运动算法，以三个限定新风机的 Mixin 修改 tickAffectedEntities：加工类型、加工调用次数、IAirCurrentSource.getSpeed 的气流倍率。不要覆盖 BE 的动能 getSpeed，也不要复制整个气流类。

## 任务 C：菜单与界面（并行，独占以下文件）

- [x] `content/logistics/filter/NetheriteFilterItem.java`：继承 Create FilterItem，getComponentType 为 CUSTOM_DATA；在普通物流过滤中将全部样本作为白名单。
- [x] `content/logistics/filter/NetheriteFilterMenu.java`：继承 HeldItemGhostItemMenu，玩家背包格在前、36 个样本格在后。用原版 SWAP 消息承载数字键 1～4，服务端从所指槽读取样本。鼠标样本格调用 assign，按键调用 assignToGroup；保留原版真实物品保护与保存。
- [x] `client/NetheriteFilterScreen.java`：四区图标/编号，鼠标与数字键提示，深灰面板与清楚格边。
- [x] `content/kinetics/fan/NetheriteFanMenu.java`：构造 `(int, Inventory, NetheriteEncasedFanBlockEntity)` 与 `(int, Inventory, RegistryFriendlyByteBuf)`；真实过滤器格和背包，DataSlot 同步模式、clickMenuButton 选择 0～3，检查距离/方块/输入，正确 shift-click。
- [x] `client/NetheriteFanScreen.java`：四种方式按钮、过滤器格，提示“未匹配物品使用当前模式”。
- [x] `registry/BoosterMenuTypes.java`：DeferredRegister，字段 NETHERITE_FILTER / NETHERITE_FAN，register(IEventBus)。

界面代理不修改 Booster、BoosterClient、现有注册/配置/资源或共享数据类。整合者添加 RegisterMenuScreensEvent。后台代理也不改这些整合文件。所有包以 `io.github.boosterproject.booster` 为前缀。

## 验证与收尾

- [x] 检查匹配优先、未匹配回退、跨区移动、满区不丢规则、样本不改变真实物品、存储往返。
- [x] 检查两条加工路径成功即停止、无动力不加工、普通风机不变。
- [x] 完整构建、锅炉回归；在运行环境允许时核对 Mixin 启动和两个界面，分别记录验证范围。
- [x] 更新 README 和 NeoForge 技术文档；只提交本任务文件，自动 push。

官方锁定版本源码归档：`build/create-6.0.10-sources.jar`（不提交）。不以相邻旧 Forge 源码替代当前版本接口。

进度：已实现并整合全部功能；完整构建、15 项锅炉检查和 15 项过滤器检查通过，81 个资源 JSON 解析通过，独立源码审查未发现重要问题。当前客户端已启动并进入独立新测试世界；因同时输入停止界面操作，菜单交互和真实气流加工尚未完成游戏内验证。验证详情见 NeoForge 技术文档；完成收尾后按项目约定自动提交与推送。
