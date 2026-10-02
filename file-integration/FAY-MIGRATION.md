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
- Oracle FTP：`192.168.20.118:21`。FTP 登录、进入 `/WIS` 和被动数据连接已验证；已读取 `fayint_item_20260930.csv`；格式与参考分支相同。
- 2026-10-02 现场检查确认 最初 integrationservice 仅有 Service；现已在 app1 部署独立镜像，端口 `8880`、NodePort `30681`，Pod Ready、健康检查 UP。
- app2 kubelet 仍有独立故障；文件适配器可以用 systemd 运行，不依赖该节点加入 Kubernetes。本次不修改 kubelet。

## 上线前待核对

- 所有业务服务镜像标签为 v1.60；数据库配置指向 `10.0.11.34:3306/cwms`，`ddl-auto: none`。已只读核验 37 张 integration 表和 560 个显式标注的简单字段，并通过 Hibernate 全量启动校验。
- 已通过 Fay layout API 确认：公司代码 `20901`、公司 ID `1`、仓库 ID `1`、名称 `WMEC`。用户确认 `01 → Finish Good`、`RM → Raw materials`，沿用源包装规则；已核对分类存在。
- 用户确认 FTP 目录 `/WIS`、前缀 `fayint_item`。FTP 登录和列表读取已确认；实际 CSV `fayint_item_20260930.csv` 已提供：Oracle 大写五列头，1,105 条成品；407 条已存在，698 条新增候选，155 条触发源包装默认值。
- 复用现有数据库及表，不启用自动 DDL 更新。Item 数量和 Stop 序号映射适配既有列类型；收货确认兼容字段按显式迁移补齐，详见下文。
- Fay 镜像构建、DB/Redis 健康、Kafka TCP 连接和 Item GET API 已通过；首次 PUT、Kafka 完成回执和新增物料仍待小批验证。
- 首次单批验证通过后再确认执行时间、启用定时器和决定是否允许删除远端 CSV。

## 测试边界

离线 Python 测试覆盖去重、业务状态、错误/未知结果和远端清理。本机 Python 3.12 和 Fay app2 Python 3.6.8 均通过 55 项 Python 测试。修正一个上游测试对新版 Mock.args 的依赖，不改变业务实现。Fay 新增测试覆盖共享目录中的前缀隔离、非法前缀在联网前拒绝、完成文件保留、后续批次不被阻塞、空 CSV 保留及无效配置在网络操作前被拒绝。

源码构建与现场 API 联调是独立步骤。仅有 mock 测试通过，不能说明 Fay 数据库或业务接口已验证。

## 当前准备状态

- app2 已创建专用系统用户/组 `cwmsfayitem` 和独立配置/状态目录。
- FTP 凭据工具已安装到 `/usr/local/sbin/fay-item-save-ftp`，用户已本机输入，密码不进入源码或记录。
- 文件适配器及手动单批服务已安装，但处于 inactive；未设置 batch-file、未安装定时器。本轮未提交生产 Item，也未删除 FTP 文件。后端 Kafka 监听暂时暂停，Host API 外发关闭。
- app2 的测试副本保存在 `/tmp/fay-item-validation.7FpOtl`。

- 初版及现场适配代码已提交；HTTPS 推送缺少凭据后改用本机已授权的 GitHub SSH 认证，成功创建远端分支 `codex/fay-oracle-item-file-import`。

## Fay 兼容适配及备份

- `integration_item_unit_of_measure.quantity` 是 BIGINT：保留 Integer API，加显式数据库列定义。
- `integration_stop.sequence` 是 INT：保留 Long API，加显式数据库列定义。
- 补齐 `integration_receipt_line_confirmation.quickbook_item_listid VARCHAR(255) NULL`。该字段属于启动扫描的既有实体；不启用 QuickBooks。
- 执行前备份该表的 CREATE TABLE 和全部 1,880 条记录（每个值 base64 编码，gzip 保存）。备份位于主节点 `/root/fay-integration-backups/receipt-line-confirmation-before-20261002T220611Z.json.gz`，SHA256 `e600450ec431557573491298e3e5c181706efd452b46c5e763941ce1d1101af6`。
- 执行后逐值比较原列，确认全部 1,880 条原记录未变。新增列允许 NULL；未删除任何列或记录。
- integration 认证日志移除账号密码请求和用户 token 内容，不修改认证行为。

## 当前运行产物

- app1 镜像：`cwms-fay-integrationserver:v1.60-item-d5e3fd34bba7`，使用 app1 已存在的 v1.60 Java 运行环境，未启动第二个库存服务。
- 部署配置：`deploy/fay/integrationservice.yaml`，固定 app1、`ddl-auto=validate`、`HOST_API_ENABLED=false`、`SPRING_KAFKA_LISTENER_AUTO_STARTUP=false`。
- app2 程序：`/opt/cwms-fay-oracle-items`；配置：`/etc/cwms-fay-oracle-items/config.json`；状态：`/var/lib/cwms-fay-oracle-items/state`。
- 已准备 3 条无默认补值的新料作为小批预览，存放 app2 `/tmp/fay-item-validation.7FpOtl/fayint_item_pilot20261002.csv`，未写入 MES，未上传或修改 FTP。

- 当前停在首批写入确认：3 条测试物料为 `028-3146-2`（CS 6 / PL 450）、`12 CAN-1`（CS 400 / PL 400）、`12562058`（CS 1 / PL 1），均为 Finish Good、PCS 1，未启用自动导入。
