import { NavLink } from 'react-router-dom';
import { Card } from '../components/ui/Card';

const links = [
  { to: '/admin', label: 'Dashboard', end: true },
  { to: '/admin/groups', label: 'Groups' },
  { to: '/admin/items', label: 'Items' },
];

export function AdminNav(): JSX.Element {
  return (
    <Card className="sticky top-24 p-3">
      <nav className="flex flex-col gap-1">
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            end={link.end}
            className={({ isActive }) =>
              `rounded-md px-3 py-2 text-sm font-medium no-underline transition-colors ${
                isActive ? 'bg-brand text-white' : 'text-content-secondary hover:bg-bg-subtle hover:text-content'
              }`
            }
          >
            {link.label}
          </NavLink>
        ))}
      </nav>
    </Card>
  );
}
