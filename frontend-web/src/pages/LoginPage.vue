<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import AuthShell from "../components/AuthShell.vue";
import { loginWithAuthApi } from "../lib/auth";

const route = useRoute();
const router = useRouter();

const nextPath = typeof route.query.next === "string" ? route.query.next : "/";
const identifier = ref(typeof route.query.identifier === "string" ? route.query.identifier : "");
const password = ref("");
const showPassword = ref(false);
const submitting = ref(false);

const touched = reactive({
  identifier: false,
  password: false,
});

const fieldErrors = reactive({
  identifier: "",
  password: "",
});

function validateIdentifier() {
  fieldErrors.identifier = identifier.value.trim() ? "" : "请填写账号。";
}

function validatePassword() {
  fieldErrors.password = password.value.trim() ? "" : "请填写密码。";
}

function handleIdentifierBlur() {
  touched.identifier = true;
  validateIdentifier();
}

function handlePasswordBlur() {
  touched.password = true;
  validatePassword();
}

function handleIdentifierInput() {
  if (touched.identifier || fieldErrors.identifier) {
    validateIdentifier();
  }
}

function handlePasswordInput() {
  if (touched.password || fieldErrors.password) {
    validatePassword();
  }
}

function validateForm() {
  touched.identifier = true;
  touched.password = true;
  validateIdentifier();
  validatePassword();

  return !fieldErrors.identifier && !fieldErrors.password;
}

async function handleSubmit() {
  if (!validateForm()) {
    return;
  }

  submitting.value = true;
  await new Promise((resolve) => window.setTimeout(resolve, 450));

  const result = await loginWithAuthApi({
    identifier: identifier.value,
    password: password.value,
  });

  if (!result.success) {
    touched.password = true;
    fieldErrors.password = result.message;
    submitting.value = false;
    return;
  }

  router.replace(nextPath || "/");
}
</script>

<template>
  <AuthShell
    eyebrow="sign in"
    title="登录到分析工作台"
    alternate-label="还没有账号？"
    alternate-action-text="去注册"
    :alternate-action-to="{ path: '/register', query: nextPath ? { next: nextPath } : {} }"
  >
    <div class="space-y-6">
      <div>
        <div class="text-[11px] font-semibold uppercase tracking-[0.26em] text-[#2f5fe8]">welcome back</div>
        <h2 class="display-face mt-3 text-[2rem] font-semibold tracking-[-0.05em] text-[#122033]">欢迎回来</h2>
      </div>

      <form class="space-y-4" @submit.prevent="handleSubmit">
        <label class="block">
          <span class="text-sm font-medium text-[#31455f]">账号</span>
          <input
            v-model="identifier"
            autocomplete="username"
            autocapitalize="off"
            spellcheck="false"
            enterkeyhint="next"
            class="mt-2 h-12 w-full rounded-[20px] px-4 text-[15px] text-[#20324a] outline-none transition placeholder:text-[#94a3b8] focus:bg-white focus:ring-4"
            :class="
              fieldErrors.identifier
                ? 'border border-[#f1a0ac] bg-[#fff8f9] focus:border-[#d94b66] focus:ring-[#ffe3e8]'
                : 'border border-[#d9e4f6] bg-[linear-gradient(180deg,#ffffff_0%,#f8fbff_100%)] focus:border-[#4f8df7] focus:ring-[#ddebff]'
            "
            placeholder="用户名/手机号/email"
            @blur="handleIdentifierBlur"
            @input="handleIdentifierInput"
            @keydown.stop
            @copy.stop
            @cut.stop
            @paste.stop
          />
          <p class="mt-1 min-h-[20px] text-right text-xs leading-5 text-[#d94b66]">
            {{ touched.identifier ? fieldErrors.identifier : "" }}
          </p>
        </label>

        <label class="block">
          <span class="text-sm font-medium text-[#31455f]">密码</span>
          <div class="relative mt-2">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              enterkeyhint="go"
              class="h-12 w-full rounded-[20px] px-4 pr-14 text-[15px] text-[#20324a] outline-none transition placeholder:text-[#9aa7bb] focus:bg-white focus:ring-4"
              :class="
                fieldErrors.password
                  ? 'border border-[#f1a0ac] bg-[#fff8f9] focus:border-[#d94b66] focus:ring-[#ffe3e8]'
                  : 'border border-[#d9e4f6] bg-[linear-gradient(180deg,#ffffff_0%,#f8fbff_100%)] focus:border-[#4f8df7] focus:ring-[#ddebff]'
              "
              placeholder="请输入密码"
              @blur="handlePasswordBlur"
              @input="handlePasswordInput"
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
          <p class="mt-1 min-h-[20px] text-right text-xs leading-5 text-[#d94b66]">
            {{ touched.password ? fieldErrors.password : "" }}
          </p>
        </label>

        <button
          type="submit"
          :disabled="submitting"
          class="w-full rounded-[18px] bg-[#2563eb] px-5 py-3.5 text-sm font-semibold text-white shadow-[0_14px_30px_rgba(37,99,235,0.22)] transition hover:bg-[#1f57cf] disabled:cursor-not-allowed disabled:opacity-60"
        >
          {{ submitting ? "正在登录..." : "登录" }}
        </button>
      </form>
    </div>
  </AuthShell>
</template>
