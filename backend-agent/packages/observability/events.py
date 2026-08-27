from pydantic import BaseModel


class StreamEvent(BaseModel):
    task_id: str
    trace_id: str
    event_type: str
    timestamp: str
    message: str | None = None

