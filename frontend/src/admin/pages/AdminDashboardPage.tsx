import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { listGroups } from '../../api/groups';
import { listItems } from '../../api/items';
import { Card } from '../../components/ui/Card';
import { Skeleton } from '../../components/ui/Skeleton';

export function AdminDashboardPage(): JSX.Element {
  const groupsQuery = useQuery({
    queryKey: ['groups', { pageNumber: 1, pageSize: 1 }],
    queryFn: () => listGroups({ pageNumber: 1, pageSize: 1 }),
  });
  const itemsQuery = useQuery({
    queryKey: ['items', { pageNumber: 1, pageSize: 1 }],
    queryFn: () => listItems({ pageNumber: 1, pageSize: 1 }),
  });

  const stats = [
    { label: 'Media groups', value: groupsQuery.data?.totalCount, href: '/admin/groups' },
    { label: 'Media items', value: itemsQuery.data?.totalCount, href: '/admin/items' },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="section-title">Admin dashboard</h2>
        <p className="section-copy mt-1">Manage groups, items, and thumbnails from one place.</p>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        {stats.map((stat) => (
          <Card key={stat.label}>
            <p className="text-sm text-content-secondary">{stat.label}</p>
            {stat.value === undefined ? (
              <Skeleton className="mt-3 h-10 w-24" />
            ) : (
              <p className="mt-3 text-4xl font-bold text-content">{stat.value}</p>
            )}
            <Link className="mt-4 inline-flex text-sm font-medium" to={stat.href}>
              View {stat.label.toLowerCase()} →
            </Link>
          </Card>
        ))}
      </div>
      <Card>
        <h3 className="text-lg font-semibold text-content">Quick actions</h3>
        <div className="mt-4 flex flex-wrap gap-3">
          <Link className="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white no-underline" to="/admin/groups/new">
            New group
          </Link>
          <Link className="rounded-md bg-surface px-4 py-2 text-sm font-medium text-content no-underline border border-border" to="/admin/items">
            View items
          </Link>
        </div>
      </Card>
    </div>
  );
}
