# Agent 须知

本仓库是 GregTech Odyssey 整合包维护的 EMI 分支（EMI GTO Edition），唯一分支 `1.20.1`，以上游 EMI `1.1.24+1.20.1` 为基础。

- 任何改变游戏设计的修改（界面、交互、功能、配置项、显示与反馈等），必须在同一次提交中更新根目录的 [GTO_CHANGES.md](GTO_CHANGES.md)，记录方式见该文档的"维护约定"。GTO_CHANGES.md 只记录玩家可感知的差异，不记录实现方式。
- 模组 ID 保持 `emi`（内置的 EMI Trades 保持 `emitrades`），不得改动 Java 包名 `dev.emi.emi`、`io.github.prismwork.emitrades` 与资源命名空间，以免破坏现有 EMI 插件与存档数据。
- 本模组不得强依赖应用能源、GregTech、GTOCore、GTOLib 等整合包模组。需要它们参与的行为，开放成通用接口，由对方单向依赖本模组来实现：
  - 物品与流体连同 NBT 的驻留键由 `dev.emi.emi.stack.StackKeys` 统一创建，`EmiStack.getStackKey()` 返回它，内容相同即同一对象，用 `==` 比较。装有应用能源时直接用应用能源的 `AEItemKey` / `AEFluidKey`（只在编译期引入，`AeStackKeys` 只在装有时加载），与应用能源共用一套键；没装时才用自带的 `EmiItemKey` / `EmiFluidKey`，挂在物品与流体上的 mixin 也由 `StackKeyMixinPlugin` 在装有应用能源时跳过。联动方拿到键后用 `ItemEmiStack.ofKey` / `FluidEmiStack.ofKey` 包回 EMI 栈。
  - `dev.emi.emi.api.EmiHooks`：数量渲染、包装了流体的物品解析、收藏变动回调，在客户端初始化时注册一次。
  - 应用能源的联动全部写在应用能源 GTO 分支的 `appeng.integration.modules.emi`，入口 `AppEngEmiHooks.register()` 与 `AppEngEmiPlugin`。
- 只维护 Forge 1.20.1 版，构建与运行用 JDK 21，Mojang 映射，构建插件为 MDG legacyforge。FastCollection 只在编译期引入，运行时由整合包里的 LeanObject 提供。
- 同一个 jar 里的 `emi` 与 `emitrades` 共用 Forge 的一个排序节点，二者之间的依赖不能写 `BEFORE`/`AFTER`，否则形成自环（`BundledEmiMetadataTest` 会检查）。
- 普通 JUnit 不会应用 mixin，也没有 FML 模组列表，此时按类路径上有没有应用能源选键；本仓库测试走自带的键，用到时 `mockStatic(EmiItemKey.class)` 或直接 `new EmiItemKey(...)`；引导 MC 用 `dev.emi.emi.TestBootstrap.init()`。
- 严格比较走资源的 `getStrictComparison()` / `isStrictEqual()`，比较和 hash 复用同一资源专属比较器。`COMPARE_NBT`、`Comparison.compareNbt()` 和 `EmiPort.compareStrict()` 仅保留为弃用的兼容入口，新调用不得使用；自定义资源不得靠临时序列化 NBT 匹配。
- 修改容器时按语义选型：注册对象、配方实例用 fastutil 引用容器，数值用原始类型容器，枚举用 `EnumMap` / `EnumSet`，复杂值键用 FastCollection 哈希缓存容器；内部声明保留具体类型，协议边界保留接口。不要用 Guava `Maps` / `Sets` / `Lists` 工厂隐式创建默认容器。有序容器必须保留顺序；需要同时保留插入顺序和哈希缓存时可用 `LinkedHashMap` / `LinkedHashSet`。

## 发布

- 用 `com.gto.gtopublishgradleplugin` 发布到 `https://gtodyssey.com/maven/releases`，坐标 `dev.emi:emi-forge-1.20.1:<版本>`（插件把 artifactId 改写成 `emi-forge-1.20.1`）。凭据从 `~/.gradle/gradle.properties` 的 `gtodysseyRepositoryUsername` / `gtodysseyRepositoryPassword` 读取，不得写入仓库。
- 版本号写在 `gradle.properties` 的 `gto_version`，格式 `x.y.z`，沿上游版本往后递增（首版 `1.1.25`），每次发布递增最后一段。
- 发布的是重混淆后的正式版 `reobfJar`（类里是 SRG 名，如 `m_41720_`），不是开发版 `jar`；发布后要检查 jar 里确实是 SRG 名。
- 本地联调用 `./gradlew publishToMavenLocal`，应用能源与 GTOCore 的 `emi_version` 改成同一个版本号即可。正式发布用 `./gradlew gtoValidate`，确认无误后 `./gradlew gtoPublishMaven`。
- 整合包统一发布时，本仓库有改动就要先于应用能源、GTOCore 发布，并同步更新它们的 `emi_version`。`mods.toml` 里对 `ae2` 的可选前置写了最低版本，改动键的交接方式时两边一起提版本。
- CurseForge 项目 ID 1760584，`gtoPublish` 同时发布到 Maven 与 CurseForge；前置为 Lean Object（运行时提供 FastCollection），与官方 EMI、EMI Trades 不兼容，JEI 可选。
