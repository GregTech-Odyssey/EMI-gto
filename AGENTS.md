# Agent 须知

本仓库是 GregTech Odyssey 整合包维护的 EMI 分支（EMI GTO Edition），唯一分支 `1.20.1`，以上游 EMI `1.1.24+1.20.1` 为基础。

- 任何改变游戏设计的修改（界面、交互、功能、配置项、显示与反馈等），必须在同一次提交中更新根目录的 [GTO_CHANGES.md](GTO_CHANGES.md)，记录方式见该文档的"维护约定"。GTO_CHANGES.md 只记录玩家可感知的差异，不记录实现方式。
- 模组 ID 保持 `emi`（内置的 EMI Trades 保持 `emitrades`），不得改动 Java 包名 `dev.emi.emi`、`io.github.prismwork.emitrades` 与资源命名空间，以免破坏现有 EMI 插件与存档数据。
- 本模组不得依赖应用能源、GregTech、GTOCore、GTOLib 等整合包模组。需要它们参与的行为，开放成通用接口，由对方单向依赖本模组来实现：
  - `dev.emi.emi.api.stack.EmiStackKey`（`EmiItemKey` / `EmiFluidKey`）：物品与流体连同 NBT 的驻留键，内容相同即同一对象，用 `==` 比较；`setAttachment` 槽位留给联动方缓存自己的键（应用能源在这里缓存 AEKey）。
  - `dev.emi.emi.api.EmiHooks`：数量渲染、包装了流体的物品解析、收藏变动回调，在客户端初始化时注册一次。
  - 应用能源的联动全部写在应用能源 GTO 分支的 `appeng.integration.modules.emi`，入口 `AppEngEmiHooks.register()` 与 `AppEngEmiPlugin`。
- 只维护 Forge 1.20.1 版，构建与运行用 JDK 21，Mojang 映射，构建插件为 MDG legacyforge。FastCollection 只在编译期引入，运行时由整合包里的 LeanObject 提供。
- 同一个 jar 里的 `emi` 与 `emitrades` 共用 Forge 的一个排序节点，二者之间的依赖不能写 `BEFORE`/`AFTER`，否则形成自环（`BundledEmiMetadataTest` 会检查）。
- 普通 JUnit 不会应用 mixin，测试里用到物品或流体键时 `mockStatic(EmiItemKey.class)` 或直接 `new EmiItemKey(...)`；引导 MC 用 `dev.emi.emi.TestBootstrap.init()`。

## 发布

- 用 `com.gto.gtopublishgradleplugin` 发布到 `https://gtodyssey.com/maven/releases`，坐标 `dev.emi:emi-forge-1.20.1:<版本>`（插件把 artifactId 改写成 `emi-forge-1.20.1`）。凭据从 `~/.gradle/gradle.properties` 的 `gtodysseyRepositoryUsername` / `gtodysseyRepositoryPassword` 读取，不得写入仓库。
- 版本号写在 `gradle.properties` 的 `gto_version`，格式 `x.y.z`，沿上游版本往后递增（首版 `1.1.25`），每次发布递增最后一段。
- 发布的是重混淆后的正式版 `reobfJar`（类里是 SRG 名，如 `m_41720_`），不是开发版 `jar`；发布后要检查 jar 里确实是 SRG 名。
- 本地联调用 `./gradlew publishToMavenLocal`，应用能源与 GTOCore 的 `emi_version` 改成同一个版本号即可。正式发布用 `./gradlew gtoValidate`，确认无误后 `./gradlew gtoPublishMaven`。
- 整合包统一发布时，本仓库有改动就要先于应用能源、GTOCore 发布，并同步更新它们的 `emi_version`。
- CurseForge 项目 ID 1760584，`gtoPublish` 同时发布到 Maven 与 CurseForge；前置为 Lean Object（运行时提供 FastCollection），与官方 EMI、EMI Trades 不兼容，JEI 可选。
