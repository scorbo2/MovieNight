import { cn } from '../../lib/cn';

interface ThumbnailProps {
  src?: string;
  alt: string;
  className?: string;
}

function FilmIcon(): JSX.Element {
  return (
    <svg className="h-12 w-12 text-content-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="M7 5v14M17 5v14M3 9h4M3 15h4M17 9h4M17 15h4" />
    </svg>
  );
}

export function Thumbnail({ src, alt, className }: ThumbnailProps): JSX.Element {
  return (
    <div className={cn('relative aspect-video overflow-hidden rounded-lg bg-thumb-placeholder', className)}>
      {src ? (
        <img src={src} alt={alt} className="h-full w-full object-cover" />
      ) : (
        <div className="flex h-full w-full items-center justify-center">
          <FilmIcon />
        </div>
      )}
    </div>
  );
}
