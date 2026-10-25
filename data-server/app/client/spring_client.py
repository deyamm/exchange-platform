import logging
from typing import Any
import httpx

from app.core.registry import registry
from app.core.config import settings
from app.schemas.collection_task.task_template import TaskRunRequest, TaskRunResult

logger = logging.getLogger(__name__)

class SpringClient:

    def __init__(self):
        self.base_url = settings.SPRING_CALLBACK_BASE_URL.rstrip("/")
        self.timeout = 30


    async def post_task_run_result(self, payload: dict[str, Any]) -> None:
        url = self.base_url + "/api/v1/collection/task-runs/callback"

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            resp = await client.post(url, json=payload)
            logger.info("Posted task run result to %s, status=%s", url, resp.status_code)
        
    