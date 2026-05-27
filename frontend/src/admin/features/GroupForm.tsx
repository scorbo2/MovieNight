import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import type { GroupUpsertPayload, MediaGroup } from '../../api/types';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Select } from '../../components/ui/Select';
import { Textarea } from '../../components/ui/Textarea';

const schema = z.object({
  parentGroupId: z.string().optional(),
  title: z.string().trim().min(1, 'Title is required'),
  description: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

interface GroupFormProps {
  groups: MediaGroup[];
  initialValues?: {
    parentGroupId?: number | null;
    title?: string;
    description?: string | null;
  };
  loading?: boolean;
  currentGroupId?: number;
  onSubmit: (payload: GroupUpsertPayload) => Promise<void>;
}

function FieldError({ message }: { message?: string }): JSX.Element | null {
  return message ? <p className="text-sm text-danger">{message}</p> : null;
}

export function GroupForm({ groups, initialValues, loading, currentGroupId, onSubmit }: GroupFormProps): JSX.Element {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      parentGroupId: initialValues?.parentGroupId ? String(initialValues.parentGroupId) : '',
      title: initialValues?.title ?? '',
      description: initialValues?.description ?? '',
    },
  });

  const parentOptions = groups.filter((group) => group.id !== currentGroupId);

  return (
    <form
      className="space-y-5"
      onSubmit={form.handleSubmit(async (values) => {
        await onSubmit({
          parentGroupId: values.parentGroupId ? Number(values.parentGroupId) : null,
          title: values.title.trim(),
          description: values.description?.trim() ? values.description.trim() : null,
        });
      })}
    >
      <div className="space-y-2">
        <label className="text-sm font-medium text-content">Parent group</label>
        <Select {...form.register('parentGroupId')}>
          <option value="">None (top-level)</option>
          {parentOptions.map((group) => (
            <option key={group.id} value={group.id}>
              {group.title}
            </option>
          ))}
        </Select>
      </div>

      <div className="space-y-2">
        <label className="text-sm font-medium text-content">Title</label>
        <Input error={Boolean(form.formState.errors.title)} {...form.register('title')} />
        <FieldError message={form.formState.errors.title?.message} />
      </div>

      <div className="space-y-2">
        <label className="text-sm font-medium text-content">Description</label>
        <Textarea {...form.register('description')} />
      </div>

      <div className="flex justify-end">
        <Button type="submit" loading={loading}>
          Save group
        </Button>
      </div>
    </form>
  );
}
