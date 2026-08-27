from typing import TypedDict


class AgentState(TypedDict, total=False):
    task_id: str
    trace_id: str
    question: str
    dataset_ids: list[str]
    plan: list[str]
    sql: list[str]
    warnings: list[str]
    final_answer: str

