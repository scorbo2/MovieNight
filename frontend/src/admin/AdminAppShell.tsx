import { Outlet } from 'react-router-dom';
import { AdminNav } from './AdminNav';
import { AdminTopBar } from './AdminTopBar';

export function AdminAppShell(): JSX.Element {
  return (
    <div className="min-h-screen bg-bg-app">
      <AdminTopBar />
      <main className="page-shell">
        <div className="grid gap-6 lg:grid-cols-[220px_minmax(0,1fr)]">
          <aside>
            <AdminNav />
          </aside>
          <section className="min-w-0">
            <Outlet />
          </section>
        </div>
      </main>
    </div>
  );
}
