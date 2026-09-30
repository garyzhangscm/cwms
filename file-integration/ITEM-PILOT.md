# Item 第一轮联调

## 当前结果

三条新测试物料已通过现有 MES integration 接口创建，集成记录全部为 COMPLETED。
已核对实际物料、分类、Main 包装、三层数量和默认选项；用户已在 MES 界面检查并确认无问题。
真实业务文件、测试记录明细、环境配置和提交脚本只保留在本地，不提交到仓库。

这是小批量 HTTP 接口验证；五列 FTP 轮询代码现已完成本地模拟测试，但还未连接真实 FTP、部署自动运行或批量导入正式物料。

## Oracle 五列协议

| CSV 列 | Oracle 原生表头 | 规则 |
|---|---|---|
| item_number | SEGMENT1 | 必填，同文件不能重复 |
| item_description | DESCRIPTION | 必填，保留原始业务描述 |
| item_type | ITEM_TYPE | 文本编码，01 映射到 Finish Good；未知编码拒绝 |
| pieces_per_carton | PIECES_PER_CARTON | 空或 0 默认 1，其他值必须为正整数 |
| pieces_per_pallet | PIECES_PER_PALLET | 空或 0 默认 1，其他值必须为正整数 |

保持 UTF-8 和编码前导零。补值在 defaultsApplied 中记录字段和 empty/zero 原因。
公司代码、仓库名称、货主及分类对应表来自部署配置，不增加 Oracle 文件列。
公司代码不等于数据库内部 companyId，不能互换。

## 包装配置

Main 包装使用 PCS=1、CS=每箱件数、PL=每托件数，数量均以 PCS 为基准。
实际箱单位已核对为 CS，不能另建 CTN 代替。cartonUnit 仍可按不同环境配置。

unitOptions 使用 piece/carton/pallet 三个角色。用户已确认 PCS、CS 的以下五项全部 false，PL 全部 true：

- defaultForInboundReceiving
- defaultForWorkOrderReceiving
- trackingLpn
- defaultForDisplay
- caseFlag

提供 unitOptions 时，三个角色及各自五个布尔字段必须完整。
unitMeasurements 可为每个角色配置 length、width、height、weight 四个正数。
用户授权本次三条测试的这四个值全部使用 1；这是测试占位值，不是正式物料实测值。
示例配置不默认启用测试尺寸，避免将占位值自动应用到整个业务文件。

## 本地检查

```sh
python3 -m unittest discover -s tests -v
python3 oracle_items.py examples/oracle-items-demo.csv \
  --config item-mapping.example.json --batch-id demo001 \
  --output item-preview.json
```

oracle_items.py 仅生成离线预览，不会联网提交。输出不可覆盖已有文件。
示例使用虚构数据及公司/仓库占位符。生成的 recordId 取决于批次号、公司/仓库/货主和料号，
不受行顺序影响；同一事件重试应保持批次号，不能借更换 ID 绕过去重。

五列解析已连接 adapter.py 的独立 `oracle-items-v1` FTP 轮询模式；文件名为
`oracle-items__<批次号>.csv`，须配合同名 `.ready` 文件。配置及报告命令见 [README](README.md)。
其映射和文件名与早期通用模板模式不同，不能将五列 CSV 放入通用模板 inbox。
examples/items__demo001.csv 是早期通用基础物料格式，与五列格式不同；
五列预览包含嵌套分类和包装，不需要为同一测试另外重复发送包装文件。

## 已修复的接口问题

旧版 DBBasedItemFamily(ItemFamily) 丢弃 companyId/companyCode，造成分类转换失败。
已补齐字段复制，通过原 JAR 失败、新类通过的回归测试；使用原 v1.62 镜像，仅替换目标类后部署。
构建及回退方法见 [热修复说明](../integrationsvr/tools/ITEM-FAMILY-HOTFIX.md)。

分类描述也必须提供，转换器使用映射后的分类名称作为分类描述；物料描述仍来自 Oracle。
库存数据库要求包装高度等数值非空，测试补齐用户确认的尺寸重量后完成创建。
旧失败记录保留，没有直接编辑数据库或盲目重送不完整记录。

## 正式批量导入前仍需处理

- 当前接口未保存长宽高及重量的单位标签；实际数值已保存，单位字段仍为空。
- 嵌套分类被保存为仓库级同名 Finish Good，尚未复用原公司级分类。
- 本次只验证新测试物料。已有物料更新时，不能假设省略字段会保留；需要验证成本、分类、
  包装及其他 MES 设置的保留行为。
- FTP 文件接入已完成本地模拟测试，真实连接、持续运行、服务身份、失败重试与人工核对流程仍需完成联调。
- 完整源码版本与部署版本不同，不能把整个新源码镜像当作本次单类修复直接替换。
