from typing import AsyncGenerator
from fastapi import Depends, Request
from redis.asyncio import Redis
from sqlalchemy.ext.asyncio import AsyncConnection, AsyncSession

from app.core.providers import AkShareProvider, TuShareProvider
from app.client.spring_client import SpringClient
from app.service.collection.task_run_service import TaskRunService
from app.service.analytics.market_service import MarketService
from app.service.analytics.stock_service import StockService
from app.service.analytics.index_service import IndexService
from app.core.db import BasicDataSession, ExchangeDataSession, FinanceDataSession, basic_data_engine, exchange_data_engine, finance_data_engine


ak = AkShareProvider()
ts = TuShareProvider()

def get_spring_client() -> SpringClient:
    return SpringClient()

def get_task_run_service() -> TaskRunService:
    return TaskRunService(spring_client=get_spring_client())

def get_redis_client(request: Request) -> Redis:
    return request.app.state.redis

# FastAPI 依赖注入，获取异步数据库 session，session 操作用于 orm 模型（将表构建为类）的增删改查，使用完毕后会自动关闭连接
async def get_basic_session() -> AsyncGenerator[AsyncSession, None]:
    async with BasicDataSession() as db:
        yield db


async def get_exchange_session() -> AsyncGenerator[AsyncSession, None]:
    async with ExchangeDataSession() as db:
        yield db


async def get_finance_session() -> AsyncGenerator[AsyncSession, None]:
    async with FinanceDataSession() as db:
        yield db


# 数据库的 connection 依赖，直接返回 connection 对象，适用于不使用 orm 模型的原生 sql 查询
async def get_basic_conn() -> AsyncGenerator[AsyncConnection, None]:
    async with basic_data_engine.connect() as conn:
        yield conn

async def get_exchange_conn() -> AsyncGenerator[AsyncConnection, None]:
    async with exchange_data_engine.connect() as conn:
        yield conn

async def get_finance_conn() -> AsyncGenerator[AsyncConnection, None]:
    async with finance_data_engine.connect() as conn:
        yield conn


def get_market_service(
    basic_conn: AsyncConnection = Depends(get_basic_conn),
    exchange_conn: AsyncConnection = Depends(get_exchange_conn),
) -> MarketService:
    return MarketService(basic_conn=basic_conn, exchange_conn=exchange_conn)


def get_stock_service(
    basic_conn: AsyncConnection = Depends(get_basic_conn),
    exchange_conn: AsyncConnection = Depends(get_exchange_conn),
    finance_conn: AsyncConnection = Depends(get_finance_conn)
) -> StockService:
    return StockService(basic_conn=basic_conn, exchange_conn=exchange_conn, finance_conn=finance_conn)

def get_index_service(
    basic_conn: AsyncConnection = Depends(get_basic_conn),
    exchange_conn: AsyncConnection = Depends(get_exchange_conn),
    finance_conn: AsyncConnection = Depends(get_finance_conn),
    redis_client: Redis = Depends(get_redis_client)
) -> IndexService:
    return IndexService(basic_conn=basic_conn, exchange_conn=exchange_conn, finance_conn=finance_conn, redis_client=redis_client)
