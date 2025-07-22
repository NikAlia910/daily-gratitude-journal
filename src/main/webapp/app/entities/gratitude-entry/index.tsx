import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import GratitudeEntry from './gratitude-entry';
import GratitudeEntryDetail from './gratitude-entry-detail';
import GratitudeEntryUpdate from './gratitude-entry-update';
import GratitudeEntryDeleteDialog from './gratitude-entry-delete-dialog';

const GratitudeEntryRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<GratitudeEntry />} />
    <Route path="new" element={<GratitudeEntryUpdate />} />
    <Route path=":id">
      <Route index element={<GratitudeEntryDetail />} />
      <Route path="edit" element={<GratitudeEntryUpdate />} />
      <Route path="delete" element={<GratitudeEntryDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default GratitudeEntryRoutes;
