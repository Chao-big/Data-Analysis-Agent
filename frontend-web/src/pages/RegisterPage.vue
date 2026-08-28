<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import AuthShell from "../components/AuthShell.vue";
import { loginWithAuthApi, registerWithAuthApi, type RegisterFieldName } from "../lib/auth";
import {
  validateRegisterConfirmPassword,
  validateRegisterEmail,
  validateRegisterPassword,
  validateRegisterPhone,
  validateRegisterUsername,
} from "../lib/register-validation";

type RegisterField = RegisterFieldName | "confirmPassword";

const route = useRoute();
const router = useRouter();
const nextPath = typeof route.query.next === "string" ? route.query.next : "/";

const form = reactive({
  username: "",
  email: "",
  phone: "",
  password: "",
  confirmPassword: "",
});

const touched = reactive<Record<RegisterField, boolean>>({
  username: false,
  email: false,
  phone: false,
  password: false,
  confirmPassword: false,
});

const fieldErrors = reactive<Record<RegisterField, string | null>>({
  username: null,
  email: null,
  phone: null,
  password: null,
  confirmPassword: null,
});

const submitting = ref(false);
const showPassword = ref(false);
const showConfirmPassword = ref(false);

function validateField(field: RegisterField) {
  switch (field) {
    case "username":
      fieldErrors.username = validateRegisterUsername(form.username);
      break;
    case "email":
      fieldErrors.email = validateRegisterEmail(form.email);
      break;
    case "phone":
      fieldErrors.phone = validateRegisterPhone(form.phone);
      break;
    case "password":
      fieldErrors.password = validateRegisterPassword(form.password);
      if (touched.confirmPassword || form.confirmPassword) {
        fieldErrors.confirmPassword = validateRegisterConfirmPassword(form.password, form.confirmPassword);
      }
      break;
    case "confirmPassword":
      fieldErrors.confirmPassword = validateRegisterConfirmPassword(form.password, form.confirmPassword);
      break;
  }
}

function handleBlur(field: RegisterField) {
  touched[field] = true;
  validateField(field);
}

function handleInput(field: RegisterField) {
  if (touched[field] || fieldErrors[field]) {
    validateField(field);
  }
}

function validateForm() {
  (Object.keys(touched) as RegisterField[]).forEach((field) => {
    touched[field] = true;
    validateField(field);
  });

  return !Object.values(fieldErrors).some(Boolean);
}

function setServerFieldError(field: RegisterField, message: string) {
  touched[field] = true;
  fieldErrors[field] = message;
}

async function handleSubmit() {
  if (!validateForm()) {
    return;
  }

  submitting.value = true;
  await new Promise((resolve) => window.setTimeout(resolve, 450));

  const registerResult = await registerWithAuthApi({
    displayName: form.username,
    username: form.username,
    email: form.email,
    phone: form.phone,
    password: form.password,
  });

  if (!registerResult.success) {
    setServerFieldError(registerResult.field ?? "password", registerResult.message);
    submitting.value = false;
    return;
  }

  router.replace(nextPath || "/");
  return;

  const loginResult = await loginWithAuthApi({
    identifier: form.username,
    password: form.password,
  });

  if (!loginResult.success) {
    setServerFieldError("password", "注册成功，但自动登录失败，请直接登录。");
    submitting.value = false;
    return;
  }

  router.replace(nextPath || "/");
}
</script>

<template>
  <AuthShell
    eyebrow="sign up"
    title="创建你的分析账号"
    alternate-label="已经有账号？"
    alternate-action-text="去登录"
    :alternate-action-to="{ path: '/login', query: nextPath ? { next: nextPath } : {} }"
  >
    <div class="space-y-6">
      <div>
        <div class="text-[11px] font-semibold uppercase tracking-[0.26em] text-[#2f5fe8]">create account</div>
        <h2 class="display-face mt-3 text-[2rem] font-semibold tracking-[-0.05em] text-[#122033]">注册新账号</h2>
      </div>

      <form class="space-y-3" @submit.prevent="handleSubmit">
        <label class="block">
          <span class="text-sm font-medium text-[#31455f]">用户名</span>
          <input
            v-model="form.username"
            autocomplete="username"
            autocapitalize="off"
            spellcheck="false"
            class="mt-1.5 h-11 w-full rounded-[18px] px-4 text-[15px] text-[#20324a] outline-none transition placeholder:text-[#94a3b8] focus:bg-white focus:ring-4"
            :class="
              fieldErrors.username
                ? 'border border-[#f1a0ac] bg-[#fff8f9] focus:border-[#d94b66] focus:ring-[#ffe3e8]'
                : 'border border-[#d9e4f6] bg-[linear-gradient(180deg,#ffffff_0%,#f8fbff_100%)] focus:border-[#4f8df7] focus:ring-[#ddebff]'
            "
            placeholder="请输入用户名"
            @blur="handleBlur('username')"
            @input="handleInput('username')"
            @keydown.stop
            @copy.stop
            @cut.stop
            @paste.stop
          />
          <p class="mt-1 min-h-[16px] text-right text-[11px] leading-4 text-[#d94b66]">
            {{ touched.username ? (fieldErrors.username ?? "") : "" }}
          </p>
        </label>

        <label class="block">
          <span class="text-sm font-medium text-[#31455f]">邮箱</span>
          <input
            v-model="form.email"
            type="email"
            autocomplete="email"
            inputmode="email"
            class="mt-1.5 h-11 w-full rounded-[18px] px-4 text-[15px] text-[#20324a] outline-none transition placeholder:text-[#94a3b8] focus:bg-white focus:ring-4"
            :class="
              fieldErrors.email
                ? 'border border-[#f1a0ac] bg-[#fff8f9] focus:border-[#d94b66] focus:ring-[#ffe3e8]'
                : 'border border-[#d9e4f6] bg-[linear-gradient(180deg,#ffffff_0%,#f8fbff_100%)] focus:border-[#4f8df7] focus:ring-[#ddebff]'
            "
            placeholder="请输入邮箱"
            @blur="handleBlur('email')"
            @input="handleInput('email')"
            @keydown.stop
            @copy.stop
            @cut.stop
            @paste.stop
          />
          <p class="mt-1 min-h-[16px] text-right text-[11px] leading-4 text-[#d94b66]">
            {{ touched.email ? (fieldErrors.email ?? "") : "" }}
          </p>
        </label>

        <label class="block">
          <span class="text-sm font-medium text-[#31455f]">手机号</span>
          <input
            v-model="form.phone"
            autocomplete="tel"
            inputmode="tel"
            class="mt-1.5 h-11 w-full rounded-[18px] px-4 text-[15px] text-[#20324a] outline-none transition placeholder:text-[#94a3b8] focus:bg-white focus:ring-4"
            :class="
              fieldErrors.phone
                ? 'border border-[#f1a0ac] bg-[#fff8f9] focus:border-[#d94b66] focus:ring-[#ffe3e8]'
                : 'border border-[#d9e4f6] bg-[linear-gradient(180deg,#ffffff_0%,#f8fbff_100%)] focus:border-[#4f8df7] focus:ring-[#ddebff]'
            "
            placeholder="请输入手机号"
            @blur="handleBlur('phone')"
            @input="handleInput('phone')"
            @keydown.stop
            @copy.stop
            @cut.stop
            @paste.stop
          />
          <p class="mt-1 min-h-[16px] text-right text-[11px] leading-4 text-[#d94b66]">
            {{ touched.phone ? (fieldErrors.phone ?? "") : "" }}
          </p>
        </label>

        <label class="block">
          <span class="text-sm font-medium text-[#31455f]">密码</span>
          <div class="relative mt-1.5">
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              class="h-11 w-full rounded-[18px] px-4 pr-11 text-[15px] text-[#20324a] outline-none transition placeholder:text-[#94a3b8] focus:bg-white focus:ring-4"
              :class="
                fieldErrors.password
                  ? 'border border-[#f1a0ac] bg-[#fff8f9] focus:border-[#d94b66] focus:ring-[#ffe3e8]'
                  : 'border border-[#d9e4f6] bg-[linear-gradient(180deg,#ffffff_0%,#f8fbff_100%)] focus:border-[#4f8df7] focus:ring-[#ddebff]'
              "
              placeholder="请输入密码"
              @blur="handleBlur('password')"
              @input="handleInput('password')"
              @keydown.stop
              @copy.stop
              @cut.stop
              @paste.stop
            />
            <button
              type="button"
              class="absolute right-2.5 top-1/2 flex h-6.5 w-6.5 -translate-y-1/2 items-center justify-center rounded-full text-[#6d7e95] transition hover:bg-[#eef4ff] hover:text-[#2563eb]"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              @click="showPassword = !showPassword"
            >
              <svg v-if="showPassword" viewBox="0 0 24 24" class="h-3.5 w-3.5 fill-none stroke-current stroke-[1.8]">
                <path d="M3 3 21 21" />
                <path d="M10.58 10.58a2 2 0 0 0 2.84 2.84" />
                <path d="M9.88 5.09A10.94 10.94 0 0 1 12 4.91c5.05 0 9.27 3.11 10.5 7.09a11.44 11.44 0 0 1-3.03 4.67" />
                <path d="M6.53 6.53A11.28 11.28 0 0 0 1.5 12c1.23 3.98 5.45 7.09 10.5 7.09 1.47 0 2.88-.26 4.17-.73" />
              </svg>
              <svg v-else viewBox="0 0 24 24" class="h-3.5 w-3.5 fill-none stroke-current stroke-[1.8]">
                <path d="M1.5 12C2.73 8.02 6.95 4.91 12 4.91S21.27 8.02 22.5 12C21.27 15.98 17.05 19.09 12 19.09S2.73 15.98 1.5 12Z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            </button>
          </div>
          <p class="mt-1 min-h-[16px] text-right text-[11px] leading-4 text-[#d94b66]">
            {{ touched.password ? (fieldErrors.password ?? "") : "" }}
          </p>
        </label>

        <label class="block">
          <span class="text-sm font-medium text-[#31455f]">确认密码</span>
          <div class="relative mt-1.5">
            <input
              v-model="form.confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              autocomplete="new-password"
              class="h-11 w-full rounded-[18px] px-4 pr-11 text-[15px] text-[#20324a] outline-none transition placeholder:text-[#94a3b8] focus:bg-white focus:ring-4"
              :class="
                fieldErrors.confirmPassword
                  ? 'border border-[#f1a0ac] bg-[#fff8f9] focus:border-[#d94b66] focus:ring-[#ffe3e8]'
                  : 'border border-[#d9e4f6] bg-[linear-gradient(180deg,#ffffff_0%,#f8fbff_100%)] focus:border-[#4f8df7] focus:ring-[#ddebff]'
              "
              placeholder="请再次输入密码"
              @blur="handleBlur('confirmPassword')"
              @input="handleInput('confirmPassword')"
              @keydown.stop
              @copy.stop
              @cut.stop
              @paste.stop
            />
            <button
              type="button"
              class="absolute right-2.5 top-1/2 flex h-6.5 w-6.5 -translate-y-1/2 items-center justify-center rounded-full text-[#6d7e95] transition hover:bg-[#eef4ff] hover:text-[#2563eb]"
              :aria-label="showConfirmPassword ? '隐藏确认密码' : '显示确认密码'"
              @click="showConfirmPassword = !showConfirmPassword"
            >
              <svg v-if="showConfirmPassword" viewBox="0 0 24 24" class="h-3.5 w-3.5 fill-none stroke-current stroke-[1.8]">
                <path d="M3 3 21 21" />
                <path d="M10.58 10.58a2 2 0 0 0 2.84 2.84" />
                <path d="M9.88 5.09A10.94 10.94 0 0 1 12 4.91c5.05 0 9.27 3.11 10.5 7.09a11.44 11.44 0 0 1-3.03 4.67" />
                <path d="M6.53 6.53A11.28 11.28 0 0 0 1.5 12c1.23 3.98 5.45 7.09 10.5 7.09 1.47 0 2.88-.26 4.17-.73" />
              </svg>
              <svg v-else viewBox="0 0 24 24" class="h-3.5 w-3.5 fill-none stroke-current stroke-[1.8]">
                <path d="M1.5 12C2.73 8.02 6.95 4.91 12 4.91S21.27 8.02 22.5 12C21.27 15.98 17.05 19.09 12 19.09S2.73 15.98 1.5 12Z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            </button>
          </div>
          <p class="mt-1 min-h-[16px] text-right text-[11px] leading-4 text-[#d94b66]">
            {{ touched.confirmPassword ? (fieldErrors.confirmPassword ?? "") : "" }}
          </p>
        </label>

        <button
          type="submit"
          :disabled="submitting"
          class="w-full rounded-[18px] bg-[#2563eb] px-5 py-3.5 text-sm font-semibold text-white shadow-[0_14px_30px_rgba(37,99,235,0.22)] transition hover:bg-[#1f57cf] disabled:cursor-not-allowed disabled:opacity-60"
        >
          {{ submitting ? "正在创建账号..." : "注册" }}
        </button>
      </form>
    </div>
  </AuthShell>
</template>
