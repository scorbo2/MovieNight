import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { listFiles } from '../../api/files';
import type { FileBrowserEntry } from '../../api/files';
import { cn } from '../../lib/cn';
import { Button } from '../ui/Button';
import { Dialog } from '../ui/Dialog';

interface FileBrowserFieldProps {
  value: string;
  onChange: (path: string) => void;
  error?: boolean;
  disabled?: boolean;
}

function FolderIcon(): JSX.Element {
  return (
    <svg
      className="h-4 w-4 shrink-0 text-warning"
      fill="currentColor"
      viewBox="0 0 20 20"
      aria-hidden="true"
    >
      <path d="M2 6a2 2 0 012-2h4l2 2h6a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z" />
    </svg>
  );
}

function FileIcon(): JSX.Element {
  return (
    <svg
      className="h-4 w-4 shrink-0 text-brand"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M3.375 19.5h17.25m-17.25 0a1.125 1.125 0 01-1.125-1.125M3.375 19.5h1.5C5.496 19.5 6 18.996 6 18.375m-3.75.125V14.25m0 5.25V14.25m0 0a1.125 1.125 0 011.125-1.125h9.75a1.125 1.125 0 011.125 1.125M3.375 14.25h17.25M21 12.75a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V9z"
      />
    </svg>
  );
}

function UpIcon(): JSX.Element {
  return (
    <svg
      className="h-4 w-4"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <path strokeLinecap="round" strokeLinejoin="round" d="M5 10l7-7m0 0l7 7m-7-7v18" />
    </svg>
  );
}

export function FileBrowserField({
  value,
  onChange,
  error,
  disabled,
}: FileBrowserFieldProps): JSX.Element {
  const [open, setOpen] = useState(false);
  const [browsePath, setBrowsePath] = useState<string | undefined>(undefined);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['files', browsePath ?? null],
    queryFn: () => listFiles(browsePath),
    enabled: open,
  });

  function handleOpen() {
    setBrowsePath(value || undefined);
    setOpen(true);
  }

  function handleClose() {
    setOpen(false);
  }

  function handleEntryClick(entry: FileBrowserEntry) {
    if (entry.type === 'directory') {
      setBrowsePath(entry.path);
    } else {
      onChange(entry.path);
      setOpen(false);
    }
  }

  return (
    <>
      <div className="flex gap-2">
        <div
          className={cn(
            'flex h-10 min-w-0 flex-1 items-center overflow-hidden rounded-md border bg-input-bg px-3 text-sm shadow-sm',
            error ? 'border-danger' : 'border-input-border',
            value ? 'text-content' : 'text-content-muted',
          )}
        >
          <span className="truncate">{value || 'No file selected'}</span>
        </div>
        <Button type="button" variant="secondary" onClick={handleOpen} disabled={disabled}>
          Browse…
        </Button>
      </div>

      <Dialog open={open} title="Browse for media file" onClose={handleClose} size="md">
        <div className="space-y-3">
          <div className="flex min-h-[1.75rem] items-center gap-2">
            {data?.parent ? (
              <button
                type="button"
                className="inline-flex shrink-0 items-center gap-1 rounded px-2 py-1 text-xs font-medium text-content-secondary transition-colors hover:bg-bg-subtle hover:text-content"
                onClick={() => setBrowsePath(data.parent)}
              >
                <UpIcon />
                Up
              </button>
            ) : null}
            {data ? (
              <p className="truncate font-mono text-xs text-content-secondary">{data.path}</p>
            ) : null}
          </div>

          <div className="max-h-96 overflow-y-auto rounded-md border border-border-subtle">
            {isLoading ? (
              <div className="flex items-center justify-center px-4 py-10 text-sm text-content-muted">
                Loading…
              </div>
            ) : isError ? (
              <div className="px-4 py-10 text-center text-sm text-danger">
                Could not load files. Check server connection.
              </div>
            ) : data?.entries.length === 0 ? (
              <div className="px-4 py-10 text-center text-sm text-content-muted">
                This folder is empty.
              </div>
            ) : (
              <ul role="listbox" aria-label="Files and folders">
                {data?.entries.map((entry) => (
                  <li key={entry.path} role="option" aria-selected={false}>
                    <button
                      type="button"
                      className={cn(
                        'flex w-full items-center gap-3 border-b border-border-subtle px-4 py-2.5 text-left text-sm transition-colors last:border-b-0',
                        entry.type === 'file'
                          ? 'hover:bg-bg-subtle text-content font-medium'
                          : 'hover:bg-bg-subtle text-content-secondary',
                      )}
                      onClick={() => handleEntryClick(entry)}
                    >
                      {entry.type === 'directory' ? <FolderIcon /> : <FileIcon />}
                      <span className="truncate">{entry.name}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </Dialog>
    </>
  );
}
