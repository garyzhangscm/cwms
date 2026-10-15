# CWMS 本地开发环境

本地代码以 `origin/v1.64` 为基准，用于 MES iOS 配套 API 和数据端开发。

## 编译

已在 `.local/tools/` 安装 Apple Silicon 版 Temurin JDK 17 和 Maven 3.9.11。
`scripts/mvn-local` 自动选择这套 JDK，依赖缓存位于 `.local/m2/repository/`。
这些工具仅保存在这台机器上，不随 Git 分发。

在仓库根目录执行：

```sh
./scripts/mvn-local -version
./scripts/mvn-local -B -ntp -DskipTests package
# 只编译指定服务：
./scripts/mvn-local -B -ntp -f adminsvr/pom.xml -DskipTests package
```

根 POM 包含 14 个服务模块，比发布脚本多一个 `zuulsvr`。
构建产物在各服务的 `target/` 下，现有 Dockerfile 引用 `v1.64.jar`。
根 POM 的 `QuickBook` 大小写与 Git 中的 `quickbook` 不一致；本机文件系统允许解析，
在大小写敏感的 Linux 文件系统上执行根目录构建前需要修正。

多数模块在 POM 中设置了 `maven.test.skip=true`。上述命令验证编译和打包，
不代表 Spring 应用启动、数据库访问或接口测试通过。运行测试前需要准备服务配置和数据库。

## Docker

Docker Desktop 安装在 `/Applications/Docker.app`。
`scripts/docker-local` 使用它附带的 CLI，并明确选择本机 `desktop-linux` context。

```sh
./scripts/docker-local version
./scripts/docker-local buildx ls
./scripts/docker-local buildx build --platform linux/arm64 \
  -t cwms-adminserver:local-v1.64 --load adminsvr
```

正式发布仍使用各服务原有 Dockerfile 和 `garyzhangscm` 镜像名称。
本次环境配置不包含向 Docker Hub 推送或远端部署。

## 本地基础服务

`docker/local/compose.yml` 提供独立的 MySQL 8.0、Redis 7.2、Kafka 3.9.1 开发容器。
Kafka 使用单节点 KRaft。版本是本地开发基线，仍需与实际部署配置核对。
本地随机数据库密码保存在 `.local/infra.env`，不加入 Git。

```sh
./scripts/docker-local compose --env-file .local/infra.env \
  -f docker/local/compose.yml up -d --wait
./scripts/docker-local compose --env-file .local/infra.env \
  -f docker/local/compose.yml ps
# 停止容器，保留数据库卷：
./scripts/docker-local compose --env-file .local/infra.env \
  -f docker/local/compose.yml down
```

| 服务 | 本机地址 | 说明 |
| --- | --- | --- |
| MySQL | `127.0.0.1:13306` | 空库 `cwms_local`，用户 `cwms_local` |
| Redis | `127.0.0.1:16379` | 仅本机可访问 |
| Kafka | `localhost:19092` | 同一 Compose 网络内使用 `kafka:29092` |

这些容器仅提供基础依赖；尚未导入 CWMS 业务表、基础数据或服务配置。
完整联调需要与 v1.64 匹配的数据库建表/升级脚本、各服务的外部配置、
本地认证配置和服务路由。仓库 README 指向 `garyzhangscm/cwms-setup`，
但没有证明其中所有初始化脚本与本次分支一致，不能直接当成已验证的运行环境。

## 验证记录

2026-09-20 在 `v1.64` 分支上验证：

- Maven 根项目 `package` 成功，14 个服务均生成可执行 JAR（跳过测试）。
- 发布清单中 13 个服务的 Dockerfile 所引用 JAR 均存在。
- `adminserver` 使用原 Dockerfile 完成 amd64/arm64 双架构构建并加载到本机；
  两种架构均执行 `java -version` 成功，尚未验证业务应用启动。
- MySQL、Redis、Kafka 均为 healthy；MySQL 本地账号查询、Redis PING 和 Kafka 元数据查询成功。

本次安装和构建日志保存在 `.local/logs/`，包括 Maven 打包、基础服务启动和 Docker 构建日志。
JDK 安装包已通过官方 SHA256 校验，Maven 安装包已通过官方 SHA512 校验。

参考：[Temurin 安装](https://adoptium.net/installation/ci-scripts)、
[Maven 安装](https://maven.apache.org/install)、
[Kafka 官方 Docker 镜像](https://kafka.apache.org/39/getting-started/docker/)。
