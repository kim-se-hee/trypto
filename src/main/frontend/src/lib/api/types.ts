export interface ApiResponseDto<T> {
  status: number;
  code: string;
  message: string;
  data: T;
}

export interface CursorPageResponseDto<T> {
  content: T[];
  nextCursor: number | null;
  hasNext: boolean;
}

export type QueryPrimitive = string | number | boolean | null | undefined;
export type QueryParams = Record<string, QueryPrimitive>;

export interface ApiRequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  query?: QueryParams;
}

export class ApiClientError extends Error {
  readonly status: number;
  readonly code: string;
  readonly data: unknown;

  constructor(message: string, status: number, code: string, data?: unknown) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.code = code;
    this.data = data;
  }
}

export function isApiClientError(error: unknown): error is ApiClientError {
  return error instanceof ApiClientError;
}

