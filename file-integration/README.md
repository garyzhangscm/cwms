# MES 文件接入适配器（协议 v1）

通过现有 HTTP 接口提交集成记录；Item 联调中发现的旧接口字段丢失问题已做最小后端修复。

```text
总部 Oracle 导出程序 → FTP 文件及 ready 标记 → 本适配器
    → integrationservice 集成接口 → 原 MES 业务处理 → 查询处理状态
```

本程序不连接 Oracle、不使用数据库账号、不直接写 MES 数据库。Oracle 导出程序由总部运行。
已授权创建三条测试物料并由用户确认。Item 五列转换已接入 FTP 轮询代码，但尚未连接真实 FTP、部署自动运行或批量导入正式物料。
Python 3.10+，Linux/macOS，标准库，无 pip 依赖。

用户确认的 Oracle **五列 Item 协议**由 `oracle_items.py` 转换，并通过 `adapter.py`
的 `oracle-items-v1` 模式接入 FTP 轮询，详见 [Item 联调说明](ITEM-PILOT.md)。
包含分类映射及 PCS/CS/PL，箱数/托数空或 0 默认 1。此模式与下文早期通用文件模板是两套独立的 inbox 协议，不要混用。

## Oracle Item FTP 自动导入

文件名使用专属前缀 `int_item__<批次号>.csv`，例如 `int_item__20260930-001.csv`。
同目录中的其他前缀文件一律忽略。
CSV 只能有五列：`SEGMENT1,DESCRIPTION,ITEM_TYPE,PIECES_PER_CARTON,PIECES_PER_PALLET`
（也接受对应的小写别名）。上传时先用 `.part` 临时名，完成后改为最终文件名，最后创建同名 `.ready` 空文件。
只有二者都出现才处理。文件一经发布不得修改，批次名不得复用。

复制 `config.oracle-items.example.json` 配置 FTP 主机、目录、测试服务地址、实际公司代码和仓库。
其中 companyId=20901 是已知公司内部 ID；warehouseId=0 是必须替换的占位值。
示例故意没有 `unitMeasurements`：正式物料的长宽高和重量规则未定，运行时会拒绝发送。
确认后必须为 piece/carton/pallet 各填写四个正数，并决定是否沿用目前的 WMEC 仓库级
`Finish Good` 分类；不要把三条测试料的占位尺寸当成正式值。
FTP 用户名和密码只从 `MES_FTP_USER`、`MES_FTP_PASSWORD` 环境变量读取，不写入仓库。
如需 MES Bearer token，给 `mes` 配置 `bearerTokenEnv` 并由服务器环境变量提供。

```sh
# 离线验证文件内容和映射；不会连接 FTP 或 MES
python3 adapter.py validate int_item__20260930-001.csv --config config.oracle-items.json
# 配置和规则确认、测试联调后才运行；此命令会写入 MES
python3 adapter.py run --config config.oracle-items.json --send --once
# 查看本地处理报告，不连接 FTP 或 MES
python3 adapter.py status --config config.oracle-items.json
python3 adapter.py status --config config.oracle-items.json --file int_item__20260930-001.csv
```

每轮先查询已接收记录的 MES 状态，再读取 FTP。每条 Item 的处理报告记录料号、原始行号、
补值原因、MES integrationId 和状态；`ACCEPTED` 仅表示接口接收，只有 `COMPLETED`
表示业务完成。FTP 暂时不可用时仍更新已接收记录的本地状态报告，同时扫描返回失败。
同一文件重复轮询不会重复提交；同一公司/仓库/料号即使更换批次名也会被拒绝。
此外，在发送新料号之前查询 MES 库存接口；如果已存在，该文件全部拒绝，不做更新。
这意味着正式 1,105 条中只要包含已有料号，就需要先制定新建/更新的拆分规则。
状态库和报告须持久化；删除状态库或换另一个空状态目录会破坏去重依据。

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
通用模板是基本字段子集；其中物料分类由单独的五列转换器支持。工单副产品/指令、订单特殊物流要求、取消/删除等尚未实现，
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
公开源码版本为 v1.63，仅作为辅助。已对 v1.62 做分类公司字段单类修复，并完成三条新测试物料的写入验证；
其他业务类型尚未完成实际写入验证。
六类均使用 `PUT /integration-data/<类型>`，状态查询为 `GET /integration-data/<类型>/<id>`。
HTTP 200 仍须检查 result=0。供应商/工单返回 id 字符串，其余通常返回含 id 的对象。

完整迁移仍需完成：

- 与总部确认必需字段、单位、全量/增量、更新/取消规则及文件保留期限。
- 使用真实测试 FTP、MES 测试账号和脱敏业务样例验证六类完整业务结果。
- 根据实际业务补齐字段和依赖调度、异常告警、人工补偿、凭据刷新及部署运维。
- 实现收货确认、出库确认、生产完工三个反向文件流程；总部负责消费回传文件并更新 Oracle。
- 联调原有 host 回调/定时任务的切换，确保 MES 不再调用 dblink；本程序未改变它们。
- 约定回退步骤后再上线。本版不含 Oracle 导出 SQL，也不启动或恢复 dblinkserver。
