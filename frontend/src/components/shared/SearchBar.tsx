import { useEffect, useState } from 'react';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';

export interface SearchValues {
  titleContains?: string;
  descriptionContains?: string;
  tagContains?: string;
}

interface SearchBarProps {
  values: SearchValues;
  onSearch: (values: SearchValues) => void;
  showDescription?: boolean;
  showTag?: boolean;
  compact?: boolean;
}

export function SearchBar({ values, onSearch, showDescription = false, showTag = false, compact = false }: SearchBarProps): JSX.Element {
  const [formValues, setFormValues] = useState<SearchValues>(values);

  useEffect(() => {
    setFormValues(values);
  }, [values]);

  return (
    <form
      className={`grid gap-3 ${compact ? 'md:grid-cols-[1.6fr_1fr_auto]' : showDescription || showTag ? 'lg:grid-cols-4' : 'md:grid-cols-[2fr_auto]'}`}
      onSubmit={(event) => {
        event.preventDefault();
        onSearch(formValues);
      }}
    >
      <Input
        value={formValues.titleContains ?? ''}
        placeholder="Search titles"
        onChange={(event) => setFormValues((current) => ({ ...current, titleContains: event.target.value }))}
      />
      {showDescription ? (
        <Input
          value={formValues.descriptionContains ?? ''}
          placeholder="Search descriptions"
          onChange={(event) => setFormValues((current) => ({ ...current, descriptionContains: event.target.value }))}
        />
      ) : null}
      {showTag ? (
        <Input
          value={formValues.tagContains ?? ''}
          placeholder="Filter by tag"
          onChange={(event) => setFormValues((current) => ({ ...current, tagContains: event.target.value }))}
        />
      ) : null}
      <div className="flex gap-3">
        <Button type="submit" className="w-full sm:w-auto">
          Search
        </Button>
        <Button
          type="button"
          variant="secondary"
          className="w-full sm:w-auto"
          onClick={() => {
            const emptyValues: SearchValues = {};
            setFormValues(emptyValues);
            onSearch(emptyValues);
          }}
        >
          Clear
        </Button>
      </div>
    </form>
  );
}
