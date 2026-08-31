import { createApp } from "vue";
import App from "./App.vue";
import { initializeAuthRuntime } from "./lib/auth";
import { router } from "./router";
import "./styles.css";

async function bootstrap() {
  await initializeAuthRuntime();
  createApp(App).use(router).mount("#app");
}

void bootstrap();
