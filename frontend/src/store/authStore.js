import { create } from "zustand";
import { apiRequest } from "../api/client";
import { API_CONTRACTS } from "../api/contracts";

const TOKEN_KEY = "dd_auth_token";
const TOKEN_EXPIRE_AT_KEY = "dd_auth_token_expire_at";
const USER_KEY = "dd_auth_user";
const ROLE_KEY = "dd_ui_role";
const ROLES_KEY = "dd_auth_roles";

const ROLE_USERNAMES = {
  support: "support",
  admin: "admin",
  expert: "expert",
  governance: "governance",
  frontline: "frontline",
  compliance: "compliance",
};

function safeRead(key, fallback = "") {
  try {
    return window.localStorage.getItem(key) || fallback;
  } catch (_) {
    return fallback;
  }
}

function safeWrite(key, value) {
  try {
    window.localStorage.setItem(key, value || "");
  } catch (_) {
    // ignore
  }
}

function safeReadJson(key, fallback) {
  try {
    const value = window.localStorage.getItem(key);
    if (!value) {
      return fallback;
    }
    return JSON.parse(value);
  } catch (_) {
    return fallback;
  }
}

function safeWriteJson(key, value) {
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch (_) {
    // ignore
  }
}

function safeReadSession(key, fallback = "") {
  try {
    return window.sessionStorage.getItem(key) || fallback;
  } catch (_) {
    return fallback;
  }
}

function safeWriteSession(key, value) {
  try {
    if (value) {
      window.sessionStorage.setItem(key, value);
      return;
    }
    window.sessionStorage.removeItem(key);
  } catch (_) {
    // ignore
  }
}

function normalizeRoles(value) {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => `${item || ""}`.trim())
    .filter((item) => item !== "");
}

function normalizeExpireAt(value) {
  const text = `${value || ""}`.trim();
  if (!text) {
    return "";
  }
  const timestamp = Date.parse(text);
  if (Number.isNaN(timestamp)) {
    return "";
  }
  return new Date(timestamp).toISOString();
}

function isExpired(expireAt) {
  const timestamp = Date.parse(`${expireAt || ""}`);
  return Number.isNaN(timestamp) || timestamp <= Date.now();
}

function clearTokenSession() {
  safeWriteSession(TOKEN_KEY, "");
  safeWriteSession(TOKEN_EXPIRE_AT_KEY, "");
}

function loadSessionToken() {
  const token = safeReadSession(TOKEN_KEY, "");
  const tokenExpireAt = normalizeExpireAt(safeReadSession(TOKEN_EXPIRE_AT_KEY, ""));
  if (!token || !tokenExpireAt || isExpired(tokenExpireAt)) {
    clearTokenSession();
    return {
      token: "",
      tokenExpireAt: "",
    };
  }
  return {
    token,
    tokenExpireAt,
  };
}

const initialSession = loadSessionToken();
const initialToken = initialSession.token;
const initialTokenExpireAt = initialSession.tokenExpireAt;

if (!initialToken) {
  safeWrite(USER_KEY, "");
  safeWriteJson(ROLES_KEY, []);
}

export const useAuthStore = create((set, get) => ({
  role: safeRead(ROLE_KEY, "admin"),
  token: initialToken,
  tokenExpireAt: initialTokenExpireAt,
  username: initialToken ? safeRead(USER_KEY, "") : "",
  roles: initialToken ? normalizeRoles(safeReadJson(ROLES_KEY, [])) : [],
  loginError: "",
  loading: false,
  setRole(role) {
    const nextRole = role || get().role || "admin";
    set({ role: nextRole, token: "", tokenExpireAt: "", username: "", roles: [], loginError: "" });
    safeWrite(ROLE_KEY, nextRole);
    clearTokenSession();
    safeWrite(USER_KEY, "");
    safeWriteJson(ROLES_KEY, []);
  },
  async loginByRole(role, password) {
    const nextRole = role || get().role || "admin";
    const username = ROLE_USERNAMES[nextRole] || ROLE_USERNAMES.support;
    const safePassword = `${password || ""}`.trim();
    set({ loading: true, loginError: "", role: nextRole });
    safeWrite(ROLE_KEY, nextRole);
    if (!safePassword) {
      set({ loading: false, loginError: "请输入密码后再登录" });
      return false;
    }
    try {
      const result = await apiRequest(API_CONTRACTS.authToken, {
        method: "POST",
        body: { username, password: safePassword },
      });
      const token = result?.accessToken || "";
      const tokenExpireAt = normalizeExpireAt(result?.expireAt);
      if (!token || !tokenExpireAt) {
        throw new Error("登录响应缺少有效会话信息，请联系系统管理员");
      }
      const currentUser = result?.username || username;
      const roles = normalizeRoles(result?.roles);
      set({
        token,
        tokenExpireAt,
        username: currentUser,
        roles,
        loading: false,
        loginError: "",
      });
      safeWriteSession(TOKEN_KEY, token);
      safeWriteSession(TOKEN_EXPIRE_AT_KEY, tokenExpireAt);
      safeWrite(USER_KEY, currentUser);
      safeWriteJson(ROLES_KEY, roles);
      return true;
    } catch (error) {
      set({
        loading: false,
        loginError: error.message || "登录失败",
        token: "",
        tokenExpireAt: "",
        username: "",
        roles: [],
      });
      clearTokenSession();
      safeWrite(USER_KEY, "");
      safeWriteJson(ROLES_KEY, []);
      return false;
    }
  },
  logout() {
    set({ token: "", tokenExpireAt: "", username: "", roles: [] });
    clearTokenSession();
    safeWrite(USER_KEY, "");
    safeWriteJson(ROLES_KEY, []);
  },
}));
