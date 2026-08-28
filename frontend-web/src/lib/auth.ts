import { reactive } from "vue";
import type { AuthApiResponse, AuthSession, AuthTokenPayload, LoginCredentials, RegisterUserForm } from "./types";

export const AUTH_STORAGE_KEY = "data-analysis-agent-session";

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

function loadStoredSession(): AuthSession | null {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as AuthSession;
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export const authSessionState = reactive<{
  session: AuthSession | null;
}>({
  session: loadStoredSession(),
});

const pendingRequests: PendingRequest[] = [];
let refreshLock: Promise<void> | null = null;

async function readApiResponse<T>(response: Response): Promise<AuthApiResponse<T> | null> {
  try {
    return (await response.json()) as AuthApiResponse<T>;
  } catch {
    return null;
  }
}

function persistSession(session: AuthSession) {
  if (typeof window !== "undefined") {
    window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
  }
  authSessionState.session = session;
}

function clearSession() {
  if (typeof window !== "undefined") {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
  }
  authSessionState.session = null;
}

function toSession(payload: AuthTokenPayload, previous?: Partial<AuthSession> | null): AuthSession {
  return {
    accessToken: payload.accessToken,
    refreshToken: payload.refreshToken,
    accessTokenExpiresIn: payload.accessTokenExpiresIn,
    refreshTokenExpiresIn: payload.refreshTokenExpiresIn,
    userId: payload.userId,
    username: payload.username,
    displayName: payload.nickname || payload.username,
    email: previous?.email ?? "",
    phone: previous?.phone ?? "",
    tenantId: payload.tenantId,
    roles: payload.roles,
    loginAt: previous?.loginAt ?? new Date().toISOString(),
  };
}

function mapRegisterField(message: string): RegisterFieldName | undefined {
  if (message.includes("用户名") || message.includes("鐢ㄦ埛鍚")) {
    return "username";
  }

  if (message.includes("邮箱") || message.includes("閭")) {
    return "email";
  }

  if (message.includes("手机号") || message.includes("鎵嬫満鍙")) {
    return "phone";
  }

  if (message.includes("密码") || message.includes("瀵嗙爜")) {
    return "password";
  }

  return undefined;
}

function buildHeaders(init?: RequestInit, accessToken?: string) {
  const headers = new Headers(init?.headers ?? {});
  if (!headers.has("Content-Type") && init?.body) {
    headers.set("Content-Type", "application/json");
  }
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }
  return headers;
}

async function performAuthorizedFetch(input: string, init: RequestInit = {}, accessToken?: string) {
  return fetch(input, {
    ...init,
    headers: buildHeaders(init, accessToken),
  });
}

async function refreshAccessToken() {
  const currentSession = getSession();
  if (!currentSession?.refreshToken) {
    throw new Error("missing refresh token");
  }

  const response = await fetch("/api/auth/refresh", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      refreshToken: currentSession.refreshToken,
    }),
  });

  const payload = await readApiResponse<AuthTokenPayload>(response);
  if (!response.ok || !payload?.success || !payload.data) {
    throw new Error(payload?.message || "refresh token is invalid or expired");
  }

  const session = toSession(payload.data, currentSession);
  persistSession(session);
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

async function ensureFreshAccessToken() {
  if (!refreshLock) {
    refreshLock = (async () => {
      try {
        await refreshAccessToken();
        resolveQueuedRequests();
      } catch (error) {
        clearSession();
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

export function getSession(): AuthSession | null {
  return authSessionState.session;
}

export async function authFetch(input: string, init: RequestInit = {}): Promise<Response> {
  const session = getSession();
  const response = await performAuthorizedFetch(input, init, session?.accessToken);

  if (response.status !== 401 || !session?.refreshToken || shouldBypassRefresh(input)) {
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

    ensureFreshAccessToken().catch(() => undefined);
  });
}

export async function loginWithAuthApi(credentials: LoginCredentials): Promise<LoginResult> {
  const response = await fetch("/api/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
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

  const session = toSession(payload.data);
  persistSession(session);
  return { success: true, session };
}

export async function registerWithAuthApi(payload: RegisterUserForm): Promise<RegisterResult> {
  const response = await fetch("/api/auth/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
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
    const session = toSession(result.data, {
      email: payload.email.trim(),
      phone: payload.phone.trim(),
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
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${session.accessToken}`,
        },
        body: JSON.stringify({
          refreshToken: session.refreshToken,
        }),
      });
    }
  } finally {
    clearSession();
  }
}
