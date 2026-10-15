# Inbound API 阅读笔记与完整接口清单

基于本地 `v1.64` 分支，提交 `342386d6`，阅读日期 2026-09-21。
覆盖 `inboundsvr` 中全部 13 个 Controller、89 个显式 HTTP 映射，包括测试及运维接口。
这是源码阅读结果，尚未连接业务数据库执行接口联调；不包含框架自动提供的端点。

## 路由与响应约定

- 服务名 `inboundservice`，源码默认端口 `8580`。
- API Gateway 将 `/api/inbound/**` 去掉前两段后转发到 `http://inboundservice:8580`。
  例如外部 `/api/inbound/receipts` 对应服务内 `/receipts`。实际域名取决于部署。
- 网关通常读取 `Authorization: Bearer <JWT>`，同时校验 `companyId`（请求头或查询参数）。
  Controller 声明的 `@RequestParam companyId` 仍需以查询参数传入，网关头部校验不能替代它。
- `warehouseId` 在许多列表查询中是必填查询参数。`ClientRestriction` 由客户权限切面处理，
  不应由 iOS 构造为可信权限信息。
- 对 JSON 响应，`BaseGlobalResponseBodyAdvice` 通常包装为 `{"result":0,"message":"","data":...}`。
  下文返回类型表示包装前的业务数据；已返回 `ResponseBodyWrapper` 的方法不重复包装。
  `result != 0` 表示业务错误，不能只根据 HTTP 状态判断成功。
  通用异常的 `data` 可以是 `ExceptionResponse`；上传方法捕获异常时会返回 `result=-1`。
  非 JSON 类型（例如 String 返回值）不保证经过相同包装。
- 分页仅显式提供 `/paginated-receipts`：`pageIndex=0`、`pageSize=20`，按 `number` 倒序；
  `data` 是 Spring `Page<Receipt>`，与非分页接口的数组不同。
- 日期查询使用 ISO DATE / DATE_TIME；模型中的自定义时间序列化输出 UTC ISO 时间，
  反序列化调用 `Instant.parse`，对接时应使用带 `Z` 的 UTC 时间字符串。

## 核心业务理解

1. 收货单可直接创建，也可从采购单指定行和数量生成。
2. 常规流程为 `OPEN → CHECK_IN → RECEIVING → CLOSED`。
   Check-in 只允许 OPEN，记录 UTC 到货时间、重算质检量，并调用库位服务创建收货单同名库位。
3. 收货请求携带 `Inventory`。服务按收货规则校验超收及货品，匹配质检规则，
   调用库存服务入账，再更新收货行累计数量、收货单状态、计费信息和收货流水。
   单 LPN 收货支持 `rfCode`、`receiveToStage`、`stageLocation`；批量接口逐个处理 Inventory。
4. 上架分配会调用库位服务预留库位，并调用库存服务设置 movement path，具有写入副作用。
   不能把它当成纯粹的“查询推荐库位”。实际库存移动需要继续结合库存服务接口。
5. 完成收货单会设置 CLOSED、发送完成集成数据，并在关联采购单时刷新采购单收货量。
6. `/reverse` 在当前 ReceiptLineService 中回减收货行数量、按参数回减质检累计数；
   该方法没有直接删除或冲销库存，不能单独当成完整的库存撤销流程。
7. 客户退货具有独立订单与订单行，但也使用 Inventory 收货、质检、打印等能力。
8. 文件导入采用“上传 → 返回 key → 查询进度 → 查询逐行结果”；进度以两位小数字符串返回。
   打印接口返回 ReportHistory，不是直接返回 PDF 字节。

## 对接时特别注意

- HTTP 方法沿用现有实现：例如收货单行新增是 PUT、修改是 POST；规则新增 PUT、修改 POST。
- 采购单列表状态参数实际拼写是 `purchasOrderStatusList`；收货单为 `receipt_status_list`，退货单为 `status_list`。
- 普通收货 Inventory 模型使用嵌套 `item`、`itemPackageType`、`inventoryStatus`；
  收货校验直接访问 `inventory.item.id`，不能仅凭其他接口习惯假设发送顶层 `itemId` 即可。
- `/receipts/receiving-inventory/upload` 默认 `removeExistingInventoryWithSameLPN=true`、`emptyLocation=true`，
  对接前需要明确这些导入选项，不能将其当成普通追加收货接口。
- 路径绑定问题一：退货行重算质检路径使用 `{lineId}`，Java 参数却是未显式命名的 `@PathVariable Long receiptLineId`。
- 路径绑定问题二：修改收货行计费活动路径使用 `{receiptLineId}`，Java 参数却是 `@PathVariable Long receiptId`。
  这两处从源码看会发生路径变量绑定失败；本次只记录，未修复或通过 HTTP 复现。
- 修改上架配置路径虽然含 `{id}`，实现没有绑定它，而是直接保存请求体，需注意请求体 id。
- `/test-data/init`、`/test-data/clear` 是数据写入操作，不用于正常 iOS 业务流程。

## 接口分组

| Controller | 数量 | 范围 |
| --- | ---: | --- |
| ReceiptController | 40 | 收货单、收货行、收货、打印、导入与计费 |
| CustomerReturnOrderController | 15 | 客户退货单与退货收货 |
| PurchaseOrderController | 3 | 采购单查询与转收货单 |
| InboundReceivingConfigurationController | 6 | 收货规则与最佳匹配 |
| InboundQCConfigurationController | 5 | 入库质检规则 |
| PutawayConfigurationController | 8 | 上架规则与库位分配 |
| ReceivingTransactionController | 1 | 收货操作流水 |
| ReceiptBillableActivitiesController | 1 | 收货计费活动查询 |
| EulogiaController | 3 | Eulogia 客户装箱单导入 |
| InboundController | 1 | 入库相关货品引用替换 |
| ProbeController | 1 | 存活探针（访问数据库） |
| WebSiteInformationController | 1 | 应用信息 |
| TestDataInitController | 4 | 测试数据查看、初始化与清理 |

## 完整接口与参数

以下均为服务内路径，经过网关时加 `/api/inbound`。
参数声明按源码保留：`@PathVariable` 为路径参数，`@RequestParam` 为查询/表单参数，
`@RequestBody` 为 JSON 请求体，`MultipartFile` 通过 multipart/form-data 上传。
`required=false` 和 `defaultValue` 按声明解释；没有注解的简单参数保留原样，
表示依赖 Spring 隐式参数绑定，业务必填条件还需结合服务层。
返回类型是 Java 声明，不是经过业务数据验证的完整 JSON Schema。

### 收货单、收货行、收货、打印、导入与计费（ReceiptController）

#### `GET /receipts`

实现：[findAllReceipts](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:68>)；返回：`List<Receipt>`。

```java
@RequestParam Long warehouseId,
@RequestParam(name="number", required = false, defaultValue = "") String number,
@RequestParam(name="supplierName", required = false, defaultValue = "") String supplierName,
@RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId,
@RequestParam(name="clientName", required = false, defaultValue = "") String clientName,
@RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
@RequestParam(name="receipt_status_list", required = false, defaultValue = "") String receiptStatusList,
@RequestParam(name = "checkInStartTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime checkInStartTime,
@RequestParam(name = "checkInEndTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime checkInEndTime,
@RequestParam(name = "checkInDate", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
@RequestParam(name = "purchaseOrderId", required = false, defaultValue = "") Long purchaseOrderId,
@RequestParam(name = "ids", required = false, defaultValue = "") String ids,
@RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails,
ClientRestriction clientRestriction
```

#### `GET /paginated-receipts`

实现：[findPaginatedReceipts](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:92>)；返回：`Page<Receipt>`。

```java
@RequestParam Long warehouseId,
@RequestParam(name="number", required = false, defaultValue = "") String number,
@RequestParam(name="supplierName", required = false, defaultValue = "") String supplierName,
@RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId,
@RequestParam(name="clientName", required = false, defaultValue = "") String clientName,
@RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
@RequestParam(name="receipt_status_list", required = false, defaultValue = "") String receiptStatusList,
@RequestParam(name = "checkInStartTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime checkInStartTime,
@RequestParam(name = "checkInEndTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime checkInEndTime,
@RequestParam(name = "checkInDate", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
@RequestParam(name = "purchaseOrderId", required = false, defaultValue = "") Long purchaseOrderId,
@RequestParam(name = "ids", required = false, defaultValue = "") String ids,
@RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails,
@RequestParam(name = "pageIndex", required = false, defaultValue = "0") int pageIndex,
@RequestParam(name = "pageSize", required = false, defaultValue = "20") int pageSize,
ClientRestriction clientRestriction
```

#### `GET /receipts/count-by-supplier`

实现：[getReceiptCountBySupplier](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:123>)；返回：`Integer`。

```java
@RequestParam Long warehouseId,
@RequestParam(name="supplierName", required = false, defaultValue = "") String supplierName,
@RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId
```

#### `POST /receipts`

实现：[addReceipts](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:132>)；返回：`Receipt`。

```java
@RequestBody Receipt receipt
```

#### `GET /receipts/{id}`

实现：[findReceipt](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:138>)；返回：`Receipt`。

```java
@PathVariable Long id,
@RequestParam(name="ignoreNotFoundError", required = false, defaultValue = "false") Boolean ignoreNotFoundError
```

#### `GET /receipts/receipt-lines/{id}`

实现：[findReceiptLine](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:152>)；返回：`ReceiptLine`。

```java
@PathVariable Long id
```

#### `PUT /receipts/{id}`

实现：[changeReceipt](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:158>)；返回：`Receipt`。

```java
@PathVariable Long id,
@RequestBody Receipt receipt
```

#### `POST /receipts/{id}/complete`

实现：[completeReceipt](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:171>)；返回：`Receipt`。

```java
@PathVariable Long id
```

#### `DELETE /receipts`

实现：[removeReceipts](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:183>)；返回：`void`。

```java
@RequestParam(name = "receipt_ids", required = false, defaultValue = "") String receiptIds
```

#### `GET /receipts/{id}/next-line-number`

实现：[getNextReceiptLineNumber](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:195>)；返回：`ResponseBodyWrapper`。

```java
@PathVariable Long id
```

#### `PUT /receipts/{id}/lines`

实现：[addReceiptLine](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:201>)；返回：`ReceiptLine`。

```java
@PathVariable Long id,
@RequestBody ReceiptLine receiptLine
```

#### `POST /receipts/{receiptId}/lines/{receiptLineId}`

实现：[changeReceiptLine](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:214>)；返回：`ReceiptLine`。

```java
@PathVariable Long receiptId,
@PathVariable Long receiptLineId,
@RequestBody ReceiptLine receiptLine
```

#### `POST /receipts/{receiptId}/lines/{receiptLineId}/receive`

实现：[receive](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:235>)；返回：`Inventory`。

```java
@PathVariable Long receiptId,
@PathVariable Long receiptLineId,
@RequestBody Inventory inventory,
@RequestParam(name = "rfCode", defaultValue = "", required = false) String rfCode,
@RequestParam(name = "receiveToStage", defaultValue = "false", required = false) Boolean receiveToStage,
@RequestParam(name = "stageLocation", defaultValue = "", required = false) String stageLocation
```

#### `POST /receipts/lines/{receiptLineId}/recalculate-qc-quantity`

实现：[recalculateQCQuantity](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:295>)；返回：`ReceiptLine`。

```java
@PathVariable Long receiptLineId,
@RequestParam(name = "qcQuantity", required = false, defaultValue = "") Long qcQuantity,
@RequestParam(name = "qcPercentage", required = false, defaultValue = "") Double qcPercentage
```

#### `POST /receipts/{receiptId}/lines/{receiptLineId}/receive-multiple-lpns`

实现：[receive](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:316>)；返回：`List<Inventory>`。

```java
@PathVariable Long receiptId,
@PathVariable Long receiptLineId,
@RequestBody List<Inventory> inventoryList,
@RequestParam(name = "rfCode", defaultValue = "", required = false) String rfCode
```

#### `POST /receipts/{receiptId}/lines/{receiptLineId}/reverse`

实现：[reverseReceivedInventory](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:331>)；返回：`ReceiptLine`。

```java
@PathVariable Long receiptId,
@PathVariable Long receiptLineId,
@RequestParam Long quantity,
@RequestParam(name = "inboundQCRequired", defaultValue = "", required = false) Boolean inboundQCRequired,
@RequestParam(name = "reverseQCQuantity", defaultValue = "", required = false) Boolean reverseQCQuantity
```

#### `PUT /receipts/{id}/check-in`

实现：[checkInReceipt](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:347>)；返回：`Receipt`。

```java
@PathVariable Long id
```

#### `GET /receipts/{id}/inventories`

实现：[findInventoryByReceipt](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:358>)；返回：`List<Inventory>`。

```java
@PathVariable Long id
```

#### `GET /receipts/inventories`

实现：[findInventoryByReceipts](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:363>)；返回：`List<Inventory>`。

```java
@RequestParam Long warehouseId,
@RequestParam String receiptIds
```

#### `DELETE /receipts/lines`

实现：[removeReceiptLine](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:374>)；返回：`void`。

```java
@RequestParam String receiptLineIds
```

#### `POST /receipts/{id}/receiving-document`

实现：[generateReceivingDocument](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:387>)；返回：`ReportHistory`。

```java
@PathVariable Long id,
@RequestParam(name = "locale", defaultValue = "", required = false) String locale,
@RequestParam(name = "printerName", defaultValue = "", required = false) String printerName
```

#### `POST /receipts/{id}/putaway-document`

实现：[generatePutawayDocument](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:397>)；返回：`ReportHistory`。

```java
@PathVariable Long id,
@RequestParam(name = "inventoryIds", defaultValue = "", required = false) String inventoryIds,
@RequestParam(name = "notPutawayInventoryOnly", defaultValue = "false", required = false) Boolean notPutawayInventoryOnly,
@RequestParam(name = "locale", defaultValue = "", required = false) String locale,
@RequestParam(name = "printerName", defaultValue = "", required = false) String printerName
```

#### `POST /receipts/receipt-lines/{id}/pre-print-lpn-label`

实现：[generatePrePrintLPNLabel](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:410>)；返回：`ReportHistory`。

```java
@PathVariable Long id,
@RequestParam String lpn,
@RequestParam(name = "quantity", defaultValue = "", required = false) Long inventoryQuantity,
@RequestParam(name = "locale", defaultValue = "", required = false) String locale,
@RequestParam(name = "printerName", defaultValue = "", required = false) String printerName,
@RequestParam(name = "ignoreInventoryQuantity", defaultValue = "false", required = false) Boolean ignoreInventoryQuantity
```

#### `POST /receipts/receipt-lines/{id}/pre-print-lpn-report`

实现：[generatePrePrintLPNReport](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:425>)；返回：`ReportHistory`。

```java
@PathVariable Long id,
@RequestParam String lpn,
@RequestParam(name = "quantity", defaultValue = "", required = false) Long inventoryQuantity,
@RequestParam(name = "locale", defaultValue = "", required = false) String locale,
@RequestParam(name = "printerName", defaultValue = "", required = false) String printerName,
@RequestParam(name = "ignoreInventoryQuantity", defaultValue = "false", required = false) Boolean ignoreInventoryQuantity
```

#### `POST /receipts/receipt-lines/{id}/pre-print-lpn-label/batch`

实现：[generatePrePrintLPNLabelInBatch](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:441>)；返回：`ReportHistory`。

```java
@PathVariable Long id,
@RequestParam String lpn,
@RequestParam(name = "inventoryQuantity", defaultValue = "", required = false) Long inventoryQuantity,
@RequestParam(name = "count", defaultValue = "1", required = false) Integer count,
@RequestParam(name = "locale", defaultValue = "", required = false) String locale,
@RequestParam(name = "printerName", defaultValue = "", required = false) String printerName,
@RequestParam(name = "ignoreInventoryQuantity", defaultValue = "false", required = false) Boolean ignoreInventoryQuantity
```

#### `POST /receipts/{id}/pre-print-lpn-label/batch`

实现：[generateReceiptPrePrintLPNLabelInBatch](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:459>)；返回：`ReportHistory`。

```java
@PathVariable Long id,
@RequestParam(name = "lpn", defaultValue = "", required = false) String lpn,
@RequestParam(name = "locale", defaultValue = "", required = false) String locale,
@RequestParam(name = "printerName", defaultValue = "", required = false) String printerName,
@RequestBody PrintingLPNByReceiptParameters printingLPNByReceiptParameters
```

#### `POST /receipts/receipt-lines/{id}/pre-print-lpn-report/batch`

实现：[generatePrePrintLPNReportInBatch](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:477>)；返回：`ReportHistory`。

```java
@PathVariable Long id,
@RequestParam String lpn,
@RequestParam(name = "quantity", defaultValue = "", required = false) Long inventoryQuantity,
@RequestParam(name = "count", defaultValue = "1", required = false) Integer count,
@RequestParam(name = "locale", defaultValue = "", required = false) String locale,
@RequestParam(name = "printerName", defaultValue = "", required = false) String printerName,
@RequestParam(name = "ignoreInventoryQuantity", defaultValue = "false", required = false) Boolean ignoreInventoryQuantity
```

#### `GET /receipts/lines/available-for-mps`

实现：[getAvailableReceiptLinesForMPS](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:494>)；返回：`List<ReceiptLine>`。

```java
@RequestParam Long warehouseId,
@RequestParam Long itemId
```

#### `POST /receipts/upload`

实现：[uploadReceipts](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:504>)；返回：`ResponseBodyWrapper`。

```java
Long companyId,
Long warehouseId,
@RequestParam(name = "ignoreUnknownFields", defaultValue = "false", required = false) Boolean ignoreUnknownFields,
@RequestParam("file") MultipartFile file
```

#### `GET /receipts/upload/progress`

实现：[getReceiptFileUploadProgress](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:528>)；返回：`ResponseBodyWrapper`。

```java
Long warehouseId,
String key
```

#### `GET /receipts/upload/result`

实现：[getReceiptFileUploadResult](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:537>)；返回：`List<FileUploadResult>`。

```java
Long warehouseId,
String key
```

#### `POST /receipts/receiving-inventory/upload`

实现：[updateReceivingInventory](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:547>)；返回：`ResponseBodyWrapper`。

```java
Long companyId,
Long warehouseId,
@RequestParam(name = "ignoreUnknownFields", defaultValue = "false", required = false) Boolean ignoreUnknownFields,
@RequestParam(name = "removeExistingInventoryWithSameLPN", defaultValue = "true", required = false) Boolean removeExistingInventoryWithSameLPN,
@RequestParam(name = "emptyLocation", defaultValue = "true", required = false) Boolean emptyLocation,
@RequestParam("file") MultipartFile file
```

#### `GET /receipts/receiving-inventory/upload/progress`

实现：[getReceivingInventoryFileUploadProgress](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:575>)；返回：`ResponseBodyWrapper`。

```java
Long warehouseId,
String key
```

#### `GET /receipts/receiving-inventory/upload/result`

实现：[getFileUploadResult](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:584>)；返回：`List<FileUploadResult>`。

```java
Long warehouseId,
String key
```

#### `PUT /receipts/{receiptId}/billable-activities`

实现：[addReceiptBillableActivity](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:592>)；返回：`ReceiptBillableActivity`。

```java
Long warehouseId,
@PathVariable Long receiptId,
@RequestBody ReceiptBillableActivity receiptBillableActivity
```

#### `DELETE /receipts/{receiptId}/billable-activities/{id}`

实现：[removeReceiptBillableActivity](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:601>)；返回：`ResponseBodyWrapper<String>`。

```java
Long warehouseId,
@PathVariable Long receiptId,
@PathVariable Long id
```

#### `POST /receipts/{receiptId}/billable-activities/{id}`

实现：[changeReceiptBillableActivity](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:612>)；返回：`ReceiptBillableActivity`。

```java
Long warehouseId,
@PathVariable Long receiptId,
@PathVariable Long id,
@RequestBody ReceiptBillableActivity receiptBillableActivity
```

#### `PUT /receipts/lines/{receiptLineId}/billable-activities`

实现：[addReceiptLineBillableActivity](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:622>)；返回：`ReceiptLineBillableActivity`。

```java
Long warehouseId,
@PathVariable Long receiptLineId,
@RequestBody ReceiptLineBillableActivity receiptLineBillableActivity
```

#### `DELETE /receipts/lines/{receiptLineId}/billable-activities/{id}`

实现：[removeReceiptLineBillableActivity](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:630>)；返回：`ResponseBodyWrapper<String>`。

```java
Long warehouseId,
@PathVariable Long receiptLineId,
@PathVariable Long id
```

#### `POST /receipts/lines/{receiptLineId}/billable-activities/{id}`

实现：[changeReceiptLineBillableActivity](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptController.java:639>)；返回：`ReceiptLineBillableActivity`。

```java
Long warehouseId,
@PathVariable Long receiptId,
@PathVariable Long id,
@RequestBody ReceiptLineBillableActivity receiptLineBillableActivity
```

### 客户退货单与退货收货（CustomerReturnOrderController）

#### `GET /customer-return-orders`

实现：[findAllCustomerReturnOrders](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:44>)；返回：`List<CustomerReturnOrder>`。

```java
@RequestParam Long warehouseId,
@RequestParam(name="number", required = false, defaultValue = "") String number,
@RequestParam(name="status_list", required = false, defaultValue = "") String statusList,
@RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails
```

#### `POST /customer-return-orders`

实现：[addCustomerReturnOrder](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:53>)；返回：`CustomerReturnOrder`。

```java
@RequestBody CustomerReturnOrder customerReturnOrder
```

#### `GET /customer-return-orders/{id}`

实现：[findCustomerReturnOrder](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:59>)；返回：`CustomerReturnOrder`。

```java
@PathVariable Long id
```

#### `GET /customer-return-orders/lines/{id}`

实现：[findCustomerReturnOrderLine](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:64>)；返回：`CustomerReturnOrderLine`。

```java
@PathVariable Long id
```

#### `PUT /customer-return-orders/{id}`

实现：[changeCustomerReturnOrder](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:70>)；返回：`CustomerReturnOrder`。

```java
@PathVariable Long id,
@RequestBody CustomerReturnOrder customerReturnOrder
```

#### `POST /customer-return-orders/{id}/complete`

实现：[completeCustomerReturnOrder](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:77>)；返回：`CustomerReturnOrder`。

```java
@PathVariable Long id
```

#### `GET /customer-return-orders/{id}/next-line-number`

实现：[getNextReceiptLineNumber](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:83>)；返回：`ResponseBodyWrapper`。

```java
@PathVariable Long id
```

#### `POST /customer-return-orders/{id}/lines`

实现：[addCustomerReturnOrderLine](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:89>)；返回：`CustomerReturnOrderLine`。

```java
@PathVariable Long id,
@RequestBody CustomerReturnOrderLine customerReturnOrderLine
```

#### `POST /customer-return-orders/{customerReturnOrderId}/lines/{customerReturnOrderLineId}/receive`

实现：[receive](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:103>)；返回：`Inventory`。

```java
@PathVariable Long customerReturnOrderId,
@PathVariable Long customerReturnOrderLineId,
@RequestBody Inventory inventory
```

#### `POST /customer-return-orders/lines/{lineId}/recalculate-qc-quantity`

实现：[recalculateQCQuantity](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:120>)；返回：`CustomerReturnOrderLine`。

```java
@PathVariable Long receiptLineId,
@RequestParam(name = "qcQuantity", required = false, defaultValue = "") Long qcQuantity,
@RequestParam(name = "qcPercentage", required = false, defaultValue = "") Double qcPercentage
```

#### `POST /customer-return-orders/{customerReturnOrderId}/lines/{customerReturnOrderLineId}/receive-multiple-lpns`

实现：[receive](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:135>)；返回：`List<Inventory>`。

```java
@PathVariable Long customerReturnOrderId,
@PathVariable Long customerReturnOrderLineId,
@RequestBody List<Inventory> inventoryList
```

#### `POST /customer-return-orders/{customerReturnOrderId}/lines/{customerReturnOrderLineId}/reverse`

实现：[reverseReceivedInventory](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:143>)；返回：`CustomerReturnOrderLine`。

```java
@PathVariable Long customerReturnOrderId,
@PathVariable Long customerReturnOrderLineId,
@RequestParam Long quantity,
@RequestParam(name = "inboundQCRequired", defaultValue = "", required = false) Boolean inboundQCRequired,
@RequestParam(name = "reverseQCQuantity", defaultValue = "", required = false) Boolean reverseQCQuantity
```

#### `GET /customer-return-orders/{id}/inventories`

实现：[findInventoryByCustomerReturnOrder](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:152>)；返回：`List<Inventory>`。

```java
@PathVariable Long id
```

#### `POST /customer-return-orders/{id}/document`

实现：[generateDocument](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:159>)；返回：`ReportHistory`。

```java
@PathVariable Long id,
@RequestParam(name = "locale", defaultValue = "", required = false) String locale,
@RequestParam(name = "printerName", defaultValue = "", required = false) String printerName
```

#### `POST /customer-return-orders/{id}/putaway-document`

实现：[generatePutawayDocument](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/CustomerReturnOrderController.java:169>)；返回：`ReportHistory`。

```java
@PathVariable Long id,
@RequestParam(name = "inventoryIds", defaultValue = "", required = false) String inventoryIds,
@RequestParam(name = "notPutawayInventoryOnly", defaultValue = "false", required = false) Boolean notPutawayInventoryOnly,
@RequestParam(name = "locale", defaultValue = "", required = false) String locale,
@RequestParam(name = "printerName", defaultValue = "", required = false) String printerName
```

### 采购单查询与转收货单（PurchaseOrderController）

#### `GET /purchase-orders`

实现：[findAllReceipts](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/PurchaseOrderController.java:41>)；返回：`List<PurchaseOrder>`。

```java
@RequestParam Long warehouseId,
@RequestParam(name="number", required = false, defaultValue = "") String number,
@RequestParam(name="purchasOrderStatusList", required = false, defaultValue = "") String purchasOrderStatusList,
@RequestParam(name="supplierName", required = false, defaultValue = "") String supplierName,
@RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId,
@RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails
```

#### `GET /purchase-orders/{id}`

实现：[findPurchaseOrder](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/PurchaseOrderController.java:55>)；返回：`PurchaseOrder`。

```java
@PathVariable Long id
```

#### `POST /purchase-orders/{id}/create-receipt`

实现：[createReceiptFromPurchaseOrder](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/PurchaseOrderController.java:61>)；返回：`Receipt`。

```java
@PathVariable Long id,
@RequestParam Long warehouseId,
@RequestParam String receiptNumber,
@RequestParam(name="allowUnexpectedItem", required = false, defaultValue = "false") Boolean allowUnexpectedItem,
@RequestBody List<CreateReceiptFromPurchaseOrderRequest> createReceiptFromPurchaseOrderRequests
```

### 收货规则与最佳匹配（InboundReceivingConfigurationController）

#### `GET /inbound-receiving-configuration`

实现：[findAllInboundReceivingConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/InboundReceivingConfigurationController.java:38>)；返回：`List<InboundReceivingConfiguration>`。

```java
@RequestParam Long companyId,
@RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId,
@RequestParam(name="itemFamilyId", required = false, defaultValue = "") Long itemFamilyId,
@RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
@RequestParam(name="warehouseId", required = false, defaultValue = "") Long warehouseId
```

#### `GET /inbound-receiving-configuration/best-match`

实现：[findBestMatchInboundReceivingConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/InboundReceivingConfigurationController.java:49>)；返回：`InboundReceivingConfiguration`。

```java
@RequestParam Long companyId,
@RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId,
@RequestParam(name="itemFamilyId", required = false, defaultValue = "") Long itemFamilyId,
@RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
@RequestParam(name="warehouseId", required = false, defaultValue = "") Long warehouseId
```

#### `PUT /inbound-receiving-configuration`

实现：[addInboundReceivingConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/InboundReceivingConfigurationController.java:63>)；返回：`InboundReceivingConfiguration`。

```java
@RequestBody InboundReceivingConfiguration inboundReceivingConfiguration
```

#### `GET /inbound-receiving-configuration/{id}`

实现：[findInboundReceivingConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/InboundReceivingConfigurationController.java:69>)；返回：`InboundReceivingConfiguration`。

```java
@PathVariable Long id
```

#### `POST /inbound-receiving-configuration/{id}`

实现：[changeInboundReceivingConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/InboundReceivingConfigurationController.java:76>)；返回：`InboundReceivingConfiguration`。

```java
@PathVariable Long id,
@RequestBody InboundReceivingConfiguration inboundReceivingConfiguration
```

#### `DELETE /inbound-receiving-configuration/{id}`

实现：[removeInboundReceivingConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/InboundReceivingConfigurationController.java:83>)；返回：`void`。

```java
@PathVariable Long id
```

### 入库质检规则（InboundQCConfigurationController）

#### `GET /inbound-qc-configuration`

实现：[findAllInboundQCConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/InboundQCConfigurationController.java:36>)；返回：`List<InboundQCConfiguration>`。

```java
@RequestParam Long companyId,
@RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId,
@RequestParam(name="itemFamilyId", required = false, defaultValue = "") Long itemFamilyId,
@RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
@RequestParam(name="fromInventoryStatusId", required = false, defaultValue = "") Long fromInventoryStatusId,
@RequestParam(name="warehouseId", required = false, defaultValue = "") Long warehouseId
```

#### `PUT /inbound-qc-configuration`

实现：[addInboundQCConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/InboundQCConfigurationController.java:49>)；返回：`InboundQCConfiguration`。

```java
@RequestBody InboundQCConfiguration inboundQCConfiguration
```

#### `GET /inbound-qc-configuration/{id}`

实现：[findInboundQCConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/InboundQCConfigurationController.java:55>)；返回：`InboundQCConfiguration`。

```java
@PathVariable Long id
```

#### `POST /inbound-qc-configuration/{id}`

实现：[changeInboundQCConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/InboundQCConfigurationController.java:62>)；返回：`InboundQCConfiguration`。

```java
@PathVariable Long id,
@RequestBody InboundQCConfiguration inboundQCConfiguration
```

#### `DELETE /inbound-qc-configuration/{id}`

实现：[removeInboundQCConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/InboundQCConfigurationController.java:69>)；返回：`void`。

```java
@PathVariable Long id
```

### 上架规则与库位分配（PutawayConfigurationController）

#### `GET /putaway-configuration`

实现：[findAllPutawayConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/PutawayConfigurationController.java:39>)；返回：`List<PutawayConfiguration>`。

```java
@RequestParam Long warehouseId,
@RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
@RequestParam(name="sequence", required = false, defaultValue = "") Integer sequence,
@RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
@RequestParam(name="itemFamilyName", required = false, defaultValue = "") String itemFamilyName,
@RequestParam(name="inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId
```

#### `PUT /putaway-configuration`

实现：[addPutawayConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/PutawayConfigurationController.java:50>)；返回：`PutawayConfiguration`。

```java
@RequestBody PutawayConfiguration putawayConfiguration
```

#### `GET /putaway-configuration/{id}`

实现：[findPutawayConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/PutawayConfigurationController.java:56>)；返回：`PutawayConfiguration`。

```java
@PathVariable Long id
```

#### `POST /putaway-configuration/{id}`

实现：[changePutawayConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/PutawayConfigurationController.java:62>)；返回：`PutawayConfiguration`。

```java
@RequestBody PutawayConfiguration putawayConfiguration
```

#### `DELETE /putaway-configuration`

实现：[removePutawayConfigurations](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/PutawayConfigurationController.java:68>)；返回：`void`。

```java
@RequestParam(name = "putaway_configuration_ids", required = false, defaultValue = "") String putawayConfigurationIds
```

#### `DELETE /putaway-configuration/{id}`

实现：[removePutawayConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/PutawayConfigurationController.java:74>)；返回：`ResponseBodyWrapper<String>`。

```java
@PathVariable Long id
```

#### `POST /putaway-configuration/allocate-location`

实现：[allocateLocation](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/PutawayConfigurationController.java:82>)；返回：`Inventory`。

```java
@RequestBody Inventory inventory
```

#### `POST /putaway-configuration/reallocate-location`

实现：[reallocateLocation](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/PutawayConfigurationController.java:89>)；返回：`Inventory`。

```java
@RequestBody Inventory inventory
```

### 收货操作流水（ReceivingTransactionController）

#### `GET /receiving-transactions`

实现：[findAllReceivingTransactions](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceivingTransactionController.java:36>)；返回：`List<ReceivingTransaction>`。

```java
@RequestParam Long warehouseId,
@RequestParam(name="receiptId", required = false, defaultValue = "") Long receiptId,
@RequestParam(name="receiptLineId", required = false, defaultValue = "") Long receiptLineId,
@RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails
```

### 收货计费活动查询（ReceiptBillableActivitiesController）

#### `GET /receipt-billable-activities/billable-activity`

实现：[findBillableActivities](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ReceiptBillableActivitiesController.java:49>)；返回：`List<BillableActivity>`。

```java
@RequestParam Long warehouseId,
@RequestParam Long clientId,
@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endTime,
@RequestParam(name="includeLineActivity", required = false, defaultValue = "true") Boolean includeLineActivity,
ClientRestriction clientRestriction
```

### Eulogia 客户装箱单导入（EulogiaController）

#### `POST /eulogia/customer-packing-slip/upload`

实现：[uploadCustomerPackingSlip](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/EulogiaController.java:50>)；返回：`ResponseBodyWrapper`。

```java
Long companyId,
Long warehouseId,
@RequestParam(name = "ignoreUnknownFields", defaultValue = "false", required = false) Boolean ignoreUnknownFields,
@RequestParam("file") MultipartFile file
```

#### `GET /eulogia/customer-packing-slip/upload/progress`

实现：[getCustomerPackingSlipFileUploadProgress](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/EulogiaController.java:77>)；返回：`ResponseBodyWrapper`。

```java
Long warehouseId,
String key
```

#### `GET /eulogia/customer-packing-slip/upload/result`

实现：[getCustomerPackingSlipFileUploadResult](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/EulogiaController.java:86>)；返回：`List<FileUploadResult>`。

```java
Long warehouseId,
String key
```

### 入库相关货品引用替换（InboundController）

#### `POST /inbound-configuration/item-override`

实现：[handleItemOverride](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/InboundController.java:49>)；返回：`ResponseBodyWrapper<String>`。

```java
@RequestParam Long warehouseId,
@RequestParam Long oldItemId,
@RequestParam Long newItemId
```

### 存活探针（访问数据库）（ProbeController）

#### `GET /probe/live`

实现：[liveProbe](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/ProbeController.java:40>)；返回：`ResponseBodyWrapper<String>`。

无方法参数。

### 应用信息（WebSiteInformationController）

#### `GET /app`

实现：[getBasicApplicationInformation](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/WebSiteInformationController.java:33>)；返回：`String`。

无方法参数。

### 测试数据查看、初始化与清理（TestDataInitController）

#### `GET /test-data`

实现：[getTestDataNames](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/TestDataInitController.java:33>)；返回：`String[]`。

无方法参数。

#### `POST /test-data/init`

实现：[init](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/TestDataInitController.java:38>)；返回：`ResponseBodyWrapper<String>`。

```java
@RequestParam Long companyId,
@RequestParam String warehouseName
```

#### `POST /test-data/init/{name}`

实现：[init](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/TestDataInitController.java:45>)；返回：`ResponseBodyWrapper<String>`。

```java
@RequestParam Long companyId,
@PathVariable String name,
@RequestParam String warehouseName
```

#### `POST /test-data/clear`

实现：[clear](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/controller/TestDataInitController.java:52>)；返回：`ResponseBodyWrapper<String>`。

```java
@RequestParam Long warehouseId
```

## 关键请求与响应模型字段

以下为模型中声明的主要实例字段，方便定位源码；不是请求必填清单，也不包含所有继承或计算属性。

### [Receipt](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/model/Receipt.java>)

```java
private Long id;
private String number;
private Long warehouseId;
private Warehouse warehouse;
private Long clientId;
private Client client;
private Long supplierId;
private String transferOrderNumber;
private Long transferOrderWarehouseId;
private ReceiptCategory category = ReceiptCategory.PURCHASE_ORDER;
private PurchaseOrder purchaseOrder;
private Supplier supplier;
private List<ReceiptLine> receiptLines = new ArrayList<>();
private List<ReceiptBillableActivity> receiptBillableActivities = new ArrayList<>();
private ReceiptStatus receiptStatus = ReceiptStatus.OPEN;
private Boolean allowUnexpectedItem = false;
private ZonedDateTime checkInTime;
```

### [ReceiptLine](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/model/ReceiptLine.java>)

```java
private Long id;
private String number;
private PurchaseOrderLine purchaseOrderLine;
private List<ReceiptLineBillableActivity> receiptLineBillableActivities = new ArrayList<>();
private Long warehouseId;
private Warehouse warehouse;
private Long itemId;
private Item item;
private Long expectedQuantity;
private Long arrivedQuantity;
private Long receivedQuantity = 0L;
private Receipt receipt;
private Long receiptId;
private String receiptNumber;
private Long overReceivingQuantity = 0L;
private Double overReceivingPercent = 0.0;
private Long qcQuantity = 0l;
private Double qcPercentage = 0.0;
private Long qcQuantityRequested = 0L;
private String color;
private String productSize;
private String style;
private Long itemPackageTypeId;
private ItemPackageType itemPackageType;
private Double cubicMeter;
private String inventoryAttribute1;
private String inventoryAttribute2;
private String inventoryAttribute3;
private String inventoryAttribute4;
private String inventoryAttribute5;
private Long inventoryStatusId;
private InventoryStatus inventoryStatus;
```

### [Inventory](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/model/Inventory.java>)

```java
private Long id;
private Client client;
private Long clientId;
private String lpn;
private Long locationId;
private Item item;
private ItemPackageType itemPackageType;
private Long quantity;
private Boolean virtual;
private Long customerReturnOrderId;
private Long customerReturnOrderLineId;
private InventoryStatus inventoryStatus;
private Location location;
private Long receiptId;
private String receiptNumber;
private Long receiptLineId;
private String receiptLineNumber;
private List<InventoryMovement> inventoryMovements;
private Long warehouseId;
private Warehouse warehouse;
private Boolean inboundQCRequired = false;
private ZonedDateTime fifoDate;
private ZonedDateTime inWarehouseDatetime;
private String color;
private String productSize;
private String style;
private String attribute1;
private String attribute2;
private String attribute3;
private String attribute4;
private String attribute5;
private Boolean kitInventoryFlag = false;
private Boolean kitInnerInventoryFlag = false;
private Inventory kitInventory;
private List<Inventory> kitInnerInventories = new ArrayList<>();
```

### [CustomerReturnOrder](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/model/CustomerReturnOrder.java>)

```java
private Long id;
private String number;
private Long warehouseId;
private Warehouse warehouse;
private Long clientId;
private Client client;
private String rmaNumber;
private String trackingNumber;
private Long customerId;
private Customer customer;
private ReceiptCategory category = ReceiptCategory.CUSTOMER_RETURN;
private List<CustomerReturnOrderLine> customerReturnOrderLines = new ArrayList<>();
private ReceiptStatus status = ReceiptStatus.OPEN;
private Boolean allowUnexpectedItem = false;
```

### [CustomerReturnOrderLine](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/model/CustomerReturnOrderLine.java>)

```java
private Long id;
private String number;
private Long warehouseId;
private Warehouse warehouse;
private Long itemId;
private Item item;
private Long expectedQuantity;
private Long receivedQuantity = 0L;
private CustomerReturnOrder customerReturnOrder;
private Long overReceivingQuantity = 0L;
private Double overReceivingPercent = 0.0;
private Long qcQuantity = 0l;
private Double qcPercentage = 0.0;
private Long qcQuantityRequested = 0L;
private Long outboundOrderLineId;
private Long customerReturnOrderId;
private String customerReturnOrderNumber;
private String outboundOrderNumber;
```

### [CreateReceiptFromPurchaseOrderRequest](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/model/CreateReceiptFromPurchaseOrderRequest.java>)

```java
Long purchaseOrderLineId;
Long quantity;
```

### [PrintingLPNByReceiptParameters](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/model/PrintingLPNByReceiptParameters.java>)

```java
private Map<Long, Integer> lpnLabelCountByReceiptLines;
private Map<Long, Long> lpnQuantityOnLabelByReceiptLines;
private Map<Long, Boolean> ignoreInventoryQuantityByReceiptLines;
```

### [InboundReceivingConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/model/InboundReceivingConfiguration.java>)

```java
private Long id;
private Long supplierId;
private Supplier supplier;
private Long itemFamilyId;
private ItemFamily itemFamily;
private Long itemId;
private Item item;
private Long warehouseId;
private Warehouse warehouse;
private Long companyId;
private Company company;
private Double standardPalletSize;
private Boolean estimatePalletCountBySize;
private Boolean estimatePalletCountByReceiptLineCubicMeter;
private ReceiptStatus statusAllowReceiptChangeWhenUploadFile;
private ReceiptStatus statusAllowReceiptChangeFromIntegration;
private ReceiptStatus statusAllowReceiptChangeFromWebUI;
private Boolean useReceiptCheckInTimeAsInWarehouseDateTime;
private Boolean validateOverReceivingAgainstArrivedQuantity;
```

### [InboundQCConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/model/InboundQCConfiguration.java>)

```java
private Long id;
private Long supplierId;
private Supplier supplier;
private Long itemFamilyId;
private ItemFamily itemFamily;
private Long itemId;
private Item item;
private Long warehouseId;
private Warehouse warehouse;
private Long companyId;
private Company company;
private Long qcQuantityPerReceipt;
private Double qcPercentage;
private Long fromInventoryStatusId;
private InventoryStatus fromInventoryStatus;
private Long toInventoryStatusId;
private InventoryStatus toInventoryStatus;
```

### [PutawayConfiguration](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/model/PutawayConfiguration.java>)

```java
private Long id;
private Integer sequence;
private Long warehouseId;
private Warehouse warehouse;
private Long itemId;
private Item item;
private Long clientId;
private Client client;
private Long itemFamilyId;
private ItemFamily itemFamily;
private Long inventoryStatusId;
private InventoryStatus inventoryStatus;
private Long locationId;
private Location location;
private Long locationGroupId;
private LocationGroup locationGroup;
private Long locationGroupTypeId;
private LocationGroupType locationGroupType;
private String strategies;
private List<PutawayConfigurationStrategy> putawayConfigurationStrategies = new ArrayList<>();
```

### [ReportHistory](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/model/ReportHistory.java>)

```java
private Long id;
private Long warehouseId;
private Warehouse warehouse;
private String printedUsername;
private String description;
private ReportType type;
private String fileName;
```

### [FileUploadResult](</Users/xiangxiangwang/Documents/ChatGPT/MES APP/inboundsvr/src/main/java/com/garyzhangscm/cwms/inbound/model/FileUploadResult.java>)

```java
private int lineNumber;
private String record;
private String result;
private String errorMessage;
```
