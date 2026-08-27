from shared_models.task import AnalysisTaskRequest


def build_context(request: AnalysisTaskRequest) -> dict:
    return {
        "task_id": request.task_id,
        "question": request.question,
        "dataset_ids": request.dataset_ids,
    }

