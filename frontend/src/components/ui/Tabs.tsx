import { cn } from '../../lib/cn';

export interface TabItem {
  id: string;
  label: string;
}

interface TabsProps {
  tabs: TabItem[];
  activeTab: string;
  onChange: (id: string) => void;
  className?: string;
}

export function Tabs({ tabs, activeTab, onChange, className }: TabsProps): JSX.Element {
  return (
    <div className={cn('inline-flex rounded-lg bg-bg-subtle p-1', className)}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          className={cn(
            'rounded-md px-3 py-2 text-sm font-medium transition-colors',
            activeTab === tab.id ? 'bg-surface text-content shadow-sm' : 'text-content-secondary hover:text-content',
          )}
          onClick={() => onChange(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}
