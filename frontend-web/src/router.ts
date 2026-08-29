import { createRouter, createWebHistory } from "vue-router";
import { getSession } from "./lib/auth";
import DatasetsPage from "./pages/DatasetsPage.vue";
import HistoryPage from "./pages/HistoryPage.vue";
import LoginPage from "./pages/LoginPage.vue";
import ProfilePage from "./pages/ProfilePage.vue";
import RegisterPage from "./pages/RegisterPage.vue";
import WorkbenchPage from "./pages/WorkbenchPage.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", component: LoginPage, meta: { guestOnly: true } },
    { path: "/register", component: RegisterPage, meta: { guestOnly: true } },
    { path: "/prototype/profile", component: ProfilePage },
    { path: "/", component: WorkbenchPage, meta: { requiresAuth: true } },
    { path: "/datasets", component: DatasetsPage, meta: { requiresAuth: true } },
    { path: "/history", component: HistoryPage, meta: { requiresAuth: true } },
    { path: "/profile", component: ProfilePage, meta: { requiresAuth: true } },
    { path: "/:pathMatch(.*)*", redirect: "/" },
  ],
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach((to) => {
  const session = getSession();

  if (to.meta.requiresAuth && !session) {
    return {
      path: "/login",
      query: {
        next: to.fullPath,
      },
    };
  }

  if (to.meta.guestOnly && session) {
    const next = typeof to.query.next === "string" ? to.query.next : "/";
    return next;
  }

  return true;
});

export { router };
