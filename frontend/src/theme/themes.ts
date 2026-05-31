export const themes = {
  light: {
    id: 'light',
    label: 'Light',
  },
  dark: {
    id: 'dark',
    label: 'Dark',
  },
  mediumGray: {
    id: 'medium-gray',
    label: 'Medium Gray',
  },
  deepBlue: {
    id: 'deep-blue',
    label: 'Deep Blue',
  },
} as const;

export type ThemeName = (typeof themes)[keyof typeof themes]['id'];

export const themeList = Object.values(themes);
