import { Navigate, Route, Routes } from 'react-router-dom';
import { AdminAppShell } from './admin/AdminAppShell';
import { AdminDashboardPage } from './admin/pages/AdminDashboardPage';
import { AdminGroupEditPage } from './admin/pages/AdminGroupEditPage';
import { AdminGroupsPage } from './admin/pages/AdminGroupsPage';
import { AdminItemEditPage } from './admin/pages/AdminItemEditPage';
import { AdminItemsPage } from './admin/pages/AdminItemsPage';
import { BrowseAppShell } from './browse/BrowseAppShell';
import { BrowseHomePage } from './browse/pages/BrowseHomePage';
import { GroupDetailPage } from './browse/pages/GroupDetailPage';
import { ItemDetailPage } from './browse/pages/ItemDetailPage';
import { SearchResultsPage } from './browse/pages/SearchResultsPage';

function App(): JSX.Element {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/browse" replace />} />

      <Route path="/browse" element={<BrowseAppShell />}>
        <Route index element={<BrowseHomePage />} />
        <Route path="groups/:groupId" element={<GroupDetailPage />} />
        <Route path="items/:itemId" element={<ItemDetailPage />} />
        <Route path="search" element={<SearchResultsPage />} />
      </Route>

      <Route path="/admin" element={<AdminAppShell />}>
        <Route index element={<AdminDashboardPage />} />
        <Route path="groups" element={<AdminGroupsPage />} />
        <Route path="groups/new" element={<AdminGroupEditPage />} />
        <Route path="groups/:groupId/edit" element={<AdminGroupEditPage />} />
        <Route path="items" element={<AdminItemsPage />} />
        <Route path="items/new" element={<AdminItemEditPage />} />
        <Route path="items/:itemId/edit" element={<AdminItemEditPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/browse" replace />} />
    </Routes>
  );
}

export default App;
