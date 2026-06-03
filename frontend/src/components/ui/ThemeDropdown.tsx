import { useTheme } from '../../theme/ThemeProvider';
import { themeList } from '../../theme/themes';
import { Select } from './Select';

export function ThemeDropdown(): JSX.Element {
  const { theme, setTheme } = useTheme();

  return (
    <Select
      value={theme}
      aria-label="Select color theme"
      onChange={(event) => setTheme(event.target.value as typeof theme)}
      className="h-9 w-auto px-2"
    >
      {themeList.map((t) => (
        <option key={t.id} value={t.id}>
          {t.label}
        </option>
      ))}
    </Select>
  );
}
