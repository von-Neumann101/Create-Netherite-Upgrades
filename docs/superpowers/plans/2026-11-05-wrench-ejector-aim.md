# 扳手弹射蓄力与轨迹修复计划

用户最终纠正：普通右键不能弹射；只有Shift+右键蓄力，视角决定方向与仰角，松开右键后才发射；20tick蓄满、最大目标距离64格。轨迹由散点粒子改为连续高对比线，明确标出障碍碰撞位置。既有碰撞后摔伤、无阻挡首次落地保护及重生/维度清理保留。

- [x] 沿真实原生useItem和handleMovePlayer入口验证普通右键禁止发射、蓄力松开后发射；不添加宽泛移速豁免。
- [x] 原生持续使用状态承载蓄力：开始记录是否蓄力，20tick采用BowItem功率曲线；服务器从剩余使用tick计算强度，松开按最终视角发射，失活或换手取消。普通右键拒绝，弓功率低于0.1（不足3tick）松开也不发射。
- [x] 轨迹计算增加power参数，目标距离随蓄力变化；预览与发射使用同一解。预览碰撞与运动分开，不把瞄准点吸附成固定落点。
- [x] 新增WrenchEjectorPreview客户端连续线与落点/碰撞标记、蓄力百分比；不新增贴图，不修改原有实体弹射置物台的选点操作。
- [x] 完整build的205项回归与94项GameTest通过；IdentityHashMap隔离逻辑两端同ID玩家、硬实体碰撞、大坐标双精度均有回归。版本0.3.18，README/技术文档同步、jar逐字节核对通过，随后按项目约定提交推送。

公共接口：`WrenchLaunch.trajectory(Player,float power)`返回原有Trajectory；`predict(Player,float power):Preview`，Preview含`List<Vec3> points, Vec3 target, boolean blocked`（feet坐标）；`isCharging(Player)`、`chargeTicks(Player)`、`powerForTime(int)`供客户端预览读取。原有trajectory(Player)/start(Player)/preview(Player)保留满额兼容。客户端Shift或已开始蓄力时渲染，图形代码由独立任务编写，根代理负责物品/物理/网络和统一构建。

限制：禁止启动客户端或使用RTX5060，真实LocalPlayer接收和图形画面不因服务端模拟测试而宣称已实测。
