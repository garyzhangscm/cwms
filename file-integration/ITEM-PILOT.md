# Item 第一轮联调

目标：先验证一个新测试物料从 CSV 进入 MES 的基础资料，再验证单位和包装。未进行生产写入。

## 已确认：Oracle 五列协议（优先于下方早期通用模板）

用户已确认只从 Oracle 提供以下五列：

也支持 Oracle 原生表头 `SEGMENT1,DESCRIPTION,ITEM_TYPE,PIECES_PER_CARTON,PIECES_PER_PALLET`，
其中 SEGMENT1 对应 item_number，DESCRIPTION 对应 item_description。无需修改源文件。

| CSV 列 | 规则 |
|---|---|
| item_number | MES 料号，必填，同文件不能重复 |
| item_description | MES 描述，必填 |
| item_type | 文本编码，`01` 映射到 `Finish Good`；未知编码拒绝 |
| pieces_per_carton | 空或 0 默认 1，其他值必须为正整数 |
| pieces_per_pallet | 空或 0 默认 1，其他值必须为正整数 |

使用 Main 包装，PCS=1、CTN=每箱件数、PL=每托件数；三者数量均以 PCS 为基准。
实际部署的箱单位已只读核对为 `CS`，示例配置现使用 CS，不能直接创建额外 CTN 单位。
用户已确认 PCS 和箱单位的五个选项全部 false，PL 全部 true：
defaultForInboundReceiving、defaultForWorkOrderReceiving、trackingLpn、defaultForDisplay、caseFlag。
配置 unitOptions 使用 piece/carton/pallet 三个角色，避免箱单位名称变化影响选项。
一旦提供 unitOptions，必须完整提供三个角色和各自五个布尔值，禁止将字符串 "false" 当布尔值。
补值在预览的 defaultsApplied 中记录字段及 empty/zero 原因。
`01` 必须保留前导零，不能通过电子表格另存成数字 1。
真实单位名称仍需核对，CTN 可在配置中修改；公司/仓库/货主由部署配置提供，不额外增加 Oracle 列。

本地预览命令（不联网、不提交）：

```sh
python3 oracle_items.py examples/oracle-items-demo.csv \
  --config item-mapping.example.json --batch-id demo001 \
  --output item-preview.json
```

输出不可覆盖已有文件。预览包含分类和三层包装的嵌套请求草案，不含成本、尺寸、重量或默认选项。
这些字段如何在新增时赋值、更新时保留，仍需核对 MES 实际行为后才能提交。
五列解析尚未连接 adapter.py 的 FTP 轮询，不能直接把这个 CSV 放入旧通用模板的 inbox。
样例全部使用虚构测试料号；item-mapping.example.json 中公司和仓库也是占位符。
recordId 由批次号、公司/仓库/货主及料号稳定生成，不受行顺序影响；重试必须保持同一批次号。
分类嵌套接口是否创建分类、包装单位是否存在，需要联调确认，预览不会自动创建任何 MES 配置。

### 提交前发现的 v1.62 兼容问题

对照生产 integration.jar 的 DBBasedItemFamily(ItemFamily) 字节码，构造器只复制
name、description、warehouseId、warehouseName，不复制 companyId/companyCode。
后续 convertToItemFamily 则要求公司字段存在。普通 Item 接口通过该构造器转换嵌套分类，
因此当前分类嵌套请求不能视为已验证可提交。不能通过删除分类来绕过需求，也不能声称导入已完成。
这与辅助公开源码行为一致；需确定修复接口转换或其他兼容接入路径，并在测试环境验证。
当前仅允许离线预览，未修改线上服务。新增预览选项不解决这个后端问题。

用户授权的一条新测试物料试提交在持久化阶段被拒绝：`Column 'description' cannot be null`。
分类对象原先仅有 name，没有 description；现已在转换器中用映射后的分类名称补充 description，
这与已核对的成品分类描述一致。物料本身的 description 仍使用 Oracle 原始描述。
查询未发现本次测试的物料或集成记录，未发送其余测试物料。
该本地修复尚未重新提交验证，且不解决上述公司信息被旧接口丢失的问题。

## 1. 确认字段

现成样例为 examples/items__demo001.csv，列的含义如下：

| 列 | 含义 | 第一轮要求 |
|---|---|---|
| recordId | 本次导出事件号，不是料号 | 固定且唯一，重传不能更换 |
| companyCode | MES 公司代码 | 从 MES 现有配置核对，不猜测 |
| warehouseName | MES 仓库名称 | 从 MES 现有配置核对，不使用服务器名 |
| name | 物料编码/料号 | 选一个未使用的新测试料号 |
| description | 物料名称/描述 | 按 Oracle 的业务含义映射 |
| unitCost | 单位成本 | 是否同步成本需要业务确认，不用 0 代替未知成本 |
| nonInventoryItem | 是否为非库存物料 | 普通库存物料为 false，不能根据名称猜测 |
| clientName（可选） | MES 货主名称 | 仓库有货主隔离时核对后填写 |

companyCode、warehouseName、name、recordId 是本适配器要求的基础定位字段。
示例中的 TEST_ONLY/TEST_WAREHOUSE 仅为占位符，不代表现有系统配置。

需要用户提供一条脱敏的 Oracle 物料样例，或 MES 物料详情截图，确认料号、描述、单位、分类、
货主和成本的实际使用方式。截图不需包含密码、连接串或凭据。

## 2. 文件检查

填写并另存为新的 items__<批次号>.csv，保持 UTF-8：

```sh
python3 adapter.py validate examples/items__demo001.csv
```

这一步只读本地文件。格式正确后，比较生成字段与预期物料信息，再配置测试 FTP/MES。

## 3. 测试环境验收

按 README 的最终文件 + ready 标记方式发布；等集成记录 COMPLETED 后，在 MES 界面核对
新物料的仓库、货主、料号、描述和成本；再次投递相同事件，确认没有重复创建集成记录。
保存 integrationId 作为核对依据。不能把 HTTP 成功或 ACCEPTED 当成最终验收。

单位和包装通过 item-package-types 文件单独接入，必须等物料成功后发布。
单位/包装尚未验收的物料不能视为已完成收发货使用配置。
物料分类尚未映射，若业务必需，应先补充实现再开始端到端联调。

## 4. 更新行为单独验证

辅助源码 inventorysvr ItemService.processIntegration 按仓库、货主和料号查找已有记录，
找到后进入更新流程。这是公开 v1.63 源码结论，尚未核对生产 inventorysvr 的实际实现。
不能假定省略的描述、成本、分类等字段会自动保留。第一轮禁止用现有正式料号试写；
新增验收完成后，用专门测试料号验证更新、缺省字段和重复事件，再确定正式增量规则。
