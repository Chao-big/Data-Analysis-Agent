export function validateRegisterUsername(username: string): string | null {
  const normalized = username.trim();

  if (!normalized) {
    return "请填写用户名。";
  }

  if (normalized.length < 3) {
    return "用户名至少需要 3 个字符。";
  }

  return null;
}

export function validateRegisterEmail(email: string): string | null {
  const normalized = email.trim();

  if (!normalized) {
    return "请填写邮箱。";
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalized)) {
    return "请输入常见格式的邮箱地址，例如 name@example.com。";
  }

  return null;
}

export function validateRegisterPhone(phone: string): string | null {
  const normalized = phone.trim();

  if (!normalized) {
    return "请填写手机号。";
  }

  if (!/^1[3-9]\d{9}$/.test(normalized)) {
    return "请输入 11 位中国大陆手机号。";
  }

  return null;
}

export function validateRegisterPassword(password: string): string | null {
  if (!password.trim()) {
    return "请填写密码。";
  }

  if (password.length < 8) {
    return "密码至少需要 8 位。";
  }

  if (!/[a-z]/.test(password)) {
    return "密码需要至少包含 1 个小写字母。";
  }

  if (!/[A-Z]/.test(password)) {
    return "密码需要至少包含 1 个大写字母。";
  }

  if (!/[!@#$%^&*()_\-+=[\]{};:'"\\|,.<>/?]/.test(password)) {
    return "密码需要至少包含 1 个特殊字符。";
  }

  return null;
}

export function validateRegisterConfirmPassword(password: string, confirmPassword: string): string | null {
  if (!confirmPassword.trim()) {
    return "请填写确认密码。";
  }

  if (password !== confirmPassword) {
    return "两次输入的密码不一致。";
  }

  return null;
}
