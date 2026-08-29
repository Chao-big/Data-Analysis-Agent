import { authFetch } from "./auth";
import type { AuthApiResponse, ProfilePayload, ProfileUpdatePayload } from "./types";

async function readApiResponse<T>(response: Response): Promise<AuthApiResponse<T> | null> {
  try {
    return (await response.json()) as AuthApiResponse<T>;
  } catch {
    return null;
  }
}

export async function fetchCurrentProfile(): Promise<ProfilePayload> {
  const response = await authFetch("/api/profile/me", {
    method: "GET",
  });

  const payload = await readApiResponse<ProfilePayload>(response);
  if (!response.ok || !payload?.success || !payload.data) {
    throw new Error(payload?.message || "获取个人资料失败");
  }

  return payload.data;
}

export async function updateCurrentProfile(request: ProfileUpdatePayload): Promise<ProfilePayload> {
  const response = await authFetch("/api/profile/me", {
    method: "PUT",
    body: JSON.stringify(request),
  });

  const payload = await readApiResponse<ProfilePayload>(response);
  if (!response.ok || !payload?.success || !payload.data) {
    throw new Error(payload?.message || "保存个人资料失败");
  }

  return payload.data;
}
