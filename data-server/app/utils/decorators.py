
from functools import wraps
import hashlib
import inspect
import json
import logging

from redis import RedisError

from app.utils.datetime_utils import resolve_trade_date

logger = logging.getLogger(__name__)

def stock_response_cache(cache_name: str, ttl_seconds: int):
    """股票详情接口 Redis 缓存装饰器。"""

    def decorator(func):
        signature = inspect.signature(func)

        @wraps(func)
        async def wrapper(self, *args, **kwargs):
            # 1. 获取完整参数，补齐函数默认值
            bound = signature.bind(self, *args, **kwargs)
            bound.apply_defaults()

            # 2. 标准化股票代码和交易日期
            bound.arguments["full_symbol"] = (
                str(bound.arguments["full_symbol"]).strip().upper()
            )

            if "trade_date" in bound.arguments:
                bound.arguments["trade_date"] = resolve_trade_date(
                    bound.arguments["trade_date"]
                )

            params = {
                key: value
                for key, value in bound.arguments.items()
                if key != "self"
            }

            # 3. 使用全部有效参数生成缓存 Key
            params_json = json.dumps(
                params,
                sort_keys=True,
                ensure_ascii=False,
                default=str,
                separators=(",", ":"),
            )

            params_hash = hashlib.sha256(
                params_json.encode("utf-8")
            ).hexdigest()[:20]

            cache_key = (
                f"stock:detail:v1:{cache_name}:"
                f"{params['full_symbol']}:{params_hash}"
            )

            # 4. 优先从 Redis 读取
            try:
                cached = await self.redis_client.get(cache_key)

                if cached is not None:
                    logger.debug(
                        "Stock cache hit: %s", cache_key
                    )
                    return json.loads(cached)

            except (RedisError, ValueError, TypeError) as exc:
                logger.warning(
                    "Stock cache read failed: %s, error=%s",
                    cache_key, exc
                )

            # 5. 缓存未命中，执行原始业务逻辑
            result = await func(*bound.args, **bound.kwargs)

            # 6. 数据不完整时缩短缓存时间
            actual_ttl = ttl_seconds

            if isinstance(result, dict):
                meta = result.get("meta") or {}
                if meta.get("partialData"):
                    actual_ttl = min(ttl_seconds, 60)

            # 7. 写入 Redis，使用 SET EX 设置过期时间
            try:
                serialized = json.dumps(
                    result,
                    ensure_ascii=False,
                    default=str,
                )

                await self.redis_client.set(
                    cache_key,
                    serialized,
                    ex=actual_ttl,
                )

                logger.debug(
                    "Stock cache stored: %s, ttl=%s",
                    cache_key, actual_ttl
                )

            except (RedisError, TypeError, ValueError) as exc:
                logger.warning(
                    "Stock cache write failed: %s, error=%s",
                    cache_key, exc
                )

            return result

        return wrapper

    return decorator