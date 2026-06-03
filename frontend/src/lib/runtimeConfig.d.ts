export {};

declare global {
  interface MovieNightConfig {
    API_BASE_PATH: string;
    PAGE_SIZE: number;
  }

  interface Window {
    MOVIENIGHT_CONFIG?: MovieNightConfig;
  }
}
