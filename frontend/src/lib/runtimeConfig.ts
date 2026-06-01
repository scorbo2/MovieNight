/// <reference path="./runtimeConfig.d.ts" />
import { getPositiveIntParam } from './url'

export function getRuntimePageSize(value: string | null) {
  const runtimeDefaultPageSize = (typeof window !== 'undefined' && window?.MOVIENIGHT_CONFIG?.PAGE_SIZE) || 10;
  return getPositiveIntParam(value, runtimeDefaultPageSize);
}

export function getRuntimeApiBasePath() {
  const rawBase = ((typeof window !== 'undefined' && window?.MOVIENIGHT_CONFIG?.API_BASE_PATH) || import.meta.env.VITE_API_BASE_PATH) ?? '/MovieNight/';
  return rawBase.endsWith('/') ? rawBase : `${rawBase}/`;
}
