from pydantic import BaseModel


class MemoryEntry(BaseModel):
    key: str
    value: str
    scope: str

