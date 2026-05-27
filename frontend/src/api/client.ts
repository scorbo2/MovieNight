const rawBase = import.meta.env.VITE_API_BASE_PATH ?? '/MovieNight/';
export const API_BASE = rawBase.endsWith('/') ? rawBase : `${rawBase}/`;

function normalizePath(path: string): string {
  return path.replace(/^\//, '');
}

export function buildApiUrl(path: string): string {
  return `${API_BASE}${normalizePath(path)}`;
}

export function buildQueryString<T extends object>(params: T): string {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (
      value === undefined ||
      value === null ||
      value === '' ||
      (typeof value !== 'string' && typeof value !== 'number' && typeof value !== 'boolean')
    ) {
      return;
    }

    searchParams.set(key, String(value));
  });

  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
}

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const url = buildApiUrl(path);
  const init: RequestInit = { ...options };

  if (init.body && !(init.body instanceof FormData)) {
    init.headers = {
      'Content-Type': 'application/json',
      ...(init.headers ?? {}),
    };
  }

  const response = await fetch(url, init);
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Unknown error' }));
    throw new Error(error.message ?? `HTTP ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
