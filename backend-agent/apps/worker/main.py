from graph_runtime.builder import build_graph


def main() -> None:
    graph = build_graph()
    print(f"worker bootstrapped graph={graph}")


if __name__ == "__main__":
    main()

