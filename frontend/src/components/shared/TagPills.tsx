import { Badge } from '../ui/Badge';

interface TagPillsProps {
  tags: string[];
  onTagClick?: (tag: string) => void;
}

export function TagPills({ tags, onTagClick }: TagPillsProps): JSX.Element {
  return (
    <div className="flex flex-wrap gap-2">
      {tags.map((tag) =>
        onTagClick ? (
          <button key={tag} type="button" onClick={() => onTagClick(tag)}>
            <Badge>{tag}</Badge>
          </button>
        ) : (
          <Badge key={tag}>{tag}</Badge>
        ),
      )}
    </div>
  );
}
