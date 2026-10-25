from fastapi import APIRouter
from .collection import collection_task_router
from .analytics import analytics_router

# /api/v1
router = APIRouter(prefix='/v1')

router.include_router(collection_task_router)

router.include_router(analytics_router)
