<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import AppShell from "../components/AppShell.vue";
import { updateSessionProfile } from "../lib/auth";
import { fetchCurrentProfile, updateCurrentProfile } from "../lib/profile";
import { workspaceStore } from "../lib/workspace-store";
import type { GenderCode, ProfilePayload, ProfileUpdatePayload } from "../lib/types";

type ProfileForm = {
  nickname: string;
  email: string;
  phone: string;
  gender: GenderCode;
};

const GENDER_UNKNOWN: GenderCode = 0;

const currentUser = computed(() => workspaceStore.currentUser);
const isEditing = ref(false);
const isLoading = ref(true);
const isSaving = ref(false);
const passwordVisible = ref(false);
const avatarInput = ref<HTMLInputElement | null>(null);
const avatarDraft = ref("");
const actionMessage = ref("");
const loadError = ref("");
const remoteProfile = ref<ProfilePayload | null>(null);

const profileForm = reactive<ProfileForm>({
  nickname: "",
  email: "",
  phone: "",
  gender: GENDER_UNKNOWN,
});

function normalizeGender(gender: GenderCode | null | undefined): GenderCode {
  return gender === 1 || gender === 2 ? gender : GENDER_UNKNOWN;
}

function genderLabel(gender: GenderCode | null | undefined) {
  if (gender === 1) {
    return "男";
  }

  if (gender === 2) {
    return "女";
  }

  return "未知";
}

function normalizeText(value: string | null | undefined) {
  if (value == null) {
    return "";
  }

  return value.trim();
}

function isHttpUrl(value: string) {
  return value.startsWith("http://") || value.startsWith("https://");
}

const profileView = computed<ProfilePayload>(() => {
  if (remoteProfile.value) {
    return remoteProfile.value;
  }

  return {
    userId: String(currentUser.value.userId ?? ""),
    username: String(currentUser.value.username ?? ""),
    nickname: String(currentUser.value.nickname || currentUser.value.displayName || ""),
    avatarUrl: currentUser.value.avatarUrl ?? null,
    email: String(currentUser.value.email ?? ""),
    phone: String(currentUser.value.phone ?? ""),
    gender: normalizeGender(currentUser.value.gender),
    status: String(currentUser.value.status ?? "ACTIVE"),
    lastLoginAt: currentUser.value.lastLoginAt || null,
    lastLoginIp: currentUser.value.lastLoginIp ?? null,
    createdAt: currentUser.value.createdAt || null,
    updatedAt: currentUser.value.updatedAt || null,
  };
});

watch(
  profileView,
  (profile) => {
    if (isEditing.value) {
      return;
    }

    profileForm.nickname = profile.nickname || "";
    profileForm.email = profile.email || "";
    profileForm.phone = profile.phone || "";
    profileForm.gender = normalizeGender(profile.gender);
  },
  { immediate: true },
);

const avatarSource = computed(() => avatarDraft.value || profileView.value.avatarUrl || "");
const avatarText = computed(() => {
  const seed = profileForm.nickname || profileView.value.nickname || profileView.value.username || "U";
  return seed.slice(0, 1).toUpperCase();
});

const profileItems = computed(() => [
  { key: "nickname" as const, label: "昵称", value: profileForm.nickname || "未设置" },
  { key: "email" as const, label: "邮箱", value: profileForm.email || "未设置" },
  { key: "phone" as const, label: "手机号", value: profileForm.phone || "未设置" },
  { key: "gender" as const, label: "性别", value: genderLabel(profileForm.gender) },
]);

function applyProfile(profile: ProfilePayload) {
  const normalizedProfile: ProfilePayload = {
    ...profile,
    gender: normalizeGender(profile.gender),
  };

  remoteProfile.value = normalizedProfile;
  profileForm.nickname = normalizedProfile.nickname || "";
  profileForm.email = normalizedProfile.email || "";
  profileForm.phone = normalizedProfile.phone || "";
  profileForm.gender = normalizeGender(normalizedProfile.gender);

  updateSessionProfile({
    nickname: normalizedProfile.nickname,
    displayName: normalizedProfile.nickname,
    avatarUrl: normalizedProfile.avatarUrl,
    status: normalizedProfile.status,
    email: normalizedProfile.email,
    phone: normalizedProfile.phone,
    gender: normalizedProfile.gender,
  });
}

async function loadProfile() {
  isLoading.value = true;
  loadError.value = "";

  try {
    applyProfile(await fetchCurrentProfile());
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : "获取个人资料失败";
  } finally {
    isLoading.value = false;
  }
}

function startEditing() {
  isEditing.value = true;
  actionMessage.value = "";
}

function cancelEditing() {
  avatarDraft.value = "";
  actionMessage.value = "";
  isEditing.value = false;
  profileForm.nickname = profileView.value.nickname || "";
  profileForm.email = profileView.value.email || "";
  profileForm.phone = profileView.value.phone || "";
  profileForm.gender = normalizeGender(profileView.value.gender);
}

function buildUpdatePayload(): ProfileUpdatePayload {
  const originalNickname = normalizeText(profileView.value.nickname);
  const originalEmail = normalizeText(profileView.value.email);
  const originalPhone = normalizeText(profileView.value.phone);
  const originalGender = normalizeGender(profileView.value.gender);
  const nextNickname = normalizeText(profileForm.nickname);
  const nextEmail = normalizeText(profileForm.email);
  const nextPhone = normalizeText(profileForm.phone);
  const nextGender = normalizeGender(profileForm.gender);
  const nextAvatarUrl = normalizeText(avatarDraft.value);
  const originalAvatarUrl = normalizeText(profileView.value.avatarUrl);

  return {
    nickname: nextNickname === originalNickname ? null : nextNickname,
    email: nextEmail === originalEmail ? null : nextEmail,
    phone: nextPhone === originalPhone ? null : nextPhone,
    gender: nextGender === originalGender ? null : nextGender,
    avatarUrl: isHttpUrl(nextAvatarUrl) && nextAvatarUrl !== originalAvatarUrl ? nextAvatarUrl : null,
  };
}

async function saveProfile() {
  const payload = buildUpdatePayload();
  const hasChanges = Object.values(payload).some((value) => value !== null);
  isSaving.value = true;
  actionMessage.value = "";

  try {
    const profile = await updateCurrentProfile(payload);
    applyProfile({
      ...profile,
      avatarUrl: payload.avatarUrl ?? profile.avatarUrl,
    });
    isEditing.value = false;
    actionMessage.value = !hasChanges
      ? "未检测到资料变更，后端已跳过更新。"
      : avatarDraft.value && payload.avatarUrl == null
      ? "资料已保存。当前头像仅做本地预览，图片上传接口尚未接入。"
      : "资料已保存。";
    avatarDraft.value = "";
  } catch (error) {
    actionMessage.value = error instanceof Error ? error.message : "保存个人资料失败";
  } finally {
    isSaving.value = false;
  }
}

function togglePasswordVisible() {
  passwordVisible.value = !passwordVisible.value;
}

function openChangePassword() {
  actionMessage.value = "修改密码接口尚未接入，这一版先完成资料差异更新。";
}

function openAvatarPicker() {
  if (!isEditing.value) {
    return;
  }

  avatarInput.value?.click();
}

function handleAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }

  const reader = new FileReader();
  reader.onload = () => {
    avatarDraft.value = typeof reader.result === "string" ? reader.result : "";
    actionMessage.value = "头像已更新为本地预览。当前后端仅保存头像地址，图片上传接口尚未接入。";
  };
  reader.readAsDataURL(file);
}

onMounted(() => {
  void loadProfile();
});
</script>

<template>
  <AppShell>
    <div class="h-full w-full pb-6">
      <div class="flex h-full flex-col rounded-[28px] border border-slate-200 bg-white/90 p-6 shadow-[0_20px_60px_rgba(15,23,42,0.06)]">
        <div class="flex-1 rounded-[28px] border border-[#dbe8ee] bg-[linear-gradient(180deg,rgba(255,255,255,0.98),rgba(245,249,251,0.98))]">
          <div class="grid h-full gap-0 lg:grid-cols-[280px_minmax(0,1fr)]">
            <aside class="flex flex-col border-b border-[#e4edf2] bg-[linear-gradient(180deg,#f8fcfd_0%,#f2f8fa_100%)] px-6 py-7 lg:border-b-0 lg:border-r">
              <div class="flex flex-1 flex-col items-center justify-start pt-8">
                <button
                  type="button"
                  :disabled="!isEditing"
                  :class="[
                    'group relative flex h-[88px] w-[88px] items-center justify-center overflow-hidden rounded-full border-4 border-white bg-white shadow-[0_10px_24px_rgba(15,23,42,0.08)]',
                    isEditing ? 'cursor-pointer' : 'cursor-default',
                  ]"
                  @click="openAvatarPicker"
                >
                  <img
                    v-if="avatarSource"
                    :src="avatarSource"
                    alt="用户头像"
                    class="h-full w-full rounded-full object-cover"
                  />
                  <div
                    v-else
                    class="flex h-full w-full items-center justify-center rounded-full bg-[linear-gradient(135deg,#ffffff,#edf8f7)] text-2xl font-semibold text-[#0f8b8d]"
                  >
                    {{ avatarText }}
                  </div>
                  <div
                    :class="[
                      'absolute inset-0 flex items-center justify-center rounded-full bg-[#102038]/58 text-[11px] font-semibold text-white transition',
                      isEditing ? 'opacity-0 group-hover:opacity-100' : 'opacity-0',
                    ]"
                  >
                    更换头像
                  </div>
                </button>
                <input ref="avatarInput" type="file" accept="image/*" class="hidden" @change="handleAvatarChange" />

                <div class="mt-5 text-center">
                  <div class="text-[1.375rem] font-semibold leading-none text-[#102038]">
                    {{ profileForm.nickname || profileView.nickname || profileView.username }}
                  </div>
                </div>
              </div>
            </aside>

            <section class="px-6 py-7">
              <div class="flex flex-wrap items-center justify-between gap-3 border-b border-[#e4edf2] pb-4">
                <div>
                  <div class="text-lg font-semibold text-[#102038]">基础信息</div>
                </div>

                <div class="flex items-center gap-3">
                  <button
                    v-if="!isEditing"
                    type="button"
                    class="h-10 rounded-[12px] border border-[#cfdde5] bg-white px-4 text-sm font-semibold text-[#42586d] transition hover:border-[#b8cbd7] hover:bg-[#f9fbfc] disabled:cursor-not-allowed disabled:opacity-60"
                    :disabled="isLoading"
                    @click="startEditing"
                  >
                    编辑资料
                  </button>
                  <template v-else>
                    <button
                      type="button"
                      class="h-10 rounded-[12px] bg-[#152239] px-4 text-sm font-semibold text-white transition hover:bg-[#0f1a2d] disabled:cursor-not-allowed disabled:opacity-60"
                      :disabled="isSaving"
                      @click="saveProfile"
                    >
                      {{ isSaving ? "保存中..." : "保存" }}
                    </button>
                    <button
                      type="button"
                      class="h-10 rounded-[12px] border border-[#cfdde5] bg-white px-4 text-sm font-semibold text-[#42586d] transition hover:border-[#b8cbd7] hover:bg-[#f9fbfc]"
                      @click="cancelEditing"
                    >
                      取消
                    </button>
                  </template>
                </div>
              </div>

              <div
                v-if="loadError"
                class="mt-5 rounded-[14px] border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700"
              >
                {{ loadError }}
              </div>

              <div
                v-else-if="isLoading"
                class="mt-5 rounded-[14px] border border-[#d9e8ed] bg-[#f7fbfc] px-4 py-3 text-sm text-[#567085]"
              >
                正在加载个人资料...
              </div>

              <template v-else>
                <div class="mt-6 grid gap-4 md:grid-cols-2">
                  <div
                    v-for="item in profileItems"
                    :key="item.key"
                    class="rounded-[18px] border border-[#dbe8ee] bg-white px-4 py-4 shadow-[0_8px_20px_rgba(15,23,42,0.03)]"
                  >
                    <div class="text-[12px] font-semibold text-[#7a8ea1]">{{ item.label }}</div>

                    <div v-if="!isEditing" class="mt-3 min-h-[28px] break-all text-[15px] leading-7 text-[#102038]">
                      {{ item.value }}
                    </div>

                    <div v-else class="mt-3">
                      <select
                        v-if="item.key === 'gender'"
                        v-model="profileForm.gender"
                        class="h-11 w-full rounded-[12px] border border-[#d6e4eb] bg-[#fbfdfe] px-4 text-sm text-[#102038] outline-none transition focus:border-[#67c3b8] focus:bg-white focus:ring-4 focus:ring-[#e4f7f4]"
                      >
                        <option :value="0">未知</option>
                        <option :value="1">男</option>
                        <option :value="2">女</option>
                      </select>
                      <input
                        v-else
                        v-model.trim="profileForm[item.key]"
                        type="text"
                        class="h-11 w-full rounded-[12px] border border-[#d6e4eb] bg-[#fbfdfe] px-4 text-sm text-[#102038] outline-none transition focus:border-[#67c3b8] focus:bg-white focus:ring-4 focus:ring-[#e4f7f4]"
                        :placeholder="`请输入${item.label}`"
                      />
                    </div>
                  </div>
                </div>

                <div class="mt-6 border-t border-[#e4edf2] pt-6">
                  <div class="mb-4 text-lg font-semibold text-[#102038]">密码设置</div>
                  <div class="grid gap-4 md:grid-cols-2">
                    <div class="rounded-[18px] border border-[#dbe8ee] bg-white px-4 py-4 shadow-[0_8px_20px_rgba(15,23,42,0.03)]">
                      <div class="text-[12px] font-semibold text-[#7a8ea1]">当前密码</div>
                      <div class="mt-3 text-[15px] leading-7 text-[#102038]">
                        {{ passwordVisible ? "Password@123" : "••••••••••" }}
                      </div>
                      <button
                        type="button"
                        class="mt-4 h-10 rounded-[12px] border border-[#cfdde5] bg-white px-4 text-sm font-semibold text-[#42586d] transition hover:border-[#b8cbd7] hover:bg-[#f9fbfc]"
                        @click="togglePasswordVisible"
                      >
                        {{ passwordVisible ? "隐藏当前密码" : "查看当前密码" }}
                      </button>
                    </div>

                    <div class="rounded-[18px] border border-[#dbe8ee] bg-white px-4 py-4 shadow-[0_8px_20px_rgba(15,23,42,0.03)]">
                      <div class="text-[12px] font-semibold text-[#7a8ea1]">密码管理</div>
                      <div class="mt-3 text-[15px] leading-7 text-[#102038]">已设置登录密码，可在这里发起修改。</div>
                      <button
                        type="button"
                        class="mt-4 h-10 rounded-[12px] bg-[#152239] px-4 text-sm font-semibold text-white transition hover:bg-[#0f1a2d]"
                        @click="openChangePassword"
                      >
                        修改密码
                      </button>
                    </div>
                  </div>
                </div>
              </template>

              <div
                v-if="actionMessage"
                class="mt-5 rounded-[14px] border border-[#d9e8ed] bg-[#f7fbfc] px-4 py-3 text-sm text-[#567085]"
              >
                {{ actionMessage }}
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  </AppShell>
</template>
