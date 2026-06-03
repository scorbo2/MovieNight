import { useMutation, useQueryClient } from '@tanstack/react-query';
import { deleteGroup } from '../../api/groups';
import { ConfirmDialog } from '../../components/shared/ConfirmDialog';
import { useToast } from '../../components/ui/ToastProvider';

interface GroupDeleteDialogProps {
  groupId: number | null;
  groupTitle?: string;
  open: boolean;
  onClose: () => void;
  onDeleted?: () => void;
}

export function GroupDeleteDialog({ groupId, groupTitle, open, onClose, onDeleted }: GroupDeleteDialogProps): JSX.Element {
  const queryClient = useQueryClient();
  const { pushToast } = useToast();
  const mutation = useMutation({
    mutationFn: async () => {
      if (!groupId) {
        return;
      }
      await deleteGroup(groupId);
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['groups'] }),
        queryClient.invalidateQueries({ queryKey: ['group', groupId ?? 0] }),
      ]);
      pushToast({ title: 'Group deleted', tone: 'success' });
      onDeleted?.();
      onClose();
    },
    onError: (error) => {
      pushToast({ title: 'Delete failed', description: error instanceof Error ? error.message : 'Unknown error', tone: 'error' });
    },
  });

  return (
    <ConfirmDialog
      open={open}
      title="Delete group"
      description={`Delete ${groupTitle ?? 'this group'}? This cannot be undone.`}
      confirmLabel="Delete group"
      loading={mutation.isPending}
      onConfirm={() => mutation.mutate()}
      onClose={onClose}
    />
  );
}
