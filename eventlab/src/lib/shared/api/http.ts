export class HttpError extends Error {
  readonly status: number;
  readonly code: string | undefined;

  constructor(status: number, code?: string) {
    super(`HTTP ${status}`);
    this.name = "HttpError";
    this.status = status;
    this.code = code;
  }
}

export const apiBaseUrl = "http://localhost:8080";

export async function ensureSuccess(response: Response): Promise<void> {
  if (response.ok) {
    return;
  }

  let code: string | undefined;

  try {
    const body: unknown = await response.json();

    if (
      typeof body === "object" &&
      body !== null &&
      "code" in body &&
      typeof body.code === "string"
    ) {
      code = body.code;
    }
  } catch {
    // Save HTTP error status even without correct JSON
  }

  throw new HttpError(response.status, code);
}
