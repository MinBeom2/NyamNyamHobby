export type ApiFieldError = {
  field: string;
  reason: string;
};

export type ApiErrorBody = {
  code: string;
  message: string;
  fields: ApiFieldError[] | null;
  retryAfterSeconds: number | null;
};

export type ApiEnvelope<T> =
  | { success: true; data: T; error: null }
  | { success: false; data: null; error: ApiErrorBody };
