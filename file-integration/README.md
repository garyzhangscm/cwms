# Fay Oracle Item 文件导入

此分支为 Fay 单独维护。先核对 Fay 的实际接口、基础资料和 FTP 文件，再部署联调。

## 数据路径

Oracle FTP CSV → Fay app2 文件适配器 → Fay integrationservice → Kafka → 库存服务 → 完成回执。

文件适配器不直连 Oracle、不直接写 MES 数据库。HTTP 接收成功不等于 Item 已创建；只有查询到 `COMPLETED` 才算业务完成。已有物料按公司、仓库和料号核对后跳过，不覆盖。

## Fay 配置

复制 `config.fay-oracle-items.example.json` 为服务器上的 `/etc/cwms-fay-oracle-items/config.json`。
这是待补齐的模板，不是可立即上线的配置：

- FTP：`192.168.20.118:21`，目录 `/WIS`；只读取 `fayint_item` 开头且带批次号的 CSV。账号待配置。
- integration：暂定使用现有 NodePort `30681`；后端已部署并通过启动和只读接口验证；首次写入/完成回执待小批验证。
- inventory：`http://10.0.11.174:30330`；已通过 Fay 接口确认公司 ID `1`、仓库 ID `1`。
- 公司代码 `20901`、仓库名称 `WMEC` 已确认；`01` 映射 `Finish Good`，`RM` 映射 `Raw materials`（均已核对 Fay 分类名称）。
- 用户确认沿用 PCS/CS/PL 包装规则。尺寸重量沿用源模板值 1，不代表实测值。
- 状态目录：`/var/lib/cwms-fay-oracle-items/state`，Fay 单独使用并持久化。
- FTP 凭据使用服务器上的 `ftp-user` 和 `ftp-password` 文件，不提交到 Git。
- `deleteSourceFiles: false`：首次联调保留 FTP 源文件，报告显示 `DISABLED`。完成后仍会跳过已完成批次，继续处理后续批次；重新轮询不会重复提交。

Fay 配置使用 `oracleItems.filenamePrefix: "fayint_item"`，例如 `fayint_item_20261002_001.csv`；同目录的 `int_item` 或其他文件不下载、不提交、不删除。文件名必须含批次号及 `.csv` 后缀。未配置此前缀的上游模板仍默认识别 `int_item`。

## 离线验证

```sh
cd file-integration
python3 -m unittest discover -s tests -v
python3 adapter.py validate examples/fayint_item_demo001.csv --config config.fay-oracle-items.example.json
```

离线测试不会联网或写入 MES。示例物料和配置不代表 Fay 的实际数据。

## FTP 凭据

在 Fay app2 的 root 终端运行 `/usr/local/sbin/fay-item-save-ftp`，交互输入 FTP 账号和密码。对应脚本为 `deploy/fay/save-ftp-credentials.py`，仅设置 Fay FTP，不修改 VPN。服务器须先建立 `cwmsfayitem` 系统组。凭据文件为 root 所有、组 cwmsfayitem 可读、权限 0640，目录权限 0750。密码不进入命令参数、日志或仓库。

## 单批联调

确认 FTP 目录、Fay 基础资料、后端部署和样例文件后，才使用下列命令。它会写入配置指定的 MES，但 Fay 模板默认不会删除 FTP 源文件。

```sh
python3 adapter.py run --config /etc/cwms-fay-oracle-items/config.json --send --once --file CONFIRMED_FAY_ITEM_BATCH.csv
```

首次扫描只记录文件指纹；至少等待 60 秒再扫描，确认文件内容稳定后提交。未知提交结果不自动重发；业务错误保留原文件和本地报告。

`deploy/fay/cwms-fay-oracle-items.service` 为手动单批联调用的服务模板，默认读取 `batch-file` 中指定的文件名。定时批量导入在首次验证完成且执行时间确认后再配置。

上游实现细节和历史测试说明保存在 `REFERENCE-IMPLEMENTATION.md`、`REFERENCE-ITEM-PILOT.md`，不作为 Fay 已验证的证据。

Fay 现场进度、部署镜像和数据库兼容适配记录见 `FAY-MIGRATION.md`。Oracle 大写列头 `SEGMENT1,DESCRIPTION,ITEM_TYPE,PIECES_PER_CARTON,PIECES_PER_PALLET` 与参考实现一致，已通过实际文件的离线校验。
