import React, { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { ValidatedField, ValidatedForm } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { convertDateTimeFromServer, convertDateTimeToServer, displayDefaultDateTime } from 'app/shared/util/date-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getUsers } from 'app/modules/administration/user-management/user-management.reducer';
import { Mood } from 'app/shared/model/enumerations/mood.model';
import { createEntity, getEntity, reset, updateEntity } from './gratitude-entry.reducer';

export const GratitudeEntryUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const users = useAppSelector(state => state.userManagement.users);
  const gratitudeEntryEntity = useAppSelector(state => state.gratitudeEntry.entity);
  const loading = useAppSelector(state => state.gratitudeEntry.loading);
  const updating = useAppSelector(state => state.gratitudeEntry.updating);
  const updateSuccess = useAppSelector(state => state.gratitudeEntry.updateSuccess);
  const moodValues = Object.keys(Mood);

  const handleClose = () => {
    navigate(`/gratitude-entry${location.search}`);
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(id));
    }

    dispatch(getUsers({}));
  }, []);

  useEffect(() => {
    if (updateSuccess) {
      handleClose();
    }
  }, [updateSuccess]);

  const saveEntity = values => {
    if (values.id !== undefined && typeof values.id !== 'number') {
      values.id = Number(values.id);
    }
    values.timestamp = convertDateTimeToServer(values.timestamp);

    const entity = {
      ...gratitudeEntryEntity,
      ...values,
      user: users.find(it => it.id.toString() === values.user?.toString()),
    };

    if (isNew) {
      dispatch(createEntity(entity));
    } else {
      dispatch(updateEntity(entity));
    }
  };

  const defaultValues = () =>
    isNew
      ? {
          timestamp: displayDefaultDateTime(),
        }
      : {
          mood: 'HAPPY',
          ...gratitudeEntryEntity,
          timestamp: convertDateTimeFromServer(gratitudeEntryEntity.timestamp),
          user: gratitudeEntryEntity?.user?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="dailyGratitudeJournalApp.gratitudeEntry.home.createOrEditLabel" data-cy="GratitudeEntryCreateUpdateHeading">
            Create or edit a Gratitude Entry
          </h2>
        </Col>
      </Row>
      <Row className="justify-content-center">
        <Col md="8">
          {loading ? (
            <p>Loading...</p>
          ) : (
            <ValidatedForm defaultValues={defaultValues()} onSubmit={saveEntity}>
              {!isNew ? (
                <ValidatedField name="id" required readOnly id="gratitude-entry-id" label="ID" validate={{ required: true }} />
              ) : null}
              <ValidatedField
                label="Date"
                id="gratitude-entry-date"
                name="date"
                data-cy="date"
                type="date"
                validate={{
                  required: { value: true, message: 'This field is required.' },
                }}
              />
              <ValidatedField
                label="Entry"
                id="gratitude-entry-entry"
                name="entry"
                data-cy="entry"
                type="textarea"
                validate={{
                  required: { value: true, message: 'This field is required.' },
                }}
              />
              <ValidatedField label="Mood" id="gratitude-entry-mood" name="mood" data-cy="mood" type="select">
                {moodValues.map(mood => (
                  <option value={mood} key={mood}>
                    {mood}
                  </option>
                ))}
              </ValidatedField>
              <ValidatedField
                label="Timestamp"
                id="gratitude-entry-timestamp"
                name="timestamp"
                data-cy="timestamp"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
                validate={{
                  required: { value: true, message: 'This field is required.' },
                }}
              />
              <ValidatedField id="gratitude-entry-user" name="user" data-cy="user" label="User" type="select">
                <option value="" key="0" />
                {users
                  ? users.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.login}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/gratitude-entry" replace color="info">
                <FontAwesomeIcon icon="arrow-left" />
                &nbsp;
                <span className="d-none d-md-inline">Back</span>
              </Button>
              &nbsp;
              <Button color="primary" id="save-entity" data-cy="entityCreateSaveButton" type="submit" disabled={updating}>
                <FontAwesomeIcon icon="save" />
                &nbsp; Save
              </Button>
            </ValidatedForm>
          )}
        </Col>
      </Row>
    </div>
  );
};

export default GratitudeEntryUpdate;
