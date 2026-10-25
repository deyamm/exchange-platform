import logging

from app.client.spring_client import SpringClient
from app.core.registry import registry
from app.schemas.collection_task.task_template import TaskRunResult

logger = logging.getLogger(__name__)

class TaskRunService:

    def __init__(self, spring_client: SpringClient):

        self.spring_client = spring_client

    async def task_run_executor(self, req):
        try:
            result = await registry.execute(req)
        except Exception as exc:
            logger.exception("Background execution failed for runId=%s", req.run_id)

            result = TaskRunResult(
                run_id=req.run_id,
                status="FAILED",
                total_count=0,
                success_count=0,
                failure_count=1,
                result_location=None,
                summary_content={"error": str(exc)},
                logs=[],
                problems=[],
                exceptions=[
                    {
                        "message": str(exc),
                        "type": exc.__class__.__name__,
                    }
                ],
            )

        await self.spring_client.post_task_run_result(result.model_dump(by_alias=True))
