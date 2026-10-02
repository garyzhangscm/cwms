# Fay 移植核对记录

## 源码基础

- Fay 分支：`codex/fay-oracle-item-file-import`。
- v1.60 源码候选基础：`9dd8602d35150f0e458013efe82ebfee66941f37`（仓库升级到 v1.61 之前的提交）。镜像标签不能单独证明二进制与此提交一致；部署前继续核对实际镜像和接口。
- 文件适配器参考：`codex/oracle-item-file-import`，提交 `e58abbf3f56adb2ecd77b5f4856523838b94a95a`。
- 两者 `integrationsvr` 的差异仅涉及项目版本、Docker JAR 路径及两处 Item 修复/回归用例。保留 v1.60 的 POM 和 Dockerfile，不升级整套 MES。

## 已移植的 Item 修复

1. `DBBasedItemFamily` 构造时保留 `companyId/companyCode`，确保分类解析仍能定位正确公司。
2. `DBBasedItemIntegration` 先保存 `SENT` 再发布 Kafka；存在外层事务时在提交后发布，避免快速 `COMPLETED` 回执被旧状态覆盖。

独立 Java 回归用例来自参考分支；已在本分支构建的 v1.60 产物上执行，两项均通过。Maven 默认跳过测试，因此这两项使用独立 javac/java 明确执行。

## 现场已确认

- Fay master：`10.0.11.188`；业务 app1：`10.0.11.174`；导入候选主机 app2：`10.0.11.195`。
- app2 的 VPN 服务为 `openvpn-client@fay-oracle-auto.service`，已验证 FTP 控制端口可达。
- Oracle FTP：`192.168.20.118:21`。尚未验证 FTP 登录、目录列表和被动数据连接。
- 2026-10-02 现场检查确认 integrationservice 仅有 Service，端口 `8880`、NodePort `30681`，无 Deployment/Pod/Endpoint。
- app2 kubelet 仍有独立故障；文件适配器可以用 systemd 运行，不依赖该节点加入 Kubernetes。本次不修改 kubelet。

## 上线前待核对

- 所有业务服务镜像标签为 v1.60；数据库配置指向 `10.0.11.34:3306/cwms`，`ddl-auto: none`。schema 和 integration 表仍需只读核验。
- 已通过 Fay layout API 确认：公司代码 `20901`、公司 ID `1`、仓库 ID `1`、名称 `WMEC`。用户确认 `01 → Finish Good`、`RM → Raw materials`，沿用源包装规则；已核对分类存在。
- 用户确认 FTP 目录 `/WIS`、前缀 `fayint_item`。FTP 账号权限和实际 Item CSV 待核对。
- integration 数据库是否已存在、版本和建表/迁移方式；不自动启用生产 DDL 更新。
- Fay integration 镜像构建、Kafka/服务连接、HTTP 鉴权及完成回执。
- 首次单批验证通过后再确认执行时间、启用定时器和决定是否允许删除远端 CSV。

## 测试边界

离线 Python 测试覆盖去重、业务状态、错误/未知结果和远端清理。本机 Python 3.12 和 Fay app2 Python 3.6.8 均通过 55 项 Python 测试。修正一个上游测试对新版 Mock.args 的依赖，不改变业务实现。Fay 新增测试覆盖共享目录中的前缀隔离、非法前缀在联网前拒绝、完成文件保留、后续批次不被阻塞、空 CSV 保留及无效配置在网络操作前被拒绝。

源码构建与现场 API 联调是独立步骤。仅有 mock 测试通过，不能说明 Fay 数据库或业务接口已验证。

## 当前准备状态

- app2 已创建专用系统用户/组 `cwmsfayitem` 和独立配置/状态目录。
- FTP 凭据工具已安装到 `/usr/local/sbin/fay-item-save-ftp`，等待用户本机输入。
- 未启用导入服务或定时器；本轮测试未提交生产 Item，也未删除 FTP 文件。
- app2 的测试副本保存在 `/tmp/fay-item-validation.7FpOtl`。
