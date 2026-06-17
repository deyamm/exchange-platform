# Spring Boot + Vue + FastAPI MVP Project Structure

这是一个最小可用项目骨架，包含：

- 两个 Spring Boot 后台服务：`system-service`、`biz-service`
- 一个 Vue 前端项目：`admin-web`
- 一个 FastAPI 数据处理服务：`data-service`
- Java 公共模块：`common-core`、`common-web-starter`
- 跨语言接口契约目录：`contracts`
- 本地部署目录：`infra`

## 推荐调用关系

```text
Vue admin-web
    |
    v
Nginx / Gateway / BFF
    |
    |----> system-service-server
    |
    |----> biz-service-server
              |
              v
        data-service FastAPI
```

## Java 模块原则

```text
api     只放 DTO / Request / Response / Enum 等契约
client  只放远程调用封装
server  只放服务实现
common  只放真正通用的基础能力
```
