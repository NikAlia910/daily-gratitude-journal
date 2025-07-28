import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { TextFormat } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './gratitude-entry.reducer';

export const GratitudeEntryDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const gratitudeEntryEntity = useAppSelector(state => state.gratitudeEntry.entity);
  const loading = useAppSelector(state => state.gratitudeEntry.loading);

  return (
    <div data-testid="gratitude-entry-detail-container">
      <Row>
        <Col md="8">
          <h2 data-cy="gratitudeEntryDetailsHeading" data-testid="gratitude-entry-detail-heading">
            Gratitude Entry Details
          </h2>
          {loading ? (
            <div data-testid="gratitude-entry-detail-loading" className="text-center p-3">
              <FontAwesomeIcon icon="spinner" spin /> Loading gratitude entry details...
            </div>
          ) : gratitudeEntryEntity ? (
            <dl className="jh-entity-details" data-testid="gratitude-entry-detail-content">
              <dt data-testid="gratitude-entry-detail-id-label">
                <span id="id">ID</span>
              </dt>
              <dd data-testid="gratitude-entry-detail-id-value">{gratitudeEntryEntity.id}</dd>
              <dt data-testid="gratitude-entry-detail-date-label">
                <span id="date">Date</span>
              </dt>
              <dd data-testid="gratitude-entry-detail-date-value">
                {gratitudeEntryEntity.date ? (
                  <TextFormat value={gratitudeEntryEntity.date} type="date" format={APP_LOCAL_DATE_FORMAT} />
                ) : null}
              </dd>
              <dt data-testid="gratitude-entry-detail-entry-label">
                <span id="entry">Entry</span>
              </dt>
              <dd data-testid="gratitude-entry-detail-entry-value">{gratitudeEntryEntity.entry}</dd>
              <dt data-testid="gratitude-entry-detail-mood-label">
                <span id="mood">Mood</span>
              </dt>
              <dd data-testid="gratitude-entry-detail-mood-value">
                <span className={`badge bg-${gratitudeEntryEntity.mood?.toLowerCase() || 'secondary'}`}>{gratitudeEntryEntity.mood}</span>
              </dd>
              <dt data-testid="gratitude-entry-detail-timestamp-label">
                <span id="timestamp">Timestamp</span>
              </dt>
              <dd data-testid="gratitude-entry-detail-timestamp-value">
                {gratitudeEntryEntity.timestamp ? (
                  <TextFormat value={gratitudeEntryEntity.timestamp} type="date" format={APP_DATE_FORMAT} />
                ) : null}
              </dd>
              <dt data-testid="gratitude-entry-detail-user-label">User</dt>
              <dd data-testid="gratitude-entry-detail-user-value">{gratitudeEntryEntity.user ? gratitudeEntryEntity.user.login : ''}</dd>
            </dl>
          ) : (
            <div data-testid="gratitude-entry-detail-not-found" className="alert alert-warning">
              <FontAwesomeIcon icon="exclamation-triangle" /> Gratitude entry not found.
            </div>
          )}
          <div className="d-flex justify-content-between" data-testid="gratitude-entry-detail-actions">
            <Button
              tag={Link}
              to="/gratitude-entry"
              replace
              color="info"
              data-cy="entityDetailsBackButton"
              data-testid="btn-back-gratitude-entry-detail"
            >
              <FontAwesomeIcon icon="arrow-left" /> <span className="d-none d-md-inline">Back</span>
            </Button>
            {gratitudeEntryEntity && (
              <Button
                tag={Link}
                to={`/gratitude-entry/${gratitudeEntryEntity.id}/edit`}
                replace
                color="primary"
                data-testid="btn-edit-gratitude-entry-detail"
              >
                <FontAwesomeIcon icon="pencil-alt" /> <span className="d-none d-md-inline">Edit</span>
              </Button>
            )}
          </div>
        </Col>
      </Row>
    </div>
  );
};

export default GratitudeEntryDetail;
