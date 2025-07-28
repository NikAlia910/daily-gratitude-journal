import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Button, Table } from 'reactstrap';
import { JhiItemCount, JhiPagination, TextFormat, getPaginationState } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSort, faSortDown, faSortUp } from '@fortawesome/free-solid-svg-icons';
import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';
import { ASC, DESC, ITEMS_PER_PAGE, SORT } from 'app/shared/util/pagination.constants';
import { overridePaginationStateWithQueryParams } from 'app/shared/util/entity-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntities } from './gratitude-entry.reducer';

export const GratitudeEntry = () => {
  const dispatch = useAppDispatch();

  const pageLocation = useLocation();
  const navigate = useNavigate();

  const [paginationState, setPaginationState] = useState(
    overridePaginationStateWithQueryParams(getPaginationState(pageLocation, ITEMS_PER_PAGE, 'id'), pageLocation.search),
  );

  const gratitudeEntryList = useAppSelector(state => state.gratitudeEntry.entities);
  const loading = useAppSelector(state => state.gratitudeEntry.loading);
  const totalItems = useAppSelector(state => state.gratitudeEntry.totalItems);

  const getAllEntities = () => {
    dispatch(
      getEntities({
        page: paginationState.activePage - 1,
        size: paginationState.itemsPerPage,
        sort: `${paginationState.sort},${paginationState.order}`,
      }),
    );
  };

  const sortEntities = () => {
    getAllEntities();
    const endURL = `?page=${paginationState.activePage}&sort=${paginationState.sort},${paginationState.order}`;
    if (pageLocation.search !== endURL) {
      navigate(`${pageLocation.pathname}${endURL}`);
    }
  };

  useEffect(() => {
    sortEntities();
  }, [paginationState.activePage, paginationState.order, paginationState.sort]);

  useEffect(() => {
    const params = new URLSearchParams(pageLocation.search);
    const page = params.get('page');
    const sort = params.get(SORT);
    if (page && sort) {
      const sortSplit = sort.split(',');
      setPaginationState({
        ...paginationState,
        activePage: +page,
        sort: sortSplit[0],
        order: sortSplit[1],
      });
    }
  }, [pageLocation.search]);

  const sort = p => () => {
    setPaginationState({
      ...paginationState,
      order: paginationState.order === ASC ? DESC : ASC,
      sort: p,
    });
  };

  const handlePagination = currentPage =>
    setPaginationState({
      ...paginationState,
      activePage: currentPage,
    });

  const handleSyncList = () => {
    sortEntities();
  };

  const getSortIconByFieldName = (fieldName: string) => {
    const sortFieldName = paginationState.sort;
    const order = paginationState.order;
    if (sortFieldName !== fieldName) {
      return faSort;
    }
    return order === ASC ? faSortUp : faSortDown;
  };

  return (
    <div data-testid="gratitude-entry-list-container">
      <h2 id="gratitude-entry-heading" data-cy="GratitudeEntryHeading" data-testid="gratitude-entry-heading">
        Gratitude Entries
        <div className="d-flex justify-content-end" data-testid="gratitude-entry-actions">
          <Button className="me-2" color="info" onClick={handleSyncList} disabled={loading} data-testid="btn-refresh-gratitude-entries">
            <FontAwesomeIcon icon="sync" spin={loading} /> Refresh list
          </Button>
          <Link
            to="/gratitude-entry/new"
            className="btn btn-primary jh-create-entity"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            data-testid="btn-create-gratitude-entry"
          >
            <FontAwesomeIcon icon="plus" />
            &nbsp; Create a new Gratitude Entry
          </Link>
        </div>
      </h2>
      <div className="table-responsive" data-testid="gratitude-entry-table-container">
        {loading && (
          <div data-testid="gratitude-entry-loading" className="text-center p-3">
            <FontAwesomeIcon icon="spinner" spin /> Loading gratitude entries...
          </div>
        )}
        {gratitudeEntryList && gratitudeEntryList.length > 0 ? (
          <Table responsive data-testid="gratitude-entry-table">
            <thead data-testid="gratitude-entry-table-header">
              <tr>
                <th className="hand" onClick={sort('id')} data-testid="header-sort-id">
                  ID <FontAwesomeIcon icon={getSortIconByFieldName('id')} />
                </th>
                <th className="hand" onClick={sort('date')} data-testid="header-sort-date">
                  Date <FontAwesomeIcon icon={getSortIconByFieldName('date')} />
                </th>
                <th className="hand" onClick={sort('entry')} data-testid="header-sort-entry">
                  Entry <FontAwesomeIcon icon={getSortIconByFieldName('entry')} />
                </th>
                <th className="hand" onClick={sort('mood')} data-testid="header-sort-mood">
                  Mood <FontAwesomeIcon icon={getSortIconByFieldName('mood')} />
                </th>
                <th className="hand" onClick={sort('timestamp')} data-testid="header-sort-timestamp">
                  Timestamp <FontAwesomeIcon icon={getSortIconByFieldName('timestamp')} />
                </th>
                <th data-testid="header-user">
                  User <FontAwesomeIcon icon="sort" />
                </th>
                <th data-testid="header-actions">Actions</th>
              </tr>
            </thead>
            <tbody data-testid="gratitude-entry-table-body">
              {gratitudeEntryList.map((gratitudeEntry, i) => (
                <tr key={`entity-${i}`} data-cy="entityTable" data-testid={`gratitude-entry-row-${gratitudeEntry.id}`}>
                  <td data-testid={`gratitude-entry-id-${gratitudeEntry.id}`}>
                    <Button
                      tag={Link}
                      to={`/gratitude-entry/${gratitudeEntry.id}`}
                      color="link"
                      size="sm"
                      data-testid={`btn-view-gratitude-entry-${gratitudeEntry.id}`}
                    >
                      {gratitudeEntry.id}
                    </Button>
                  </td>
                  <td data-testid={`gratitude-entry-date-${gratitudeEntry.id}`}>
                    {gratitudeEntry.date ? <TextFormat type="date" value={gratitudeEntry.date} format={APP_LOCAL_DATE_FORMAT} /> : null}
                  </td>
                  <td data-testid={`gratitude-entry-text-${gratitudeEntry.id}`}>{gratitudeEntry.entry}</td>
                  <td data-testid={`gratitude-entry-mood-${gratitudeEntry.id}`}>
                    <span className={`badge bg-${gratitudeEntry.mood?.toLowerCase() || 'secondary'}`}>{gratitudeEntry.mood}</span>
                  </td>
                  <td data-testid={`gratitude-entry-timestamp-${gratitudeEntry.id}`}>
                    {gratitudeEntry.timestamp ? <TextFormat type="date" value={gratitudeEntry.timestamp} format={APP_DATE_FORMAT} /> : null}
                  </td>
                  <td data-testid={`gratitude-entry-user-${gratitudeEntry.id}`}>{gratitudeEntry.user ? gratitudeEntry.user.login : ''}</td>
                  <td className="text-end" data-testid={`gratitude-entry-actions-${gratitudeEntry.id}`}>
                    <div className="btn-group flex-btn-group-container" data-testid={`gratitude-entry-action-buttons-${gratitudeEntry.id}`}>
                      <Button
                        tag={Link}
                        to={`/gratitude-entry/${gratitudeEntry.id}`}
                        color="info"
                        size="sm"
                        data-cy="entityDetailsButton"
                        data-testid={`btn-details-gratitude-entry-${gratitudeEntry.id}`}
                      >
                        <FontAwesomeIcon icon="eye" /> <span className="d-none d-md-inline">View</span>
                      </Button>
                      <Button
                        tag={Link}
                        to={`/gratitude-entry/${gratitudeEntry.id}/edit?page=${paginationState.activePage}&sort=${paginationState.sort},${paginationState.order}`}
                        color="primary"
                        size="sm"
                        data-cy="entityEditButton"
                        data-testid={`btn-edit-gratitude-entry-${gratitudeEntry.id}`}
                      >
                        <FontAwesomeIcon icon="pencil-alt" /> <span className="d-none d-md-inline">Edit</span>
                      </Button>
                      <Button
                        onClick={() =>
                          (window.location.href = `/gratitude-entry/${gratitudeEntry.id}/delete?page=${paginationState.activePage}&sort=${paginationState.sort},${paginationState.order}`)
                        }
                        color="danger"
                        size="sm"
                        data-cy="entityDeleteButton"
                        data-testid={`btn-delete-gratitude-entry-${gratitudeEntry.id}`}
                      >
                        <FontAwesomeIcon icon="trash" /> <span className="d-none d-md-inline">Delete</span>
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : (
          !loading && (
            <div className="alert alert-warning" data-testid="gratitude-entry-empty-state">
              <FontAwesomeIcon icon="info-circle" /> No Gratitude Entries found. Start your gratitude journey by creating your first entry!
            </div>
          )
        )}
      </div>
      {totalItems ? (
        <div className={gratitudeEntryList && gratitudeEntryList.length > 0 ? '' : 'd-none'} data-testid="gratitude-entry-pagination">
          <div className="justify-content-center d-flex" data-testid="gratitude-entry-item-count">
            <JhiItemCount page={paginationState.activePage} total={totalItems} itemsPerPage={paginationState.itemsPerPage} />
          </div>
          <div className="justify-content-center d-flex" data-testid="gratitude-entry-pagination-controls">
            <JhiPagination
              activePage={paginationState.activePage}
              onSelect={handlePagination}
              maxButtons={5}
              itemsPerPage={paginationState.itemsPerPage}
              totalItems={totalItems}
            />
          </div>
        </div>
      ) : (
        ''
      )}
    </div>
  );
};

export default GratitudeEntry;
