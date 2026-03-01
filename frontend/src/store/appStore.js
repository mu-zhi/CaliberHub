import { create } from "zustand";

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

const RECENT_KEY = "dd_recent_paths";
const FAVORITES_KEY = "dd_favorite_scene_ids";
const NAV_COLLAPSED_KEY = "dd_nav_collapsed";

export const useAppStore = create((set, get) => ({
  recents: safeReadJson(RECENT_KEY, []),
  favorites: safeReadJson(FAVORITES_KEY, []),
  navCollapsed: safeReadJson(NAV_COLLAPSED_KEY, false),
  recordRecent(path) {
    const next = [
      { path, at: new Date().toISOString() },
      ...get().recents.filter((item) => item.path !== path),
    ].slice(0, 80);
    set({ recents: next });
    safeWriteJson(RECENT_KEY, next);
  },
  toggleFavorite(sceneId) {
    const id = `${sceneId || ""}`;
    if (!id) {
      return;
    }
    const exists = get().favorites.includes(id);
    const next = exists ? get().favorites.filter((item) => item !== id) : [id, ...get().favorites].slice(0, 100);
    set({ favorites: next });
    safeWriteJson(FAVORITES_KEY, next);
  },
  setNavCollapsed(collapsed) {
    const value = Boolean(collapsed);
    set({ navCollapsed: value });
    safeWriteJson(NAV_COLLAPSED_KEY, value);
  },
}));
