import { useTheme } from '../../theme/ThemeProvider';
import { themeList } from '../../theme/ThemeProvider';

export function ThemeDropdown(): JSX.Element {
  const { theme, setTheme } = useTheme();

  return (
    <select
      value={theme}
      aria-label="Select color theme"
      onChange={(event) => setTheme(event.target.value as typeof theme)}
      className="h-9 rounded-md border border-input-border bg-input-bg px-2 text-sm text-content shadow-sm transition-colors focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/40"
    >
      {themeList.map((t) => (
        <option key={t.id} value={t.id}>
          {t.label}
        </option>
      ))}
    </select>
  );
}
