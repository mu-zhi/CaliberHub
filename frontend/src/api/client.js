const API_BASE = "/api";
const AUTH_TOKEN_PATH = "/system/auth/token";
const SESSION_TOKEN_KEY = "dd_auth_token";

function dispatchApiError(detail) {
  if (typeof window === "undefined") {
    return;
  }
  window.dispatchEvent(new CustomEvent("dd-api-error", { detail }));
}

function buildQuery(params = {}) {
  const pairs = Object.entries(params).filter(([, value]) => value !== undefined && value !== null && `${value}` !== "");
  if (pairs.length === 0) {
    return "";
  }
  const search = new URLSearchParams();
  pairs.forEach(([key, value]) => search.set(key, `${value}`));
  return `?${search.toString()}`;
}

function maybeDispatchByStatus(path, status, code, message, requestId) {
  if (path !== AUTH_TOKEN_PATH && [401, 403, 429].includes(status)) {
    dispatchApiError({
      status,
      code,
      message,
      requestId,
      path,
    });
  } else if (path !== AUTH_TOKEN_PATH && status >= 500) {
    dispatchApiError({
      status,
      code,
      message,
      requestId,
      path,
    });
  }
}

async function buildHttpError(response, path) {
  let code = "";
  let requestId = response.headers.get("X-Request-Id") || "";
  let message = `请求失败（${response.status}）`;
  try {
    const payload = await response.json();
    if (payload?.message) {
      message = payload.message;
    }
    if (payload?.code) {
      code = `${payload.code}`;
    }
    if (payload?.requestId) {
      requestId = `${payload.requestId}`;
    }
  } catch (_) {
    // ignore parse error
  }
  const error = new Error(message);
  error.status = response.status;
  error.code = code;
  error.requestId = requestId;
  error.path = path;
  maybeDispatchByStatus(path, response.status, code, message, requestId);
  return error;
}

export async function apiRequest(path, options = {}) {
  const { token, method = "GET", body, query, headers = {} } = options;
  const finalHeaders = {
    "Content-Type": "application/json",
    ...headers,
  };
  const resolvedToken = resolveAuthToken(path, token);
  if (resolvedToken) {
    finalHeaders.Authorization = `Bearer ${resolvedToken}`;
  }
  let response;
  try {
    response = await fetch(`${API_BASE}${path}${buildQuery(query)}`, {
      method,
      headers: finalHeaders,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (_) {
    const error = new Error("网络连接失败，请检查网络后重试");
    error.status = 0;
    error.code = "NETWORK_ERROR";
    error.requestId = "";
    error.path = path;
    dispatchApiError({
      status: 0,
      code: "NETWORK_ERROR",
      message: error.message,
      requestId: "",
      path,
    });
    throw error;
  }
  if (!response.ok) {
    throw await buildHttpError(response, path);
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

export async function apiRequestWithMeta(path, options = {}) {
  const { token, method = "GET", body, query, headers = {} } = options;
  const finalHeaders = {
    "Content-Type": "application/json",
    ...headers,
  };
  const resolvedToken = resolveAuthToken(path, token);
  if (resolvedToken) {
    finalHeaders.Authorization = `Bearer ${resolvedToken}`;
  }
  let response;
  try {
    response = await fetch(`${API_BASE}${path}${buildQuery(query)}`, {
      method,
      headers: finalHeaders,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (_) {
    const error = new Error("网络连接失败，请检查网络后重试");
    error.status = 0;
    error.code = "NETWORK_ERROR";
    error.requestId = "";
    error.path = path;
    dispatchApiError({
      status: 0,
      code: "NETWORK_ERROR",
      message: error.message,
      requestId: "",
      path,
    });
    throw error;
  }
  if (!response.ok) {
    throw await buildHttpError(response, path);
  }
  const meta = {
    status: response.status,
    requestId: response.headers.get("X-Request-Id") || "",
    path,
  };
  if (response.status === 204) {
    return { data: null, meta };
  }
  const data = await response.json();
  return { data, meta };
}

function parseSseEvent(rawChunk) {
  let event = "message";
  const dataLines = [];
  rawChunk.split(/\r?\n/).forEach((line) => {
    if (line.startsWith("event:")) {
      event = line.slice(6).trim();
    } else if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).trimStart());
    }
  });
  if (dataLines.length === 0) {
    return null;
  }
  const dataText = dataLines.join("\n");
  let data = dataText;
  try {
    data = JSON.parse(dataText);
  } catch (_) {
    // keep plain text
  }
  return { event, data };
}

export async function apiSseRequest(path, options = {}) {
  const { token, method = "POST", body, query, headers = {}, onEvent } = options;
  const finalHeaders = {
    "Content-Type": "application/json",
    Accept: "text/event-stream",
    ...headers,
  };
  const resolvedToken = resolveAuthToken(path, token);
  if (resolvedToken) {
    finalHeaders.Authorization = `Bearer ${resolvedToken}`;
  }
  let response;
  try {
    response = await fetch(`${API_BASE}${path}${buildQuery(query)}`, {
      method,
      headers: finalHeaders,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (_) {
    const error = new Error("网络连接失败，请检查网络后重试");
    error.status = 0;
    error.code = "NETWORK_ERROR";
    error.requestId = "";
    error.path = path;
    dispatchApiError({
      status: 0,
      code: "NETWORK_ERROR",
      message: error.message,
      requestId: "",
      path,
    });
    throw error;
  }
  if (!response.ok) {
    throw await buildHttpError(response, path);
  }
  if (!response.body) {
    throw new Error("流式响应不可用");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";
  let donePayload = null;

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, "\n");
    let separatorIndex = buffer.indexOf("\n\n");
    while (separatorIndex >= 0) {
      const rawChunk = buffer.slice(0, separatorIndex).trim();
      buffer = buffer.slice(separatorIndex + 2);
      if (rawChunk) {
        const parsedEvent = parseSseEvent(rawChunk);
        if (parsedEvent) {
          if (typeof onEvent === "function") {
            onEvent(parsedEvent);
          }
          if (parsedEvent.event === "done") {
            donePayload = parsedEvent.data;
          }
          if (parsedEvent.event === "error") {
            const message = typeof parsedEvent.data === "string"
              ? parsedEvent.data
              : (parsedEvent.data?.message || "流式导入失败");
            throw new Error(message);
          }
        }
      }
      separatorIndex = buffer.indexOf("\n\n");
    }
  }

  if (donePayload === null) {
    throw new Error("流式导入未返回最终结果");
  }
  return donePayload;
}

export function parseJsonText(text, fallback) {
  if (!text) {
    return fallback;
  }
  try {
    return JSON.parse(text);
  } catch (_) {
    return fallback;
  }
}

function resolveAuthToken(path, explicitToken) {
  if (explicitToken) {
    return explicitToken;
  }
  if (path === AUTH_TOKEN_PATH || typeof window === "undefined") {
    return "";
  }
  try {
    return window.sessionStorage.getItem(SESSION_TOKEN_KEY) || "";
  } catch (_) {
    return "";
  }
}
