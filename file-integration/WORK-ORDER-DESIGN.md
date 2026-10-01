# Oracle → WIS FTP → MES Work Order：第一版设计草案

## 边界与当前状态

本文件是已确定的第一版文件协议；`k8s-app2` 的 Item 定时服务只扫描 `int_item`。
Work Order 转换与轮询代码已安装到 app2，但工单定时器仍关闭，尚待测试文件联调。
第一版沿用 WIS FTP `/WIS`，只接受唯一批次名 `int_workorder_批次号.csv`，
不要求 `.ready`。Oracle 完成上传后应使用最终文件名，批次名不得复用。

MES 现有集成接口是 `PUT /integration-data/work-orders`，返回集成记录 ID；
随后通过 `GET /integration-data/work-orders/{id}` 读取 `PENDING`、`SENT`、
`COMPLETED` 或 `ERROR`。`COMPLETED` 才表示 MES 已处理工单。
工单业务接口可按仓库和工单号查询实际工单，用于发送前查重。

## 建议 CSV：一行代表一条投料

```csv
WORK_ORDER_NUMBER,FINISHED_ITEM_NUMBER,PLANNED_QUANTITY,PO_NUMBER,COMPONENT_LINE_NUMBER,COMPONENT_ITEM_NUMBER,COMPONENT_QUANTITY
WO-TEST-001,FG-001,10,PO-001,1,RM-001,20
WO-TEST-001,FG-001,10,PO-001,2,RM-002,3
```

| CSV 列 | MES 含义 | 建议规则 |
| --- | --- | --- |
| `WORK_ORDER_NUMBER` | 工单号 | 必填；同一批内同工单号的行合成一张工单 |
| `FINISHED_ITEM_NUMBER` | 成品料号 | 必填；必须已存在于 WMEC |
| `PLANNED_QUANTITY` | 计划生产数量 | 必填；正整数；同一工单各行必须一致 |
| `PO_NUMBER` | Oracle 关联单号 | 可空；同一工单各行必须一致 |
| `COMPONENT_LINE_NUMBER` | 投料行号 | 必填；同一工单内唯一 |
| `COMPONENT_ITEM_NUMBER` | 投料料号 | 必填；必须已存在于 WMEC |
| `COMPONENT_QUANTITY` | 投料需求数量 | 必填；正整数 |

公司固定为代码 `20901`，仓库固定为 `WMEC`；不要求 Oracle 在每行重复提供。
投料库存状态由 MES 导入配置统一指定，不要求 Oracle 提供此列。
运行中的 WMEC 库存状态查询返回名称代码 `AVAL`（ID 1），
仓库 CSV 中的 `Available` 是说明。导入配置使用 `AVAL`。
MES 的 `workOrderLines` 对应投料明细；每张工单第一版只处理主产品和投料，
暂不处理副产品及作业指令。源文件使用 UTF-8 CSV，可包含引号和逗号。

## 处理规则

1. 文件大小、字段、数量、重复投料行号及同工单表头一致性均在发送前验证；
   同工单号的多行正常合并为一张工单。
2. 对成品、投料、配置的库存状态及工单号做 MES 预检查。找不到对应资料则拒绝该批，
   不提交半张工单；同编号已存在的工单跳过并记录原因，包括仍处于 `PENDING` 的工单。
3. 一张工单只提交一次。保存 FTP 文件快照、每张工单的集成 ID 与处理状态；
   网络结果不确定时停住待人工核对，不盲目重试。
4. 仅在本批工单全部 `COMPLETED` 或明确跳过后，核对 FTP 源文件仍与快照相同，
   再删除这份 CSV。错误文件保留在 FTP 并记录原因。
5. 生产测试先用一张不会影响实际生产的工单，核对 MES 中的成品、数量和投料明细，
   再开放每天 10:30 的自动扫描。部署到 `k8s-app2`，与 Item 使用独立工单状态记录；不启用 DB link。

## 尚需确认

- MES 当前工单和投料数量字段均为整数类型；Oracle 可能导出小数，且暂无换算依据。
  出现小数时拒绝该批，不四舍五入，也不默默截断。
- 统一库存状态 `AVAL` 已在运行中的 MES 核对；还需一张真实测试工单样例。

MES 原 `DBBasedWorkOrderIntegration.process` 是先发 Kafka 后保存 `SENT`，
存在 Item 昨日已修复的同类状态覆盖风险。源码已调整为先保存、事务提交后发送；
在原部署镜像中旧版回归失败、修复版通过，修复镜像已部署到 app1。
首次真实发送仍需用 Oracle 测试工单核对 MES 中的成品、数量和投料。
