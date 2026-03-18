import {
  ApiClientError,
  type ApiRequestOptions,
  type ApiResponseDto,
  type QueryParams,
} from "./types";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "";
const SUCCESS_CODES = new Set(["SUCCESS", "CREATED"]);

function toQueryString(query?: QueryParams): string {
  if (!query) return "";

  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === "") {
      continue;
    }
    search.set(key, String(value));
  }

  const raw = search.toString();
  return raw ? `?${raw}` : "";
}

function toUrl(path: string, query?: QueryParams): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}${toQueryString(query)}`;
}

async function parseJson(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text) as unknown;
  } catch {
    return null;
  }
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const { body, query, headers, ...requestInit } = options;
  const response = await fetch(toUrl(path, query), {
    ...requestInit,
    headers: {
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  const parsed = await parseJson(response);
  const envelope = parsed as ApiResponseDto<T> | null;

  if (!response.ok) {
    if (envelope && typeof envelope === "object") {
      throw new ApiClientError(
        envelope.message ?? "Request failed",
        envelope.status ?? response.status,
        envelope.code ?? "UNKNOWN_ERROR",
        envelope.data,
      );
    }

    throw new ApiClientError("Request failed", response.status, "UNKNOWN_ERROR");
  }

  if (!envelope || typeof envelope !== "object") {
    throw new ApiClientError("Invalid API response", response.status, "INVALID_RESPONSE");
  }

  if (!SUCCESS_CODES.has(envelope.code)) {
    throw new ApiClientError(
      envelope.message ?? "Request failed",
      envelope.status ?? response.status,
      envelope.code,
      envelope.data,
    );
  }

  return envelope.data;
}

export function apiGet<T>(path: string, query?: QueryParams): Promise<T> {
  return apiRequest<T>(path, { method: "GET", query });
}

export function apiPost<T>(path: string, body?: unknown): Promise<T> {
  return apiRequest<T>(path, { method: "POST", body });
}

export function apiPut<T>(path: string, body?: unknown): Promise<T> {
  return apiRequest<T>(path, { method: "PUT", body });
}

