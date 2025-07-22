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
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="gratitudeEntryDetailsHeading">Gratitude Entry</h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">ID</span>
          </dt>
          <dd>{gratitudeEntryEntity.id}</dd>
          <dt>
            <span id="date">Date</span>
          </dt>
          <dd>
            {gratitudeEntryEntity.date ? <TextFormat value={gratitudeEntryEntity.date} type="date" format={APP_LOCAL_DATE_FORMAT} /> : null}
          </dd>
          <dt>
            <span id="entry">Entry</span>
          </dt>
          <dd>{gratitudeEntryEntity.entry}</dd>
          <dt>
            <span id="mood">Mood</span>
          </dt>
          <dd>{gratitudeEntryEntity.mood}</dd>
          <dt>
            <span id="timestamp">Timestamp</span>
          </dt>
          <dd>
            {gratitudeEntryEntity.timestamp ? (
              <TextFormat value={gratitudeEntryEntity.timestamp} type="date" format={APP_DATE_FORMAT} />
            ) : null}
          </dd>
          <dt>User</dt>
          <dd>{gratitudeEntryEntity.user ? gratitudeEntryEntity.user.login : ''}</dd>
        </dl>
        <Button tag={Link} to="/gratitude-entry" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" /> <span className="d-none d-md-inline">Back</span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/gratitude-entry/${gratitudeEntryEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" /> <span className="d-none d-md-inline">Edit</span>
        </Button>
      </Col>
    </Row>
  );
};

export default GratitudeEntryDetail;
