from guardrails.sql import validate_sql


def test_validate_sql_rejects_write_statement() -> None:
    assert not validate_sql("DELETE FROM sales")

