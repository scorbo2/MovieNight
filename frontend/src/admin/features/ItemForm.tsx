import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import type { ItemUpsertPayload, MediaGroup } from '../../api/types';
import { FileBrowserField } from '../../components/shared/FileBrowserField';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Select } from '../../components/ui/Select';
import { Textarea } from '../../components/ui/Textarea';

const schema = z.object({
  mediaGroupId: z.coerce.number().int().positive('Group is required'),
  title: z.string().trim().min(1, 'Title is required'),
  description: z.string().optional(),
  lastWatchedDate: z.string().optional(),
  mediaFilePath: z.string().trim().min(1, 'Media file path is required'),
  tags: z.array(z.string().trim().min(1)).default([]),
});

type FormValues = z.infer<typeof schema>;

interface ItemFormProps {
  groups: MediaGroup[];
  initialValues?: Partial<ItemUpsertPayload>;
  loading?: boolean;
  onSubmit: (payload: ItemUpsertPayload) => Promise<void>;
  onSaveAndAddAnother?: (payload: ItemUpsertPayload) => Promise<void>;
}

function FieldError({ message }: { message?: string }): JSX.Element | null {
  return message ? <p className="text-sm text-danger">{message}</p> : null;
}

function buildPayload(values: FormValues): ItemUpsertPayload {
  return {
    mediaGroupId: values.mediaGroupId,
    title: values.title.trim(),
    description: values.description?.trim() ? values.description.trim() : null,
    lastWatchedDate: values.lastWatchedDate?.trim() ? values.lastWatchedDate.trim() : null,
    mediaFilePath: values.mediaFilePath.trim(),
    tags: values.tags,
  };
}

export function ItemForm({ groups, initialValues, loading, onSubmit, onSaveAndAddAnother }: ItemFormProps): JSX.Element {
  const [tagInput, setTagInput] = useState('');
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      mediaGroupId: initialValues?.mediaGroupId ?? groups[0]?.id ?? 0,
      title: initialValues?.title ?? '',
      description: initialValues?.description ?? '',
      lastWatchedDate: initialValues?.lastWatchedDate ?? '',
      mediaFilePath: initialValues?.mediaFilePath ?? '',
      tags: initialValues?.tags ?? [],
    },
  });

  const handleSaveAndAddAnother = form.handleSubmit(async (values) => {
    await onSaveAndAddAnother!(buildPayload(values));
    form.reset({
      mediaGroupId: values.mediaGroupId,
      title: '',
      description: '',
      lastWatchedDate: '',
      mediaFilePath: '',
      tags: [],
    });
    setTagInput('');
  });

  return (
    <form
      className="space-y-5"
      onSubmit={form.handleSubmit(async (values) => {
        await onSubmit(buildPayload(values));
      })}
    >
      <div className="grid gap-5 md:grid-cols-2">
        <div className="space-y-2">
          <label className="text-sm font-medium text-content">Group</label>
          <Select error={Boolean(form.formState.errors.mediaGroupId)} {...form.register('mediaGroupId')}>
            <option value="">Choose a group</option>
            {groups.map((group) => (
              <option key={group.id} value={group.id}>
                {group.title}
              </option>
            ))}
          </Select>
          <FieldError message={form.formState.errors.mediaGroupId?.message} />
        </div>

        <div className="space-y-2">
          <label className="text-sm font-medium text-content">Last watched date</label>
          <Input type="date" {...form.register('lastWatchedDate')} />
        </div>
      </div>

      <div className="space-y-2">
        <label className="text-sm font-medium text-content">Title</label>
        <Input error={Boolean(form.formState.errors.title)} {...form.register('title')} />
        <FieldError message={form.formState.errors.title?.message} />
      </div>

      <Controller
        control={form.control}
        name="mediaFilePath"
        render={({ field }) => (
          <div className="space-y-2">
            <label className="text-sm font-medium text-content">Media file path</label>
            <FileBrowserField
              value={field.value}
              onChange={field.onChange}
              error={Boolean(form.formState.errors.mediaFilePath)}
            />
            <FieldError message={form.formState.errors.mediaFilePath?.message} />
          </div>
        )}
      />

      <div className="space-y-2">
        <label className="text-sm font-medium text-content">Description</label>
        <Textarea {...form.register('description')} />
      </div>

      <Controller
        control={form.control}
        name="tags"
        render={({ field }) => (
          <div className="space-y-3">
            <label className="text-sm font-medium text-content">Tags</label>
            <div className="flex gap-3">
              <Input
                value={tagInput}
                placeholder="Add a tag"
                onChange={(event) => setTagInput(event.target.value)}
                onKeyDown={(event) => {
                  if ((event.key === 'Enter' || event.key === ',') && tagInput.trim()) {
                    event.preventDefault();
                    const nextTag = tagInput.trim();
                    if (!field.value.includes(nextTag)) {
                      field.onChange([...field.value, nextTag]);
                    }
                    setTagInput('');
                  }
                }}
              />
              <Button
                type="button"
                variant="secondary"
                onClick={() => {
                  const nextTag = tagInput.trim();
                  if (!nextTag || field.value.includes(nextTag)) {
                    return;
                  }
                  field.onChange([...field.value, nextTag]);
                  setTagInput('');
                }}
              >
                Add tag
              </Button>
            </div>
            <div className="flex flex-wrap gap-2">
              {field.value.map((tag) => (
                <button
                  type="button"
                  key={tag}
                  onClick={() => field.onChange(field.value.filter((currentTag) => currentTag !== tag))}
                >
                  <Badge>{tag} ×</Badge>
                </button>
              ))}
            </div>
          </div>
        )}
      />

      <div className="flex justify-end gap-3">
        {onSaveAndAddAnother ? (
          <Button
            type="button"
            variant="secondary"
            loading={loading}
            onClick={() => void handleSaveAndAddAnother()}
          >
            Save and add another
          </Button>
        ) : null}
        <Button type="submit" loading={loading}>
          Save item
        </Button>
      </div>
    </form>
  );
}
