# MES 文件接入适配器（本地原型，协议 v1）

无需原 integrationservice 后端源码，通过现有 HTTP 接口提交集成记录。

```text
总部 Oracle 导出程序 → FTP 文件及 ready 标记 → 本适配器
    → integrationservice 集成接口 → 原 MES 业务处理 → 查询处理状态
```

本程序不连接 Oracle、不使用数据库账号、不直接写 MES 数据库。Oracle 导出程序由总部运行。
当前仅完成入站本地原型，未部署或提交任何生产数据。Python 3.10+，Linux/macOS，标准库，无 pip 依赖。

用户确认的 Oracle **五列 Item 协议**现由 `oracle_items.py` 离线转换预览，详见
[Item 联调说明](ITEM-PILOT.md)。包含分类映射及 PCS/CTN/PL，箱数/托数空或 0 默认 1。
此预览尚未接入 FTP 轮询；下文仍描述早期通用文件模板，不要混用两种格式。

## 已实现范围

| 文件类型前缀 | 业务 | 单据标识 | 明细 |
|---|---|---|---|
| suppliers | 供应商 | name | 无 |
| items | 物料基础信息 | name | 无 |
| item-package-types | 物料包装规格 | name + itemName | 单位和换算数量 |
| receipts | 收货单/采购到货通知 | number | 物料和数量 |
| orders | 销售出库单 | number | 物料和数量 |
| work-orders | 生产工单 | number + itemName + expectedQuantity | 投料物料和数量 |

每类提供 CSV 和 XML 样例，内容等价，实际交换选一种即可。样例中 TEST_ONLY 等值为虚构占位符。
本版是基本字段子集；物料分类、工单副产品/指令、订单特殊物流要求、取消/删除等尚未实现，
不能直接替代所有旧接口行为。额外字段会被拒绝，避免悄悄丢失业务含义。

## 本地验证（不连接服务器）

在本目录执行：

```sh
python3 -m unittest discover -s tests -v
python3 adapter.py validate examples/receipts__demo001.csv
python3 adapter.py validate examples/receipts__demo001.xml
```

测试使用假的 FTP/MES 对象，覆盖解析、接口请求格式、重复文件、未知提交结果、业务失败及进程重启。
不等价于真实 FTP 连通性、身份验证或数据库业务结果测试。

## 文件契约

文件名：`<类型>__<批次号>.csv` 或 `.xml`，例如 `receipts__20260930-001.csv`。
批次号只用英文字母、数字、下划线、点和连字符，必须字母/数字/下划线开头，最多 101 字符。
文件名不可重复使用；发布后内容不可修改。单文件最多 10 MiB、10000 行。

CSV 为 UTF-8（可含 BOM）、英文逗号分隔、有表头，按标准 CSV 引号规则转义逗号、换行和双引号。
字段名区分大小写。XML 必须为 UTF-8，根为 `batch`，每行一个 `record`，明细放在该行的 `line` 内；
不允许属性、DTD、实体声明或任意嵌套。参见 examples 中完整样例。

所有行必填 `recordId`、`companyCode`、`warehouseName` 和上表的单据标识。
`recordId` 是导出方生成的、每种业务内全局唯一的不可变事件号，最多 128 字符，
使用英文字母、数字、下划线、点、冒号和连字符，首字符必须是字母或数字。
它不是 MES 数据库 ID，也不能直接当作业务单号。

一张单据多行时，每行重复相同 recordId 和全部头字段；明细使用 `line.number`、
`line.itemName`、`line.expectedQuantity`。相同 recordId 的头字段必须一致，行号不能重复。
包装规格明细使用 `line.unitOfMeasureName` 和 `line.quantity`，单位不能重复。
物料/供应商文件一行一个 recordId。没有明细的单据会被拒绝。

空白字段在适配器中视为未提供，但这不保证 MES 更新时保留原值。辅助源码中的物料更新逻辑
并未保留所有缺省字段，因此第一轮只使用新的测试料号；更新已有物料需另行验证字段覆盖行为。
字符串首尾空格去掉；数字使用小数点，不带千分位。
单据数量只接受非负整数，包装换算数量为 Java Integer 范围；金额/尺寸为非负有限数；布尔只接受 true/false。
业务是否允许零数量、单位换算关系及有效物料等，由测试环境验证；本地格式校验通过不等于业务校验通过。

允许字段的完整清单在 adapter.py 的 SCHEMAS；常用可选字段包括 description、supplierName、
poNumber、inventoryStatusName（明细）、客户名称、地址等。不得传入 Oracle 主键作为 MES id。
本程序给明细补 companyCode/warehouseName，给工单补 PENDING 状态、工单明细补 ATTACHED 状态。

### 发布完成标记

1. 导出方以 `.part` 临时名字上传数据。
2. 上传成功后重命名为最终 `.csv` / `.xml` 名字。
3. 最后创建同名加 `.ready` 的空文件，例如 `receipts__20260930-001.csv.ready`。

只处理最终文件和 ready 都存在的文件。发布后的数据必须保持不变；ready 不是处理完成回执。
FTP 用户应限制在交换目录。当前不删除/移动远端文件，也不上传回执，处理报告仅写入本地 state 目录。
生产者需有保留和归档策略；当前每轮会重新下载已发布文件再去重，不适合无限积累文件。

### 依赖关系

先保证公司、仓库、客户、单位等基础配置存在，再导供应商/物料，然后包装规格和单据。
同一轮扫描不保证跨文件业务依赖完成；必须等前置记录 COMPLETED 后再发布依赖它的文件。
不要把同一业务同时交给旧 dblink 定时任务和新适配器导入。

## 配置及测试环境运行

复制 config.example.json 为 config.json，填写 FTP 主机、端口、目录、MES 测试接口 baseUrl。
baseUrl 为包含 integration 服务路由的地址，不含末尾 `/integration-data`；样例路由必须实测确认。
支持 FTP 和显式 FTPS（tls=true，验证证书），不支持 SFTP。用户目前指定 FTP，因此样例显式设置
allowPlainFtp=true；普通 FTP 不加密，部署网络和传输方式需符合总部允许的方案。

用户名/密码及 MES Bearer token 从环境变量读取，配置文件只写环境变量名。
在服务器本地或秘密管理系统设置 `MES_FTP_USER`、`MES_FTP_PASSWORD`、`MES_API_TOKEN`，不要写入 Git。
token 自动登录/刷新尚未实现；需要选定现有 MES 支持的服务身份方案。仅在确定接口不要求 Bearer 的
受控测试环境才可省略 bearerTokenEnv。

```sh
# 以下命令会写入配置指向的 MES，只能在完成配置后的测试环境运行。
python3 adapter.py run --config config.json --send --once
# 持续扫描，默认间隔 60 秒：
python3 adapter.py run --config config.json --send
```

stateDirectory 必须放持久磁盘并使用绝对路径。单实例运行，不能让两个实例使用不同状态目录处理同一 inbox。
文件锁只保护同一状态目录。本地状态库/原始文件含业务数据，默认新建权限受 umask 077 保护；目录应限服务账号访问。
备份时将状态库和原始文件作为整体保存。不要通过删除 ledger.sqlite3 来重试，否则可能重复导入。
本程序不会自动清理 state，部署前必须配置容量、备份和保留期限。

## 处理状态和故障恢复

每个文件生成 `<文件名>.report.json`，每条记录有状态及 MES integrationId：

| 状态 | 含义及处理 |
|---|---|
| PREPARED | 通过文件检查，尚未发送 |
| SENDING | 正在发送；若中断，启动时转 UNCERTAIN |
| UNCERTAIN | 提交结果未知，需要人工核对；不自动重发 |
| ACCEPTED | MES 已返回集成记录 ID，业务仍可能排队/处理/等待回调 |
| COMPLETED | 状态查询确认业务 COMPLETED |
| BUSINESS_ERROR | MES 返回 ERROR，需核对该集成记录错误原因 |
| REJECTED（文件级） | 格式、重复 ID 内容冲突或已发布文件被修改 |

同 kind+recordId、相同规范化内容只发送一次。相同 ID 内容不同则整份文件预检拒绝。
文件内多条单据是逐条提交，不是一个分布式事务；之前成功的记录不会因后续错误回滚。
发生 UNCERTAIN 时，本轮停止继续发送该文件，下一轮可以处理其中其他尚未发送的独立记录。
跨进程崩溃/网络中断不具备服务端 exactly-once 保证，采用“未知结果不自动重发”的保守策略。

UNCERTAIN 应先按业务单号、时间和 MES 集成记录核对是否已入库，不要换个 recordId 盲重试。
BUSINESS_ERROR 应在现有 MES 中查错，确定修复/重送方式；本版不自动调用 resend 接口。
报告不会包含服务器返回的业务错误详情或凭据。当前人工核对后的状态修复工具尚未提供。
查询接口暂时不可用时保留 ACCEPTED，不误报成功。MES 返回 SENT 也不等于 COMPLETED。

## 接口依据与上线边界

2026-09-30 对照本地读取的生产 v1.62 integration.jar 的控制器/DTO，以及公开源码
https://github.com/garyzhangscm/cwms 的提交 2d6efc9a99539a7cf1efdb55163136e31ca9932d。
公开源码版本为 v1.63，仅作为辅助；尚未通过线上写请求验证。
六类均使用 `PUT /integration-data/<类型>`，状态查询为 `GET /integration-data/<类型>/<id>`。
HTTP 200 仍须检查 result=0。供应商/工单返回 id 字符串，其余通常返回含 id 的对象。

完整迁移仍需完成：

- 与总部确认必需字段、单位、全量/增量、更新/取消规则及文件保留期限。
- 使用真实测试 FTP、MES 测试账号和脱敏业务样例验证六类完整业务结果。
- 根据实际业务补齐字段和依赖调度、异常告警、人工补偿、凭据刷新及部署运维。
- 实现收货确认、出库确认、生产完工三个反向文件流程；总部负责消费回传文件并更新 Oracle。
- 联调原有 host 回调/定时任务的切换，确保 MES 不再调用 dblink；本程序未改变它们。
- 约定回退步骤后再上线。本版不含 Oracle 导出 SQL，也不启动或恢复 dblinkserver。
