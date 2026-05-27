export function getPositiveIntParam(value: string | null, fallback: number): number {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

export function getNullableStringParam(value: string | null): string | undefined {
  if (value == null) {
    return undefined;
  }

  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

export function setParam(target: URLSearchParams, key: string, value: string | number | undefined | null): void {
  if (value === undefined || value === null || value === '') {
    target.delete(key);
    return;
  }

  target.set(key, String(value));
}
