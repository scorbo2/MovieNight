import { useMutation, useQueryClient } from '@tanstack/react-query';
import { deleteItem } from '../../api/items';
import { ConfirmDialog } from '../../components/shared/ConfirmDialog';
import { useToast } from '../../components/ui/ToastProvider';

interface ItemDeleteDialogProps {
  itemId: number | null;
  itemTitle?: string;
  open: boolean;
  onClose: () => void;
  onDeleted?: () => void;
}

export function ItemDeleteDialog({ itemId, itemTitle, open, onClose, onDeleted }: ItemDeleteDialogProps): JSX.Element {
  const queryClient = useQueryClient();
  const { pushToast } = useToast();
  const mutation = useMutation({
    mutationFn: async () => {
      if (!itemId) {
        return;
      }
      await deleteItem(itemId);
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['items'] }),
        queryClient.invalidateQueries({ queryKey: ['group-items'] }),
        queryClient.invalidateQueries({ queryKey: ['item', itemId ?? 0] }),
      ]);
      pushToast({ title: 'Item deleted', tone: 'success' });
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
      title="Delete item"
      description={`Delete ${itemTitle ?? 'this item'}? This cannot be undone.`}
      confirmLabel="Delete item"
      loading={mutation.isPending}
      onConfirm={() => mutation.mutate()}
      onClose={onClose}
    />
  );
}
