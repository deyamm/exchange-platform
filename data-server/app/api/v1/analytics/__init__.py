from fastapi import APIRouter

from .analytics_api import router

# /api/v1/data-analytics
analytics_router = APIRouter(prefix='/data-analytics', tags=['data-analytics'])

analytics_router.include_router(router)