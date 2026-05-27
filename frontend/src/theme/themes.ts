export const themes = {
  light: {
    id: 'light',
    label: 'Light',
  },
  dark: {
    id: 'dark',
    label: 'Dark',
  },
} as const;

export type ThemeName = keyof typeof themes;

export const themeList = Object.values(themes);
