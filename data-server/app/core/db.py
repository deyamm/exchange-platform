
import redis.asyncio as redis
from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.orm import declarative_base

from app.core.config import settings


# 注意：
# 这里的 URI 必须使用 SQLAlchemy 异步驱动。
#
# 示例：
# PostgreSQL: postgresql+asyncpg://user:password@host:5432/dbname
# MySQL:      mysql+aiomysql://user:password@host:3306/dbname
# MySQL:      mysql+asyncmy://user:password@host:3306/dbname
# SQLite:     sqlite+aiosqlite:///./test.db


# create async engine for basic data, exchange_data, finance_data
basic_data_engine = create_async_engine(
    settings.BASIC_DATA_URI,
    pool_pre_ping=True,
)

exchange_data_engine = create_async_engine(
    settings.EXCHANGE_DATA_URI,
    pool_pre_ping=True,
)

finance_data_engine = create_async_engine(
    settings.FINANCE_DATA_URI,
    pool_pre_ping=True,
)


# async session factories
BasicDataSession = async_sessionmaker(
    bind=basic_data_engine,
    class_=AsyncSession,
    expire_on_commit=False,
)

ExchangeDataSession = async_sessionmaker(
    bind=exchange_data_engine,
    class_=AsyncSession,
    expire_on_commit=False,
)

FinanceDataSession = async_sessionmaker(
    bind=finance_data_engine,
    class_=AsyncSession,
    expire_on_commit=False,
)


# used for db models
BasicBase = declarative_base()
ExchangeBase = declarative_base()
FinanceBase = declarative_base()


def get_engine_by_schema(schema_name: str):
    if schema_name == "type_basic_data":
        return basic_data_engine
    elif schema_name == "type_exchange_data":
        return exchange_data_engine
    elif schema_name == "type_finance_data":
        return finance_data_engine
    else:
        raise ValueError(f"不支持的 schema: {schema_name}")


async def dispose_all_engines() -> None:
    await basic_data_engine.dispose()
    await exchange_data_engine.dispose()
    await finance_data_engine.dispose()


def create_redis_client() -> redis.Redis:
    """
    创建一个 Redis 客户端实例。
    """
    return redis.from_url(
        settings.REDIS_URI, 
        encoding="utf-8", 
        decode_responses=True, 
        health_check_interval=30
    )