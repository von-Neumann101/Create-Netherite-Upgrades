# 扳手装配台与按住轮盘实施计划

**用户规格与确认：** 新增无需动力的装配台，放入扳手才能装卸最多8个真实工作方块；手持扳手按住可重绑定的X显示工具箱风格轮盘，只能切换激活状态，松开关闭且不自动选中。初始1激活位，装配台消耗一张原版下界合金升级模板永久解锁2位。点击已激活槽关闭；点击未激活槽开启，超额时关闭最早激活的槽。两个右键模块由最近激活者使用右键，被动效果按各自原有触发规则共同生效。同类型能力不重复叠加。

装配台采用工作台+安山机壳+下界合金板无序合成（维护者选定的常规配方）。物品与内部扳手/模板保存、拆除返还；装配数据仍在扳手组件内，旧单激活数据保留。UI不需要应力、频率或外部配方。旧手持装卸入口关闭。

## 公共接口与文件职责

- 根代理：WrenchData、WrenchModule、WrenchWheelNetworking、旧WrenchNetworking退役OPEN、被动效果membership接入、所有注册/资源/语言/版本/文档、统一构建。
- 装配台任务：content/equipment/wrench/AssemblyTableBlock.java、AssemblyTableBlockEntity.java，重写既有WrenchMenu为装配台菜单（保留注册ID netherite_wrench及POSITIONS供流体轮盘）；改WrenchScreen显示装配，不再提供激活按钮。专用AssemblyTableGameTest及迁移旧WrenchInventoryGameTest的手持菜单测试。
- 客户端轮盘任务：新WrenchRadialScreen.java及WrenchClient按住键打开/松开关闭，不改图形渲染或其它模块按键；复用Create原生工具箱槽框，不新增轮盘PNG。

WrenchData接口：`activeSlots(ItemStack):List<Integer>`按旧到新排序；`activationLimit(ItemStack):int`；`unlockSecondSlot(ItemStack):boolean`；`toggleSlot(ItemStack,int):boolean`；`rightClickSlot(ItemStack):int`；`rightClickModule(ItemStack):WrenchModule`；`has(ItemStack,WrenchModule)`和`has(Player,WrenchModule)`判任一已激活模块；旧activeSlot返回最新激活槽，setActiveSlot仍重置到一个槽供兼容，active优先返回rightClickModule，无右键模块时返回最新模块。

WrenchModule增加`usesRightClick()`：FAN/EJECTOR/ENGINE/CONVERTER/TANK/SAW为true，PUMP/BURNER/PRESS为false。WrenchData保留旧Active值迁移，新的ActiveSlots数组和ActivationLimit保存在原NetheriteWrench自定义数据内；移除模块后对应激活项清除。保留流体和其它未知组件。

装配台BE接口：构造(BlockPos,BlockState)，`getInventory():ItemStackHandler`两槽（0扳手，1升级模板），`getModules():ItemStackHandler`8槽全局实时镜像，`saveModules()`提交到当前扳手组件，`upgrade():boolean`原子消耗一模板并解锁。模块缓存仅为扳手视图，拆除只掉两槽真实库存，不能再掉8个镜像。WrenchMenu(int,Inventory,RegistryFriendlyByteBuf)读取BlockPos；server构造(int,Inventory,AssemblyTableBlockEntity)。Menu.slot顺序为扳手0/模板1/模块2..9/玩家10..45。Wrench槽内部(136,80)、模板(238,80)，八模块POSITIONS及背包位置沿用旧界面。升级button id0，服务器验证BE身份/维度/距离≤8后执行；无手持源槽锁。

轮盘协议：`WrenchWheelNetworking.Action(UUID session,int action,int slot)`，OPEN=0/TOGGLE=1/CLOSE=2。`State(UUID session,int hotbar,List<ItemStack> modules,List<Integer> activeSlots,int activationLimit,int rightClickSlot)`，activationLimit=0代表失效关闭。`clientState()`返回最新客户端快照。客户端按键打开立即创建屏幕并发送OPEN，未收到同session有效State前禁止点击；服务器绑定真实Player/当前主手ItemStack引用与快捷栏，TOGGLE时重新校验并仅从服务器库存读模块。模块列表固定8个，激活列表最多2个。屏幕保持直到松键，左/右点击目标区域只发TOGGLE，不装卸物品；松开/CLOSE不切换。状态更新替换快照不替换屏幕，以支持按住时多次点击。旧WrenchNetworking.Action.OPEN不再打开任何菜单。

## 验证与发布

- [x] 新注册或激活规则失败基线，保存旧94项测试中仍有效的行为。
- [x] 单激活互斥、双激活FIFO、关闭/重新开启顺序、模板一次解锁、同类型不叠加、旧数据及保存重载。
- [x] 实际装卸/Shift转移、空台拒绝模块、槽上限、拿出扳手/拆台/多人查看不复制或丢失、越界/替换BE/跨维度后的旧菜单操作拒绝。
- [x] 轮盘原生按住键/点击/松开逻辑、会话源对象与旧包拒绝，双模块被动和右键优先切换，停止旧持续能力。
- [x] 图形资源由指定美术对话提供，只新增装配台top/front两PNG和288×286的46槽GUI；旧72PNG保留。未启动客户端、未使用RTX5060。
- [x] 版本0.3.19，完整离线build及服务器GameTests、正式jar内容/元数据核对、独立复核后自动commit/push。

完成验证：205项回归及106项服务器GameTest通过；266资源、197类逐字节核对，188JSON、中英文各178键，旧72PNG保留。轮盘按住/释放/分区为源码核对，未启动客户端或使用RTX5060；实际GUI键鼠显示待图形实测。
