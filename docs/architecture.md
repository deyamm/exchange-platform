# Architecture

## MVP 调用关系

```text
admin-web(Vue)
    |
    v
Nginx / Gateway
    |
    |----> system-service-server
    |----> biz-service-server
              |
              v
        data-service(FastAPI)
```

## 模块边界

- Java 服务之间：`api + client`
- Java 调 FastAPI：`OpenAPI` 或 `data-service-client`
- Vue 调后端：统一 `/api` 入口
