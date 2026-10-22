# EMI GTO Edition

本项目是为 [GregTech Odyssey](https://github.com/GregTech-Odyssey) 整合包专门维护的 EMI 版本。EMI 是一款物品与配方查看器，本版本基于 EMI 1.20.1 的 1.1.24 版本，只提供 Forge 版，针对整合包的玩法与运行环境做了相应的适配与调整。

本版本与原版在游戏设计上的差异，详见 [GTO_CHANGES.md](GTO_CHANGES.md)。如在使用中遇到问题，烦请反馈至 [GregTech Odyssey 整合包仓库](https://github.com/GregTech-Odyssey/GregTech-Odyssey/issues)，不要反馈给 EMI 原项目。

## 署名

- **EMI**：原作者 [Emi（emilyploszaj）](https://github.com/emilyploszaj)，原项目地址 <https://github.com/emilyploszaj/emi>。本版本以原项目 `1.1.24+1.20.1`（提交 [23d251ea](https://github.com/emilyploszaj/emi/commit/23d251ea8ea3a5fd7d760948f36014b185eac69f)）为基础，保留了原项目的提交历史。
- **fork 作者**：[Crazerium](https://github.com/Crazerium) 将 EMI 改为 Mojang 映射的单一 Forge 工程，并移植了 GTNH NEI 的书签、分组与自动合成等功能（原 fork 仓库 [GregTech-Odyssey/emi](https://github.com/GregTech-Odyssey/emi)）。
- **后续改动**：[Hophenby](https://github.com/Hophenby) 在应用能源 GTO 分支（[GregTech-Odyssey/Applied-Energistics-2-gto](https://github.com/GregTech-Odyssey/Applied-Energistics-2-gto)）中内置维护期间做了异步搜索烘焙、JEI 兼容层缓存、配方索引重写、工作站布局等改动，alcox233 与 GregTech Odyssey 团队也有修改；这些改动已全部移入本仓库。
- **EMI Trades**：原作者 Prismwork 与 Flamarine，原项目地址 <https://github.com/Prismwork/EMITrades>（`xplat/1.20.1` 分支，移植基于提交 98271be1425accea90df1006f885d5af29886ef9）。本版本内置了它，模组 ID 仍为 `emitrades`，不要再另装 EMI Trades。

## 许可证

EMI 与 EMI Trades 均依据 MIT 许可证开源，本项目沿用 MIT 许可证：

- EMI 的许可证全文见 [LICENSE](LICENSE)（Copyright (c) 2022 Emi），随模组发布时为 jar 内的 `LICENSE-EMI`。
- EMI Trades 的许可证全文见 [src/main/resources/LICENSE-EMITrades](src/main/resources/LICENSE-EMITrades)（Copyright (c) 2022-2023 Prismwork）。
- 汇总的署名说明见 [NOTICE](NOTICE)，随 jar 一同发布。

## 模组信息

- 显示名为 EMI GTO Edition；模组 ID 仍为 `emi`，Java 包名 `dev.emi.emi` 与资源命名空间不变，以兼容现有的 EMI 插件与存档数据。
- 只提供 Forge 1.20.1 版，需要 Java 21。运行时需要 FastCollection（整合包中由 LeanObject 提供）。
- 不能与官方 EMI 同时安装。
- 本模组不依赖应用能源、GregTech、GTOCore、GTOLib；应用能源的联动写在应用能源 GTO 分支的 `appeng.integration.modules.emi` 中，通过 `dev.emi.emi.api.EmiHooks` 与 `EmiStackKey` 接入。

## 构建与发布

```sh
./gradlew build
./gradlew runClient
```

Windows 下用 `.\gradlew.bat`，`JAVA_HOME` 指向 JDK 21。Maven 坐标为 `dev.emi:emi-forge-1.20.1:<版本>`，发布到 <https://gtodyssey.com/maven/releases>，发布流程见 [AGENTS.md](AGENTS.md)。
