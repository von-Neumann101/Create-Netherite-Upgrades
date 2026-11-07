# 简介

**[Create-Netherite-Upgrades](https://github.com/von-Neumann101/Create-Netherite-Upgrades)** 是一个面向 Minecraft 1.21.1 NeoForge 的 Create 附属模组。

当前分支：`1.21.1-NeoForge`

当前模组版本：`0.3.21`

# 维护状态

自 2026-10-29 起，主要维护 Minecraft `1.21.1` 的 `1.21.1-NeoForge` 分支。后续功能、问题修复和依赖更新以该分支为主。

Minecraft `1.20.1` Forge 的 `main` 分支暂停更新，保留现有代码和历史技术文档。两个版本目前存在功能差异，暂停期间不再同步新增内容。

技术文档：[NeoForge 1.21.1](docs/neoforge-1.21.1.md) · [Forge 1.20.1（历史参考）](docs/forge-1.20.1.md)

# 模组内容

当前模组新增多个 Create 兼容内容：

- `createnetherite:powerful_mechanical_pump`
- `createnetherite:netherite_converter`
- `createnetherite:nether_essence_bucket`
- `createnetherite:netherite_fluid_tank`
- `createnetherite:netherite_steam_engine`
- `createnetherite:netherite_item_vault`
- `createnetherite:netherite_elevator_pulley`
- `createnetherite:netherite_mechanical_press`
- `createnetherite:netherite_mechanical_saw`
- `createnetherite:netherite_mechanical_mixer`
- `createnetherite:netherite_mechanical_arm`
- `createnetherite:netherite_blaze_burner`
- `createnetherite:netherite_encased_fan`
- `createnetherite:netherite_filter` (legacy / 旧存档兼容)
- `createnetherite:netherite_wireless_terminal`
- `createnetherite:netherite_infinite_manager` (portable)
- `createnetherite:netherite_wireless_manager` (block)
- `createnetherite:netherite_smart_chute`
- `createnetherite:netherite_weighted_ejector`
- `createnetherite:netherite_extendo_grip`
- `createnetherite:netherite_deployer`
- `createnetherite:netherite_netherite`
- `createnetherite:netherite_wrench`
- `createnetherite:netherite_netherite_wrench`
- `createnetherite:assembly_table`
- `createnetherite:netherite_sheet`

强力机械泵接入 Create 原版流体管网，默认基础应力消耗为 `16.0`、压力倍率为 `8.0`。压力倍率提高吞吐，不增加管道传播距离。

下界合金流体储罐默认拥有普通储罐 `16` 倍的单方块容量，并支持搭配下界合金蒸汽引擎构建锅炉。不足 `45` 个储罐方块时，每 `4` 个方块提供 `1` 级尺寸等级；达到 `3×3×5`（`45` 个方块）后，尺寸等级至少为 `18`，并随 `min(供水等级, 热量等级)` 继续上涨，不再限制实际锅炉等级。每级供水需求仍为 `12 mB/t`，供水等级没有 `18` 级上限。

单个下界合金蒸汽引擎的输出上限为 Create 原版引擎的 `256` 倍；按 Create 默认配置，满额输出为 `4,194,304 SU`，最高 `64 RPM`。实际输出仍受锅炉状态和连接的引擎数量影响。普通与下界合金锅炉分别识别各自的蒸汽引擎。

下界合金物品保险库拥有普通保险库 `16` 倍的槽位数，保留多方块和装配到运动结构上的存储行为。

下界合金升降机滑轮在原版移动速度计算中使用 `2` 倍转速，默认最大运输距离为 `768` 格，实际范围仍受世界最低建造高度限制。

下界合金动力冲压机支持批量冲压，并在工作盆中连续处理当前可匹配的配方；基础应力消耗沿用普通动力冲压机。

下界合金动力搅拌机采用与下界合金冲压机相同的批量加速方式：一次搅拌周期结束时，连续处理工作盆中当前仍可匹配的配方。搅拌动作和每周期等待时间沿用原版，批量处理的吞吐提升取决于盆内材料数量。应力消耗为普通搅拌机的 `1` 倍（默认基础值 `4 SU/RPM`）；配方、加热、超级加热、无序合成、酿造、过滤和工作盆输出规则沿用 Create。一个下界合金板加一个原版动力搅拌机，无序合成一个升级品。

下界合金动力臂的动作进度和动作速度上限均为原版的 `3` 倍，同转速下应力消耗为原版的 `1.5` 倍（默认基础值为 `3 SU/RPM`）。工作半径为 Create 机械手范围配置的 `3` 倍，默认从 `5` 格扩大到 `15` 格；沿用原版球形距离判定，目标距离须小于该半径。选点、分配模式、红石控制与自动喂料沿用原版。一个下界合金板加一个原版机械手，无序合成一个升级品。

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

# 下界合金鼓风机

下界合金鼓风机内置水洗、岩浆熔炼、灵魂火缠魂和火焰烟熏，无须在风口前放置介质。支持掉落物、传送带与置物台，沿用 Create 配方；默认加工进度为原版的 `4` 倍、气流计算风速为 `2` 倍，风距和动能网络转速沿用原版。

使用方式：

1. 先手持 Create 原版过滤表右键，设置匹配清单；支持普通过滤表、属性过滤器等 Create 原生过滤器。
2. 右键鼓风机，将过滤器分别装入水洗、熔炼、缠魂、烟熏按钮下方的对应槽。**每槽放一个真实过滤器，不接受普通物品或专用下界合金过滤器**。支持正常放取与 Shift 快捷转移；取出过滤器后可修改清单。
3. 点击加工模式按钮，设置未匹配物品的加工方式。**匹配某槽的物品使用该槽固定加工方式，未匹配的物品仍按所选模式加工**；四个槽有无过滤器均可切换所选模式。

例如，在“熔炼”槽安装包含粗铁和粗金的原版过滤表，两种原料都会熔炼；同时把未匹配模式设为水洗，其他物品便按水洗规则尝试加工。多槽同时匹配时按水洗→熔炼→缠魂→烟熏取第一项；空槽跳过，过滤器的黑白名单、组件匹配和嵌套规则沿用原版。

下界合金板加原版鼓风机可无序升级。拆除鼓风机返还四槽中的真实过滤器及配置。

**旧版兼容：** 专用下界合金过滤器不再提供合成、创造栏或思索入口，保留注册和原配置查看以兼容旧存档。旧机器内已安装的专用过滤器会在打开新界面时退还，背包满则掉落；未打开就拆除也会掉落原物，不会生成额外原版过滤表。升级后的四个新槽为空，按当前所选模式加工，需要重新安装原版过滤表以分类。

**思索教程：** 悬停下界合金鼓风机并按住默认 `W`，查看“内置加工模式”和“过滤器规则与统一模式”两段用法动画。教程现使用原版过滤表和四槽操作，支持中英文字幕。

**物品说明与界面：** 转换机、鼓风机、弹射置物台和伸缩机械手沿用原生思索入口，其余无思索物品使用原生 Shift 概要。界面 PNG 位于 jar 内 `assets/createnetherite/textures/gui/`，资源包可按相同路径及原尺寸替换，文字与物品由游戏绘制。

# 无线红石

下界合金无线红石信号终端（无线红石加载器）可平放、贴墙或倒挂；点击哪个面就安装到哪个面，平放和倒挂时按玩家朝向旋转。放置后默认私有。所有者右键终端可命名和设置公开状态，显示名称为“放置玩家名的终端名”。管理器同时显示坐标和所在维度；同名终端仍是独立目标。

**下界合金无线管理器**是可放置的方块，**无需应力**，右键打开管理界面。与便携版共用终端列表及多选功能；多人使用同一个方块时，每人仍只看到自己的终端和其他玩家公开的终端。使用方块界面时需保持在8格以内且处于同一维度，拆除或离开后旧界面失效；目标终端仍可跨维度、无限距离激活。

方块版有序合成：

| 末影之眼 | 发信线圈 | 地狱疣 |
| --- | --- | --- |
| 黄铜机壳 | 下界合金锭 | 黄铜机壳 |
| 黑曜石 | 哭泣的黑曜石 | 黑曜石 |

便携版沿用旧物品ID与原有合成配方，既有存档中的物品继续使用。

手持**便携式下界合金无线管理器**并右键，查看自己放置的终端和其他玩家公开的终端。点击条目勾选，支持跨页多选、“全选本页”、“清空选择”及同时激活所选终端；也可以直接编辑自己的终端。

无需频率，可跨维度、无限距离激活，包括未加载区块中的终端。每次发送输出强度为 `15`、持续 `1 秒` 的红石信号，并保持目标区块加载 `15 秒`。再次激活重新计算两个期限；计时使用游戏 tick（`20 / 300 tick`）。仅打开列表不会加载目标区块。拆除会注销终端，重新放置后按新终端登记。

合成方式：下界合金板 + Create 无线红石信号终端（`create:redstone_link`）获得下界合金终端；下界合金板 + Create 无线红石遥控器（`create:linked_controller`）获得管理器，均为无序合成。

# 下界合金智能溜槽

无需鼓风机即可按物品规则向上、向下输送。机身原生过滤槽控制哪些物品可以进入，支持原版过滤表、属性过滤与抽取数量设置；红石信号暂停输送。

空手右键主体打开设置，将物品或配置好的原版过滤表拖入“上行、下行”两个样本槽，也可悬停背包物品按 `1 / 2` 分配。样本不消耗物品，右键或空手点击清除。多个物品可先放进同一个原版过滤表；重叠规则优先上行，未匹配的物品按界面选定方向输送，初始默认下行。

两种方向分别缓存一个堆叠，支持同一列中不同物品相向输送；每段按各自规则处理，搭建长列时需为各段配置对应方向。堵塞会保留物品，保存重载保留缓存与规则；已进入缓存的物品完成原方向。Shift 空手右键取出缓存，拆除时掉落真实缓存，方向样本不会变成实物。连接原版溜槽时，原版段仍按其气流方向及红石状态自行输出。

一个下界合金板加一个原版智能溜槽，无序合成一个下界合金智能溜槽。物品沿用防火、防爆与原生 Shift 概要，GUI 使用独立 `netherite_smart_chute.png`。

# 下界合金转换机与下界精华

新增转换途径，原有合成配方保留。把转换机放在传送带、置物台等处理位的**上方两格**，将普通机器物品送到下方，可逐个转换为对应下界合金版本。支持机器和两级扳手升级，烈焰人燃烧室的空壳和已有烈焰人版本分别转换。

转换机固定占用 **2048SU**，每个物品需要连续供能 **5秒（100tick）**；提高转速不会加速，断电或过载会重新计时。每次完成消耗 **250mB下界精华** 和配方所需的下界合金值，未完成不扣这些资源。内置精华槽4000mB，可用管道或桶注入；不接受岩浆等其他流体。

右键投入一张下界合金板，会立即消耗板材并存入 **16点下界合金值**；原版下界合金升级模板兑换 **1点**。普通机器升级每件消耗 **1点**，下界合金扳手升级为下界合金下界合金扳手消耗 **16点**；均需每件250mB精华。可通过自动化批量投入，空手右键查看点数，工程师护目镜显示点数、精华与进度。已兑换的板材不能取回；旧版部分使用的板材只兑换其剩余点数。拆除或用扳手拾取时，点数随转换机物品保存，重新放置后恢复。暂不制作转换动作动画。

**思索教程：** 悬停下界合金转换机并按住默认 `W`，查看“获取与查看下界合金值”“转换普通机器”“制作两级扳手”三段用法动画。覆盖板材和模板投入、动力与精华准备、普通机器升级、基础扳手两种制作方式及16点进阶升级，支持中英文字幕。

转换机的有序合成：

| 下界合金锭 | 下界之星 | 下界合金锭 |
| --- | --- | --- |
| 龙息 | 附魔台 | 龙息 |
| 黑曜石 | 钻石块 | 哭泣的黑曜石 |

下界精华只能由**下界合金动力搅拌机**在工作盆中制作：**1烈焰粉＋1地狱疣＋1000mB岩浆→1000mB下界精华**，无需额外热源。普通动力搅拌机不能制作。

# 下界合金弹射置物台

保持平放，可自由选择斜向及不同高度的目标，也可纯向上或向下弹射。水平直线射程和垂直高度差上限均为 Create 弹射距离配置的 `2` 倍，默认 `64` 格；斜向按水平直线距离判定。相同转速和距离下，蓄力进度为原版的 `8` 倍，蓄力阶段约为原来的 `1/8`，仍受整 tick 和原版弹射动作时间影响；应力消耗沿用原版。

手持下界合金弹射置物台，`Shift + 右键`目标方块，再正常放置。选点后会预览轨迹，重复选中同一目标可清除。手持已选目标的置物台右键已有下界合金置物台，可以重新设定且不消耗手中物品；在途物品抵达后才能改向。工程师扳手可旋转目标，悬停时显示现有轨迹。保留原生数量设置、库存和红石暂停，实体与物品均可弹射，实体方块会阻挡物品轨迹。

一个下界合金板加一个原版弹射置物台，无序合成一个升级品。物品具备防火、防爆。

**思索教程：** 悬停下界合金弹射置物台并按住默认 `W`，查看“选定目标与弹射物品”和“改向、数量与红石控制”两段用法动画，支持中英文字幕。

# 下界合金伸缩机械手

比 Create 原版伸缩机械手额外增加 `3` 格方块触及距离：单持总加成 `6` 格，混合双持 `8` 格，两只升级品双持 `11` 格。保留原生伸缩动作与远距实体交互。

副手持有新机械手、主手持空桶，生存玩家右键原版或下界合金流体储罐即可装液体，每桶抽取 `1000 mB`。主手持新机械手，对工作方块长按右键可直接供能，松开或移开视线即停止；按住 Shift 开始可反向转动。仅驱动点击方块，沿用原生加工所需转速条件。默认 `16 RPM`、`256 SU`，容量跟随强力机械泵的基础应力配置，恰好够一台强力泵工作；超过此需求的方块不能启动，已有动力的方块仍使用其原有动力。

耐久上限沿用原版的 `200`。成功取一桶扣一次资源，供能开始扣一次，随后每 `20 tick` 扣一次；优先消耗已装备背罐的压缩空气，没有空气时消耗 `1` 点耐久。普通远距破坏、放置和攻击继续沿用 Create 的消耗规则与配置。下界合金板加原版伸缩机械手无序升级；防火、防爆。

**思索教程：** 悬停下界合金伸缩机械手并按住默认 `W`，查看“副手辅助装液体”和“主手给工作方块供能”两段用法动画，支持中英文字幕。

# 下界合金机械手（加工）

下界合金机械手（`netherite_deployer`）在一次伸出动作中，批量加工传送带或置物台上当前同组物品。逐件应用当前配方、消耗对应材料或工具耐久，并返还容器；材料耗尽、工具损坏或不再匹配时停止，保留未加工输入。支持普通机械手配方、砂纸打磨和序列组装，每件只推进当前一个步骤，产物不会在同一次动作内被递归加工。置物台输出缓存放不下时，沿用原版掉落剩余产物的规则。

动作速度和应力消耗沿用原版；加速来自批量处理，不是固定动作倍率。筛选、红石、使用/攻击模式、动力臂给料和移动结构交互保留原生用法；世界放置、攻击、挖掘仍按原版单次动作执行。下界合金板加原版机械手无序升级，具备防火、防爆与原生 Shift 概要。

原有负责搬运的 `netherite_mechanical_arm` 按官方名称显示为“下界合金动力臂”，原ID、性能与存档保持不变。

# 下界合金下界合金

用一个下界合金锭和一个下界合金板无序合成。按住右键可持续播放食用动作，满饱食度也能使用；物品不会被吃掉，也不会增加饱食度、饱和度或生命值，松开即停。

连续使用**超过10秒**（超过200个游戏tick）可获得紫色挑战进度**“你的牙还好吗？”**；松开或换物品后重新计时，短按不累计。支持主手与副手，获得进度后仍可继续啃。原生 Shift 概要为：**“你为什么要做这个？”**。沿用下界合金物品的防火、防爆属性。

# 下界合金动力锯

向上放置时，_一个单件加工周期_即可处理整叠物品，不随输入数量延长加工时间。保留原版过滤、石切配方开关、序列组装逐步推进、概率产物、容器余料与输出行为。

横放砍树的计算速度为原版 **2倍**，固定放置和装配到运动结构后的伤害均为原版 **1.8倍**，应力消耗沿用原版。砍树存在整数tick与整树队列，因此实际耗时不保证严格减半。

一个下界合金板加一个原版动力锯无序升级，也可在下界合金转换机中转换。将它装入下界合金下界合金扳手并激活后，长按右键持续锯击攻击距离内最近的前方目标；按256RPM计算为每次 **18点伤害**，沿用原生受伤间隔、队伍和PVP规则，墙体阻挡，松开即停。扳手最多装8个模块，初始激活1个，可在装配台升级为同时激活2个。

# 下界合金扳手

**原版Create扳手＋下界合金板** 无序合成，或将原版扳手放到下界合金转换机下方，消耗 **1点下界合金值＋250mB下界精华** 制作。主手持有时 **方块触及距离+2**，实体攻击距离不变，保留原版扳手功能；无耐久损耗，掉落物防火、防爆。基础扳手不使用模块轮盘。

# 下界合金下界合金扳手

将 **下界合金扳手** 放到转换机下方，消耗 **16点下界合金值＋250mB下界精华**，连续供能5秒即可制成。基础伤害 **10**（下界合金剑+2）、攻速 **1.6**，无耐久损耗，保留主手 **方块触及距离+2** 和 Create 扳手旋转、拆卸功能。

先将扳手放入 **装配台** 中央槽，再往周围 **8个真实槽位** 装入下列工作方块（每槽一个），也可取出；取出已激活的模块立即停用，模块物品组件和储液随扳手保存。装配台无需动力，用 **工作台＋安山机壳＋下界合金板** 无序合成，拆除会返还内部扳手和剩余模板。

主手持有扳手，**按住 X** 显示工具箱风格轮盘，点击区域切换该模块的激活状态，松开关闭；按键可在设置中修改。初始 **1个激活位**，开启另一模块会关闭原模块；在装配台放入一张原版 **下界合金升级模板** 并点击升级，可永久解锁 **第2个激活位**。已激活两个时再开启一个，会关闭最早激活者。两个模块的被动效果共同生效，同种效果不重复叠加；**最近激活的右键模块优先**，激活被动模块不会抢占右键。轮盘中的数字表示激活顺序，“右键”标记当前右键模块。

| 模块 | 激活后的用法 |
| --- | --- |
| 强力机械泵 | 伤害+6、实体攻击距离+1、击退+2级。 |
| 下界合金鼓风机 | 长按右键吹动实体，按下Shift切换吸/吹。以256RPM及下界合金风速倍率计算，实体受方块遮挡。 |
| 下界合金弹射置物台 | Shift显示连续轨迹；Shift＋长按右键蓄力，松开发射，1秒蓄满，最大目标距离64格。视角和蓄力时长实时调整轨迹；普通右键或蓄力不足不发射。无阻挡落地免摔，途中碰撞恢复正常摔伤。 |
| 下界合金烈焰人燃烧室 | 持有者造成的火焰伤害4倍，包括火焰附加造成的后续燃烧。 |
| 下界合金动力冲压机 | 一次实际攻击的总伤害1.5倍，并定身1秒。 |
| 下界合金动力锯 | 长按右键锯击攻击距离内最近的前方目标，每次18点伤害，沿用原生受伤间隔；墙体阻挡。 |
| 下界合金蒸汽引擎 | 长按右键直接驱动点击的单个工作方块；默认16RPM/256SU，恰好一台强力机械泵，不耗空气或耐久，松开即停。 |
| 下界合金转换机 | 右键生物使其永久获得抗性I、盔甲+3，不叠加；死亡必掉一张原版下界合金升级模板。 |
| 下界合金流体储罐 | 右键收纳一桶流体源；按Shift打开流体轮盘，选液后Shift+右键放一桶。不同流体合计最多64桶，卸下模块仍保留储液。 |

# 防爆与防火

上述全部物品（含强力机械泵和下界合金板）的掉落物均免疫爆炸伤害；十四种方块放置后均不被爆炸破坏，储罐与保险库中的内容也因此保留。方块基础爆炸抗性统一为 `1200`。

全部十九种物品也具有下界合金物品的防火属性，掉落在火焰或岩浆中不会被烧毁。

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
build/libs/createnetherite-1.21.1-0.3.21.jar
```

# Introduction

**[Create-Netherite-Upgrades](https://github.com/von-Neumann101/Create-Netherite-Upgrades)** is a Create addon for Minecraft 1.21.1 on NeoForge.

Current branch: `1.21.1-NeoForge`

Current mod version: `0.3.21`

# Maintenance status

Since 2026-10-29, development primarily targets Minecraft `1.21.1` on the `1.21.1-NeoForge` branch. Features, fixes, and dependency updates focus on this branch.

Updates to the Minecraft `1.20.1` Forge `main` branch are paused. Its code and historical technical documentation remain available. The two versions have feature differences, and new content will not be synchronized while Forge updates are paused.

Technical documentation (Chinese): [NeoForge 1.21.1](docs/neoforge-1.21.1.md) · [Forge 1.20.1 (historical reference)](docs/forge-1.20.1.md)

# Content

The mod currently adds several Create-compatible entries:

- `createnetherite:powerful_mechanical_pump`
- `createnetherite:netherite_converter`
- `createnetherite:nether_essence_bucket`
- `createnetherite:netherite_fluid_tank`
- `createnetherite:netherite_steam_engine`
- `createnetherite:netherite_item_vault`
- `createnetherite:netherite_elevator_pulley`
- `createnetherite:netherite_mechanical_press`
- `createnetherite:netherite_mechanical_saw`
- `createnetherite:netherite_mechanical_mixer`
- `createnetherite:netherite_mechanical_arm`
- `createnetherite:netherite_blaze_burner`
- `createnetherite:netherite_encased_fan`
- `createnetherite:netherite_filter` (legacy / 旧存档兼容)
- `createnetherite:netherite_wireless_terminal`
- `createnetherite:netherite_infinite_manager` (portable)
- `createnetherite:netherite_wireless_manager` (block)
- `createnetherite:netherite_smart_chute`
- `createnetherite:netherite_weighted_ejector`
- `createnetherite:netherite_extendo_grip`
- `createnetherite:netherite_deployer`
- `createnetherite:netherite_netherite`
- `createnetherite:netherite_wrench`
- `createnetherite:netherite_netherite_wrench`
- `createnetherite:assembly_table`
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

# Netherite Fan

The Netherite Encased Fan provides built-in Washing, Blasting, Haunting, and Smoking without requiring media in front of it. It processes dropped items, belt items, and depot items using Create recipes. Defaults remain `4` times the normal processing progress and `2` times the air-current speed calculation, with native airflow range and kinetic RPM.

1. Hold an original Create filter and right-click to configure its list. Original list, attribute, and other native Create filter types are supported.
2. Right-click the fan and install filters in the four slots below the Washing, Blasting, Haunting, and Smoking buttons. **Each slot holds one real original filter; ordinary items and the dedicated Netherite Filter are rejected.** Normal inventory actions and Shift transfer work. Remove a filter to edit it.
3. Click a processing button to choose the unmatched-item mode. **Matched items use their slot's fixed processing type; unmatched items use the selected mode.** This remains selectable with all four filters installed.

For example, put a list containing raw iron and raw gold in the Blasting slot and select Washing as the fallback. Both ores are blasted; other items attempt washing. Overlaps resolve in Washing, Blasting, Haunting, Smoking order, skipping empty slots. Native allow/deny lists, component matching, and nested filters retain their behavior.

Craft a Netherite Sheet with a regular Encased Fan to upgrade it. Breaking returns the real installed filters with their configurations.

**Legacy saves:** The dedicated Netherite Filter has no crafting recipe, creative-tab entry, or Ponder entry. Its registration and configuration viewer remain for old saves. An old installed filter is returned when opening the new fan menu, or dropped if inventory space is unavailable; breaking an unopened old fan also returns the original item. Migration creates no extra native filters. The four new slots start empty and processing uses the selected mode until you install original Create filters.

**Ponder:** Hover the fan and hold the default `W` key for two usage tutorials covering built-in modes, original filters in four slots, and selected-mode fallback. English and Chinese captions are available.

**Descriptions and GUI:** The converter, fan, ejector and extendo grip have native Ponder entries; items without Ponder use native Shift summaries. GUI PNGs are under `assets/createnetherite/textures/gui/` inside the jar and can be replaced by resource packs at matching paths and dimensions; text and items remain game-rendered.

# Wireless Redstone

The Netherite Wireless Redstone Terminal mounts on floors, walls, and ceilings according to the clicked face. Floor and ceiling placement follows the player's horizontal direction. It starts private. Its owner can right-click to name it or make it public. Names display as “placing player's name + terminal name”; the manager also shows coordinates and dimension. Duplicate display names remain separate targets.

The **Netherite Wireless Manager** is a placeable block that needs **no rotational power**. Right-click to open the shared management interface. Each user sees their own terminals and public terminals, even when using the same block. Stay within eight blocks and in the same dimension while using it; removal or leaving invalidates the menu. Target terminals can still be activated across dimensions and at any distance.

Shaped block recipe:

| Eye of Ender | Transmitter | Nether Wart |
| --- | --- | --- |
| Brass Casing | Netherite Ingot | Brass Casing |
| Obsidian | Crying Obsidian | Obsidian |

The portable version keeps its previous item ID and recipe, so existing items remain usable.

Right-click while holding a **Portable Netherite Wireless Manager** to list your terminals and public terminals placed by others. Click rows to select, keep selections across pages, select the current page, clear selections, or activate selected terminals together. You can also edit your own terminals remotely.

No frequencies are needed. Terminals work across dimensions at any distance, including unloaded chunks. Each activation emits a strength-`15` signal for `1 second` and keeps the target chunk ticking for `15 seconds`. Sending again renews both deadlines, measured as `20 / 300 game ticks`. Listing terminals does not load their chunks. Breaking unregisters a terminal; placing it again creates a new identity.

Craft a Netherite Sheet with a Create Redstone Link (`create:redstone_link`) for the terminal, or with a Linked Controller (`create:linked_controller`) for the manager. Both recipes are shapeless.

# Netherite Smart Chute

Routes items up or down without a fan. The native filter on the casing controls admission, including Create filters, attribute filters, and extraction counts. Redstone pauses transport.

Right-click empty-handed to configure the Up and Down sample slots. Drag an item or a configured original Create filter into a slot, or hover an inventory item and press `1 / 2`. Samples consume no items; right-click or click with an empty cursor to clear. Use an original filter to match multiple item types. Up rules win overlaps; unmatched items use the selected default direction, initially Down.

Each direction buffers one stack independently, allowing opposing traffic in a column. Configure each segment for its intended routes. Blocked outputs retain items, and saves retain buffers and rules. Buffered items finish their original direction. Sneak-right-click empty-handed to retrieve buffers; breaking drops real buffered items without creating sample items. Connected original chutes keep their own airflow direction and redstone control.

Craft one Netherite Sheet with an original Smart Chute in a shapeless recipe. The upgrade has fire and blast protection, a native Shift summary, and a separate `netherite_smart_chute.png` GUI texture.

# Netherite Converter and Nether Essence

An additional upgrade route; existing crafting recipes remain. Place the converter **two blocks above** a belt, depot or compatible processing inventory. It upgrades machines and both wrench tiers one item at a time, with separate empty and occupied Blaze Burner recipes.

The converter uses a fixed **2048 SU** and **5 continuous seconds (100 ticks)** per item. Higher RPM does not speed it up; power loss or overstress restarts progress. Completion costs **250 mB of Nether Essence** and the recipe's Netherite Value. Unfinished work spends neither resource. Its 4000 mB tank accepts essence through pipes or buckets and rejects other fluids.

Right-click to immediately redeem a Netherite Sheet for **16 Netherite Value**, or a vanilla Netherite Upgrade Smithing Template for **1 value**. Ordinary upgrades cost **1 value**; upgrading a Netherite Wrench to a Netherite Netherite Wrench costs **16 value**. Each still uses 250 mB essence. Automation can deposit stacks. Right-click empty-handed to view value; Engineer's Goggles show value, fluid and progress. Redeemed items cannot be retrieved; legacy partially used sheets add only their remaining value. Breaking or wrench pickup stores value on the converter item, and placing it restores that value. The machine currently has no conversion animation.

**Ponder tutorials:** Hover the Netherite Converter and hold the default `W` key for “Getting and checking Netherite Value”, “Converting a machine”, and “Making both wrench tiers”. These usage scenes cover deposits, power and essence, ordinary upgrades, both basic-wrench methods, and the 16-value advanced upgrade, with English and Chinese captions.

Shaped crafting recipe:

| Netherite Ingot | Nether Star | Netherite Ingot |
| --- | --- | --- |
| Dragon's Breath | Enchanting Table | Dragon's Breath |
| Obsidian | Diamond Block | Crying Obsidian |

Only a **Netherite Mechanical Mixer** can produce essence in a basin: **1 Blaze Powder + 1 Nether Wart + 1000 mB Lava → 1000 mB Nether Essence**. No extra heat source is required. A regular mixer cannot make it.

# Netherite Weighted Ejector

Stays flat while targeting diagonally, at different heights, or straight up/down. Its horizontal radius and vertical offset limits are twice Create's ejector distance setting, `64` blocks by default; diagonals use horizontal straight-line distance. At matching speed and distance, charge progress is `8` times the original, making the charging phase about `1/8` as long, subject to whole ticks and the original launch animation. Stress consumption follows the original.

Hold the upgraded ejector, `Shift + right-click` a target block, then place normally. A trajectory preview appears after selection; select the same target again to clear it. Right-click an existing upgraded ejector with a target selected to retarget it without consuming the held item; wait for flying items to arrive before changing direction. Wrench rotation rotates the target, and hovering with a wrench previews the current trajectory. Native stack-size settings, inventory, and redstone pause remain available. Both entities and items can launch; solid blocks obstruct item trajectories.

Craft one Netherite Sheet with an original Weighted Ejector in a shapeless recipe. The upgrade has fire and blast protection.

**Ponder tutorials:** Hover the Netherite Weighted Ejector and hold the default `W` key for “Selecting a target and launching items” and “Retargeting, stack size and redstone”. Both usage scenes have English and Chinese subtitles.

# Netherite Extendo Grip

Adds `3` more blocks of block reach than the original Create Extendo Grip: `6` bonus blocks with one upgrade, `8` with a mixed pair, and `11` with two upgrades. Keeps native extending animations and long-range entity interactions.

With the upgrade in the offhand and an empty bucket in the main hand, survival players can right-click an original or Netherite Fluid Tank to fill a bucket, draining `1000 mB`. In the main hand, hold right-click on a working block to power it directly; release or look away to stop. Sneak when starting to reverse rotation. Only the clicked block is driven, and its native minimum speed still applies. Default output is `16 RPM` and `256 SU`; capacity follows the Powerful Mechanical Pump's stress setting, exactly enough for one pump. Blocks requiring more cannot start, and existing mechanical power remains in control.

Keeps the original `200` durability. A successful bucket fill costs one action; hand power costs one action initially and every `20 ticks` afterward. Equipped backtank air is used first, then `1` durability without air. Native remote breaking, placement, and attacks keep Create's cost rules and settings. Craft one Netherite Sheet with an original Extendo Grip in a shapeless recipe. Includes fire and blast protection.

**Ponder tutorials:** Hover the Netherite Extendo Grip and hold the default `W` key for “Filling buckets with an offhand grip” and “Powering a working block by hand”. Both usage scenes have English and Chinese subtitles.

# Netherite Deployer

The Netherite Deployer (`netherite_deployer`) batch-processes the current stack on a belt or depot in one extending action. Each input uses the current recipe, consumes its application material or tool durability, and returns its container. Stops when supplies or tools run out or matching changes, preserving unprocessed inputs. Supports normal deploying, sandpaper polishing, and sequenced assembly; each input advances only one step, and outputs are not recursively processed in the same action. Excess depot outputs follow the original drop behavior when its output buffer is full.

Movement speed and stress consumption match the original; acceleration comes from batching rather than a fixed animation multiplier. Native filters, redstone, use/punch modes, Mechanical Arm feeding, and moving-contraption interactions remain available. World placement, attacks, and mining still perform one native action. Craft one Netherite Sheet with an original Deployer in a shapeless recipe. Includes fire and blast protection and a native Shift summary.

The existing item-moving `netherite_mechanical_arm` keeps its ID, behavior, and saved data; its Chinese name is corrected to match Create's Mechanical Arm terminology.

# Netherite Netherite

Craft one Netherite Ingot with one Netherite Sheet in a shapeless recipe. Hold right-click to keep playing the eating animation, even at full hunger. The item is never consumed and grants no hunger, saturation, or health; release to stop.

Using it continuously for **more than 10 seconds** (over 200 game ticks) grants the purple challenge **“Are your teeth okay?”**. Releasing or switching items resets the timer; separate short uses do not accumulate. Works in either hand and keeps chewing after the challenge is earned. Its native Shift summary is **“Why would you make this?”**. Includes fire and blast protection.

# Netherite Mechanical Saw

Facing upward, it processes an entire stack in **one single-item processing cycle**, without increasing duration with input count. Keeps native filtering, the stonecutting recipe option, per-step sequenced assembly, random outputs, crafting remainders and output handling.

Tree-cutting calculation speed is **2 times** the original. Damage is **1.8 times** the original, both stationary and mounted on contraptions. Stress consumption is unchanged. Integer ticks and the tree-breaking queue mean actual cutting time is not always exactly halved.

Upgrade one original Mechanical Saw with one Netherite Sheet in a shapeless recipe, or use the Netherite Converter. Install and activate it in the Netherite Netherite Wrench, then hold right-click to saw the nearest target ahead within attack reach for **18 damage** at a 256 RPM reference. Native hurt cooldown, teams, PVP and solid obstacles apply; releasing stops it. The wrench holds at most eight modules, with one active initially and two after upgrading at an Assembly Table.

# Netherite Wrench

Craft shapelessly from a **Create Wrench + Netherite Sheet**, or convert a Create Wrench for **1 Netherite Value + 250 mB Nether Essence**. Adds **2 blocks of block interaction reach** in the main hand without changing entity attack reach. Retains the original wrench functions, with no durability, fireproof and explosion-proof drops. The basic wrench does not use the module wheel.

# Netherite Netherite Wrench

Convert a **Netherite Wrench** using **16 Netherite Value + 250 mB Nether Essence** with five seconds of uninterrupted power. Base attack damage is **10** (Netherite Sword +2), attack speed **1.6**, with no durability loss. Retains **2 extra blocks of main-hand block reach** and Create wrench rotation and dismantling.

Put the wrench in the center slot of an **Assembly Table**, then install or remove the listed blocks in its **eight real module slots**, one per slot. Removing an active module disables it; item components and fluids stay with the wrench. The table needs no power and is crafted shapelessly from a **Crafting Table + Andesite Casing + Netherite Sheet**. Breaking it returns the stored wrench and remaining templates.

With the wrench in the main hand, **hold X** to show the toolbox-style wheel. Click a region to toggle its activation; release to close. Rebind the key in settings. Starts with **one active slot**, so enabling another replaces it. Insert one vanilla **Netherite Upgrade Smithing Template** at the table and click Upgrade to permanently unlock the **second active slot**. Enabling a third disables the oldest activation. Both active modules retain their passive effects; identical effects do not stack. The **most recently activated right-click module takes priority**; passive modules do not take over right-click. Numbers show activation order, and the Right-click badge marks the current action.

| Module | Active ability |
| --- | --- |
| Powerful Mechanical Pump | +6 attack damage, +1 entity attack reach, +2 knockback levels. |
| Netherite Fan | Hold right-click for airflow; press Shift to toggle push/pull. Uses a 256 RPM reference and the Netherite wind-speed multiplier. Solid blocks obstruct the current. |
| Netherite Weighted Ejector | Shift previews a continuous arc. Hold Shift+right-click to charge, then release to launch; full charge takes 1 second, up to 64 blocks. View and charge time update the arc. Ordinary right-click and insufficient charge do not launch. Clear landings prevent fall damage; collisions restore it. |
| Netherite Blaze Burner | Your fire damage is multiplied by four, including subsequent Fire Aspect burning. |
| Netherite Mechanical Press | Total actual attack damage is multiplied by 1.5; target is rooted for one second. |
| Netherite Mechanical Saw | Hold right-click to saw the nearest target ahead within attack reach for 18 damage, respecting native hurt cooldown and solid obstacles. |
| Netherite Steam Engine | Hold right-click to power only the clicked work block: default 16 RPM/256 SU, exactly enough for one Powerful Pump, without air or durability costs. Release to stop. |
| Netherite Converter | Right-click a creature for permanent Resistance I and +3 armor, without stacking. Death guarantees one vanilla Netherite Upgrade Smithing Template. |
| Netherite Fluid Tank | Right-click collects one source bucket; Shift opens fluid selection, then Shift+right-click places one bucket. All types share 64 buckets total. Removing the module preserves stored fluids. |

# Blast and Fire Protection

All items listed above, including the Powerful Mechanical Pump and Netherite Sheet, are immune to explosion damage when dropped. All fourteen placed blocks are protected from explosion destruction, preserving tank and vault contents. Their base blast resistance is `1200`.

All nineteen items also have the fire resistance of netherite items: dropped items cannot be burned by fire or lava.

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
build/libs/createnetherite-1.21.1-0.3.21.jar
```
