import { useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { deleteThumbnail, getThumbnailUrl, uploadThumbnail, type ThumbnailEntityType } from '../../api/thumbnails';
import { ConfirmDialog } from '../../components/shared/ConfirmDialog';
import { Thumbnail } from '../../components/shared/Thumbnail';
import { Button } from '../../components/ui/Button';
import { useToast } from '../../components/ui/ToastProvider';
import { cn } from '../../lib/cn';

interface ThumbnailPanelProps {
  entityType: ThumbnailEntityType;
  entityId: number;
  title: string;
  hasThumbnail: boolean;
}

export function ThumbnailPanel({ entityType, entityId, title, hasThumbnail }: ThumbnailPanelProps): JSX.Element {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const queryClient = useQueryClient();
  const { pushToast } = useToast();

  const invalidate = async (): Promise<void> => {
    if (entityType === 'media-groups') {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['groups'] }),
        queryClient.invalidateQueries({ queryKey: ['group', entityId] }),
      ]);
      return;
    }

    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['items'] }),
      queryClient.invalidateQueries({ queryKey: ['group-items'] }),
      queryClient.invalidateQueries({ queryKey: ['item', entityId] }),
    ]);
  };

  const uploadMutation = useMutation({
    mutationFn: async () => {
      if (!selectedFile) {
        throw new Error('Choose an image first');
      }
      return uploadThumbnail(entityType, entityId, selectedFile, hasThumbnail);
    },
    onSuccess: async (response) => {
      await invalidate();
      setSelectedFile(null);
      pushToast({ title: response.message, tone: 'success' });
    },
    onError: (error) => {
      pushToast({ title: 'Thumbnail update failed', description: error instanceof Error ? error.message : 'Unknown error', tone: 'error' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async () => deleteThumbnail(entityType, entityId),
    onSuccess: async () => {
      await invalidate();
      pushToast({ title: 'Thumbnail deleted', tone: 'success' });
      setConfirmDelete(false);
    },
    onError: (error) => {
      pushToast({ title: 'Thumbnail delete failed', description: error instanceof Error ? error.message : 'Unknown error', tone: 'error' });
    },
  });

  return (
    <div className="space-y-4">
      <Thumbnail
        alt={`${title} thumbnail`}
        src={hasThumbnail ? `${getThumbnailUrl(entityType, entityId)}?t=${Date.now()}` : undefined}
        className="border border-border-subtle"
      />
      <div
        className={cn(
          'rounded-lg border border-dashed border-border p-6 text-center transition-colors',
          isDragging ? 'border-brand bg-brand/5' : 'bg-bg-subtle/40',
        )}
        onDragOver={(event) => {
          event.preventDefault();
          setIsDragging(true);
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={(event) => {
          event.preventDefault();
          setIsDragging(false);
          const file = event.dataTransfer.files[0];
          if (file) {
            setSelectedFile(file);
          }
        }}
      >
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
        />
        <p className="text-sm font-medium text-content">Drag and drop an image here</p>
        <p className="mt-1 text-sm text-content-secondary">or click below to browse for a JPEG/PNG file.</p>
        <Button className="mt-4" variant="secondary" onClick={() => inputRef.current?.click()}>
          Choose file
        </Button>
        {selectedFile ? <p className="mt-3 text-sm text-content-secondary">Selected: {selectedFile.name}</p> : null}
      </div>
      <div className="flex flex-wrap gap-3">
        <Button loading={uploadMutation.isPending} onClick={() => uploadMutation.mutate()}>
          {hasThumbnail ? 'Replace thumbnail' : 'Upload thumbnail'}
        </Button>
        {hasThumbnail ? (
          <Button variant="danger" onClick={() => setConfirmDelete(true)} loading={deleteMutation.isPending}>
            Delete thumbnail
          </Button>
        ) : null}
      </div>
      <ConfirmDialog
        open={confirmDelete}
        title="Delete thumbnail"
        description={`Remove the thumbnail for ${title}?`}
        confirmLabel="Delete thumbnail"
        loading={deleteMutation.isPending}
        onConfirm={() => deleteMutation.mutate()}
        onClose={() => setConfirmDelete(false)}
      />
    </div>
  );
}
