import { TaskStream } from "../components/task-stream";
import { UploadPanel } from "../features/upload-panel";

export default function HomePage() {
  return (
    <main className="mx-auto flex min-h-screen max-w-7xl flex-col gap-8 px-6 py-12">
      <section className="max-w-3xl">
        <p className="text-sm uppercase tracking-[0.32em] text-teal-800">Dual Backend</p>
        <h1 className="mt-3 text-5xl font-semibold leading-tight text-stone-900">
          Data Analysis Agent Platform
        </h1>
        <p className="mt-4 max-w-2xl text-base leading-7 text-stone-700">
          Spring Boot owns auth, task entry, and SSE delivery. FastAPI plus LangGraph owns
          orchestration, tools, memory, and MCP-powered analysis.
        </p>
      </section>
      <section className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
        <UploadPanel />
        <TaskStream />
      </section>
    </main>
  );
}

