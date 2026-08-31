import { reactive } from "vue";
import type { AuthApiResponse, AuthSession, AuthTokenPayload, LoginCredentials, RegisterUserForm } from "./types";

const LEGACY_AUTH_STORAGE_KEYS = ["data-analysis-agent-session", "data-analysis-agent-users"] as const;
const LAST_ACTIVITY_STORAGE_KEY = "data-analysis-agent-last-activity-at";
const CLIENT_LAST_ACTIVITY_HEADER = "X-Client-Last-Activity-At";
const ACCESS_TOKEN_REFRESH_BUFFER_MS = 60 * 1000;
const PROACTIVE_REFRESH_ACTIVITY_WINDOW_MS = 15 * 60 * 1000;
const MAX_INACTIVITY_MS = 48 * 60 * 60 * 1000;
const ACTIVITY_STORAGE_WRITE_INTERVAL_MS = 30 * 1000;
const REFRESH_CHECK_INTERVAL_MS = 60 * 1000;
const ACTIVITY_REFRESH_DEBOUNCE_MS = 400;

export type RegisterFieldName = "username" | "email" | "phone" | "password";

export type RegisterResult =
  | {
      success: true;
      session: AuthSession;
    }
  | {
      success: false;
      message: string;
      field?: RegisterFieldName;
    };

export type LoginResult =
  | {
      success: true;
      session: AuthSession;
    }
  | {
      success: false;
      message: string;
    };

type PendingRequest = {
  resolve: (response: Response) => void;
  reject: (error: unknown) => void;
  retry: () => Promise<Response>;
};

type RefreshOptions = {
  allowWithoutSession?: boolean;
  redirectOnFail?: boolean;
};

const pendingRequests: PendingRequest[] = [];

let refreshLock: Promise<void> | null = null;
let runtimeInitialized = false;
let runtimeBootstrapPromise: Promise<void> | null = null;
let refreshIntervalId: number | null = null;
let activityRefreshTimeoutId: number | null = null;
let lastActivityAtMs: number | null = null;
let lastActivityPersistedAtMs: number | null = null;

function clearLegacyAuthStorage() {
  if (typeof window === "undefined") {
    return;
  }

  [window.localStorage, window.sessionStorage].forEach((storage) => {
    LEGACY_AUTH_STORAGE_KEYS.forEach((key) => {
      storage.removeItem(key);
    });
  });
}

function loadStoredLastActivityAt() {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.localStorage.getItem(LAST_ACTIVITY_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  const timestamp = Date.parse(raw);
  if (Number.isNaN(timestamp)) {
    window.localStorage.removeItem(LAST_ACTIVITY_STORAGE_KEY);
    return null;
  }

  return timestamp;
}

function initializeSession(): AuthSession | null {
  clearLegacyAuthStorage();
  lastActivityAtMs = loadStoredLastActivityAt();
  lastActivityPersistedAtMs = lastActivityAtMs;
  return null;
}

export const authSessionState = reactive<{
  session: AuthSession | null;
}>({
  session: initializeSession(),
});

function setStoredLastActivityAt(timestampMs: number, forcePersist = false) {
  lastActivityAtMs = timestampMs;

  if (typeof window === "undefined") {
    return;
  }

  const shouldPersist =
    forcePersist ||
    lastActivityPersistedAtMs == null ||
    timestampMs - lastActivityPersistedAtMs >= ACTIVITY_STORAGE_WRITE_INTERVAL_MS;

  if (!shouldPersist) {
    return;
  }

  window.localStorage.setItem(LAST_ACTIVITY_STORAGE_KEY, new Date(timestampMs).toISOString());
  lastActivityPersistedAtMs = timestampMs;
}

function clearStoredLastActivity() {
  lastActivityAtMs = null;
  lastActivityPersistedAtMs = null;

  if (typeof window !== "undefined") {
    window.localStorage.removeItem(LAST_ACTIVITY_STORAGE_KEY);
  }
}

function getLastActivityAtMs() {
  if (lastActivityAtMs != null) {
    return lastActivityAtMs;
  }

  lastActivityAtMs = loadStoredLastActivityAt();
  lastActivityPersistedAtMs = lastActivityAtMs;
  return lastActivityAtMs;
}

function getLastActivityAtIso() {
  const timestamp = getLastActivityAtMs();
  return timestamp == null ? null : new Date(timestamp).toISOString();
}

function hasRecentActivity(windowMs: number, now = Date.now()) {
  const lastActivity = getLastActivityAtMs();
  if (lastActivity == null) {
    return false;
  }

  return now - lastActivity <= windowMs;
}

function hasExceededInactivityLimit(now = Date.now()) {
  const lastActivity = getLastActivityAtMs();
  if (lastActivity == null) {
    return true;
  }

  return now - lastActivity > MAX_INACTIVITY_MS;
}

function markUserActivity(forcePersist = false) {
  setStoredLastActivityAt(Date.now(), forcePersist);
}

function persistSession(session: AuthSession) {
  clearLegacyAuthStorage();
  authSessionState.session = session;
}

function clearSession(options?: { clearActivity?: boolean; redirect?: boolean }) {
  clearLegacyAuthStorage();
  authSessionState.session = null;

  if (options?.clearActivity) {
    clearStoredLastActivity();
  }

  if (options?.redirect) {
    redirectToLogin();
  }
}

function toSession(payload: AuthTokenPayload, previous?: Partial<AuthSession> | null): AuthSession {
  return {
    accessToken: payload.accessToken,
    accessTokenExpiresIn: payload.accessTokenExpiresIn,
    refreshTokenExpiresIn: payload.refreshTokenExpiresIn,
    userId: payload.userId,
    username: payload.username,
    nickname: payload.nickname || payload.username,
    displayName: payload.nickname || payload.username,
    avatarUrl: payload.avatarUrl,
    status: payload.status || "ACTIVE",
    tenantId: payload.tenantId,
    roles: payload.roles,
    email: previous?.email ?? "",
    phone: previous?.phone ?? "",
    gender: previous?.gender ?? null,
    tokenIssuedAt: new Date().toISOString(),
    loginAt: previous?.loginAt ?? new Date().toISOString(),
  };
}

export function updateSessionProfile(profile: Partial<Pick<AuthSession, "nickname" | "displayName" | "avatarUrl" | "status" | "email" | "phone" | "gender">>) {
  const session = authSessionState.session;
  if (!session) {
    return;
  }

  authSessionState.session = {
    ...session,
    ...profile,
    nickname: profile.nickname ?? session.nickname,
    displayName: profile.displayName ?? profile.nickname ?? session.displayName,
    avatarUrl: profile.avatarUrl ?? session.avatarUrl,
    status: profile.status ?? session.status,
    email: profile.email ?? session.email,
    phone: profile.phone ?? session.phone,
    gender: profile.gender ?? session.gender,
  };
}

function mapRegisterField(message: string): RegisterFieldName | undefined {
  if (message.includes("用户名") || message.includes("账号")) {
    return "username";
  }

  if (message.includes("邮箱") || message.includes("邮件")) {
    return "email";
  }

  if (message.includes("手机号") || message.includes("手机")) {
    return "phone";
  }

  if (message.includes("密码") || message.includes("口令")) {
    return "password";
  }

  return undefined;
}

function buildHeaders(init?: RequestInit, accessToken?: string) {
  const headers = new Headers(init?.headers ?? {});
  // FormData 由浏览器自动生成带 boundary 的 multipart Content-Type，不能手动覆盖
  const isFormData = typeof FormData !== "undefined" && init?.body instanceof FormData;
  if (!headers.has("Content-Type") && init?.body && !isFormData) {
    headers.set("Content-Type", "application/json");
  }
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  const lastActivityAt = getLastActivityAtIso();
  if (lastActivityAt) {
    headers.set(CLIENT_LAST_ACTIVITY_HEADER, lastActivityAt);
  }

  return headers;
}

async function readApiResponse<T>(response: Response): Promise<AuthApiResponse<T> | null> {
  try {
    return (await response.json()) as AuthApiResponse<T>;
  } catch {
    return null;
  }
}

function getAccessTokenExpiresAt(session: AuthSession) {
  const issuedAt = Date.parse(session.tokenIssuedAt);
  if (Number.isNaN(issuedAt)) {
    return 0;
  }

  return issuedAt + session.accessTokenExpiresIn * 1000;
}

function isAccessTokenExpiringSoon(session: AuthSession, now = Date.now(), bufferMs = ACCESS_TOKEN_REFRESH_BUFFER_MS) {
  return getAccessTokenExpiresAt(session) - now <= bufferMs;
}

function isPublicRoute(pathname = window.location.pathname) {
  return pathname === "/login" || pathname === "/register";
}

function redirectToLogin() {
  if (typeof window === "undefined" || isPublicRoute()) {
    return;
  }

  const next = `${window.location.pathname}${window.location.search}${window.location.hash}`;
  const query = next && next !== "/" ? `?next=${encodeURIComponent(next)}` : "";
  window.location.replace(`/login${query}`);
}

async function performAuthorizedFetch(input: string, init: RequestInit = {}, accessToken?: string) {
  return fetch(input, {
    ...init,
    credentials: init.credentials ?? "include",
    headers: buildHeaders(init, accessToken),
  });
}

function resolveQueuedRequests() {
  const queued = pendingRequests.splice(0, pendingRequests.length);
  queued.forEach(({ resolve, reject, retry }) => {
    retry().then(resolve).catch(reject);
  });
}

function rejectQueuedRequests(error: unknown) {
  const queued = pendingRequests.splice(0, pendingRequests.length);
  queued.forEach(({ reject }) => reject(error));
}

async function refreshAccessToken(options: RefreshOptions = {}) {
  if (!options.allowWithoutSession && !getSession()) {
    throw new Error("missing access session");
  }

  if (hasExceededInactivityLimit()) {
    clearSession({ clearActivity: true, redirect: options.redirectOnFail });
    throw new Error("session inactive for too long");
  }

  const response = await fetch("/api/auth/refresh", {
    method: "POST",
    credentials: "include",
    headers: buildHeaders({
      headers: {
        "Content-Type": "application/json",
      },
    }),
  }).catch(() => null);

  if (!response) {
    throw new Error("refresh service unavailable");
  }

  const payload = await readApiResponse<AuthTokenPayload>(response);
  if (!response.ok || !payload?.success || !payload.data) {
    clearSession({ clearActivity: true, redirect: options.redirectOnFail });
    throw new Error(payload?.message || "refresh token is invalid or expired");
  }

  const session = toSession(payload.data, getSession());
  persistSession(session);
}

async function ensureFreshAccessToken(options: RefreshOptions = {}) {
  if (!refreshLock) {
    refreshLock = (async () => {
      try {
        await refreshAccessToken(options);
        resolveQueuedRequests();
      } catch (error) {
        rejectQueuedRequests(error);
        throw error;
      } finally {
        refreshLock = null;
      }
    })();
  }

  return refreshLock;
}

function shouldBypassRefresh(url: string) {
  return url.startsWith("/api/auth/login") || url.startsWith("/api/auth/register") || url.startsWith("/api/auth/refresh");
}

async function maybeRefreshAfterUserActivity() {
  if (hasExceededInactivityLimit()) {
    clearSession({ clearActivity: true, redirect: true });
    return;
  }

  const session = getSession();
  if (!session) {
    try {
      await ensureFreshAccessToken({ allowWithoutSession: true, redirectOnFail: true });
    } catch {
      return;
    }
    return;
  }

  if (!isAccessTokenExpiringSoon(session, Date.now(), ACCESS_TOKEN_REFRESH_BUFFER_MS * 2)) {
    return;
  }

  try {
    await ensureFreshAccessToken({ redirectOnFail: true });
  } catch {
    return;
  }
}

function scheduleActivityDrivenRefresh() {
  if (typeof window === "undefined" || activityRefreshTimeoutId != null) {
    return;
  }

  activityRefreshTimeoutId = window.setTimeout(() => {
    activityRefreshTimeoutId = null;
    void maybeRefreshAfterUserActivity();
  }, ACTIVITY_REFRESH_DEBOUNCE_MS);
}

function handleUserActivityEvent() {
  markUserActivity();
  scheduleActivityDrivenRefresh();
}

function installActivityListeners() {
  if (typeof window === "undefined") {
    return;
  }

  const passive = { passive: true } as const;
  window.addEventListener("pointerdown", handleUserActivityEvent, passive);
  window.addEventListener("keydown", handleUserActivityEvent);
  window.addEventListener("scroll", handleUserActivityEvent, passive);
  window.addEventListener("mousemove", handleUserActivityEvent, passive);
  window.addEventListener("focus", handleUserActivityEvent);
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") {
      handleUserActivityEvent();
    }
  });
}

function startRefreshMonitor() {
  if (typeof window === "undefined" || refreshIntervalId != null) {
    return;
  }

  refreshIntervalId = window.setInterval(() => {
    void (async () => {
      if (hasExceededInactivityLimit()) {
        clearSession({ clearActivity: true, redirect: true });
        return;
      }

      const session = getSession();
      if (!session) {
        return;
      }

      if (!hasRecentActivity(PROACTIVE_REFRESH_ACTIVITY_WINDOW_MS)) {
        return;
      }

      if (!isAccessTokenExpiringSoon(session)) {
        return;
      }

      try {
        await ensureFreshAccessToken({ redirectOnFail: true });
      } catch {
        return;
      }
    })();
  }, REFRESH_CHECK_INTERVAL_MS);
}

export async function initializeAuthRuntime() {
  if (runtimeInitialized) {
    return runtimeBootstrapPromise ?? Promise.resolve();
  }

  runtimeInitialized = true;
  clearLegacyAuthStorage();
  installActivityListeners();
  startRefreshMonitor();

  runtimeBootstrapPromise = (async () => {
    if (hasExceededInactivityLimit()) {
      clearSession({ clearActivity: true });
      return;
    }

    if (!hasRecentActivity(MAX_INACTIVITY_MS)) {
      return;
    }

    try {
      await ensureFreshAccessToken({ allowWithoutSession: true });
    } catch {
      clearSession({ clearActivity: false });
    }
  })();

  return runtimeBootstrapPromise;
}

export function getSession(): AuthSession | null {
  return authSessionState.session;
}

export async function authFetch(input: string, init: RequestInit = {}): Promise<Response> {
  const session = getSession();
  const response = await performAuthorizedFetch(input, init, session?.accessToken);

  if (response.status !== 401 || shouldBypassRefresh(input)) {
    return response;
  }

  if (hasExceededInactivityLimit()) {
    clearSession({ clearActivity: true, redirect: true });
    return response;
  }

  return new Promise<Response>((resolve, reject) => {
    pendingRequests.push({
      resolve,
      reject,
      retry: async () => {
        const latestSession = getSession();
        if (!latestSession?.accessToken) {
          throw new Error("missing refreshed access token");
        }
        return performAuthorizedFetch(input, init, latestSession.accessToken);
      },
    });

    ensureFreshAccessToken({
      allowWithoutSession: true,
      redirectOnFail: true,
    }).catch(() => undefined);
  });
}

export async function loginWithAuthApi(credentials: LoginCredentials): Promise<LoginResult> {
  const response = await fetch("/api/auth/login", {
    method: "POST",
    credentials: "include",
    headers: buildHeaders({
      headers: {
        "Content-Type": "application/json",
      },
    }),
    body: JSON.stringify({
      username: credentials.identifier.trim(),
      password: credentials.password,
    }),
  }).catch(() => null);

  if (!response) {
    return { success: false, message: "登录服务暂不可用，请稍后再试。" };
  }

  const payload = await readApiResponse<AuthTokenPayload>(response);
  if (!response.ok || !payload?.success || !payload.data) {
    return {
      success: false,
      message: payload?.message || "账号或密码错误。",
    };
  }

  markUserActivity(true);
  const session = toSession(payload.data);
  persistSession(session);
  return { success: true, session };
}

export async function registerWithAuthApi(payload: RegisterUserForm): Promise<RegisterResult> {
  const response = await fetch("/api/auth/register", {
    method: "POST",
    credentials: "include",
    headers: buildHeaders({
      headers: {
        "Content-Type": "application/json",
      },
    }),
    body: JSON.stringify({
      username: payload.username.trim(),
      password: payload.password,
      nickname: payload.displayName.trim() || payload.username.trim(),
      email: payload.email.trim(),
      phone: payload.phone.trim(),
    }),
  }).catch(() => null);

  if (!response) {
    return {
      success: false,
      message: "注册服务暂不可用，请稍后再试。",
    };
  }

  const result = await readApiResponse<AuthTokenPayload>(response);
  if (response.ok && result?.success && result.data) {
    markUserActivity(true);
    const session = toSession(result.data, {
      loginAt: new Date().toISOString(),
    });
    persistSession(session);
    return { success: true, session };
  }

  const message = result?.message || "注册失败，请稍后再试。";
  return {
    success: false,
    message,
    field: mapRegisterField(message),
  };
}

export async function logoutSession() {
  const session = getSession();

  try {
    if (session?.accessToken) {
      await fetch("/api/auth/logout", {
        method: "POST",
        credentials: "include",
        headers: buildHeaders({
          headers: {
            "Content-Type": "application/json",
          },
        }, session.accessToken),
      });
    }
  } finally {
    clearSession({ clearActivity: true });
  }
}
