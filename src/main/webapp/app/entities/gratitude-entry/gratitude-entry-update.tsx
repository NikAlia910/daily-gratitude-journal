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
    <div data-testid="gratitude-entry-form-container">
      <Row className="justify-content-center">
        <Col md="8">
          <h2
            id="dailyGratitudeJournalApp.gratitudeEntry.home.createOrEditLabel"
            data-cy="GratitudeEntryCreateUpdateHeading"
            data-testid="gratitude-entry-form-heading"
          >
            {isNew ? 'Create a new Gratitude Entry' : 'Edit Gratitude Entry'}
          </h2>
        </Col>
      </Row>
      <Row className="justify-content-center">
        <Col md="8">
          {loading ? (
            <div data-testid="gratitude-entry-form-loading" className="text-center p-3">
              <FontAwesomeIcon icon="spinner" spin /> Loading gratitude entry...
            </div>
          ) : (
            <ValidatedForm defaultValues={defaultValues()} onSubmit={saveEntity} data-testid="gratitude-entry-form">
              {!isNew ? (
                <ValidatedField
                  name="id"
                  required
                  readOnly
                  id="gratitude-entry-id"
                  label="ID"
                  validate={{ required: true }}
                  data-testid="input-gratitude-entry-id"
                />
              ) : null}
              <ValidatedField
                label="Date"
                id="gratitude-entry-date"
                name="date"
                data-cy="date"
                data-testid="input-gratitude-entry-date"
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
                data-testid="textarea-gratitude-entry-text"
                type="textarea"
                validate={{
                  required: { value: true, message: 'This field is required.' },
                  minLength: { value: 10, message: 'This field is required to be at least 10 characters.' },
                  maxLength: { value: 1000, message: 'This field cannot be longer than 1000 characters.' },
                }}
              />
              <ValidatedField
                label="Mood"
                id="gratitude-entry-mood"
                name="mood"
                data-cy="mood"
                type="select"
                data-testid="select-gratitude-entry-mood"
              >
                {moodValues.map(mood => (
                  <option value={mood} key={mood} data-testid={`option-mood-${mood.toLowerCase()}`}>
                    {mood}
                  </option>
                ))}
              </ValidatedField>
              <ValidatedField
                label="Timestamp"
                id="gratitude-entry-timestamp"
                name="timestamp"
                data-cy="timestamp"
                data-testid="input-gratitude-entry-timestamp"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
                validate={{
                  required: { value: true, message: 'This field is required.' },
                }}
              />
              <ValidatedField
                id="gratitude-entry-user"
                name="user"
                data-cy="user"
                label="User"
                type="select"
                data-testid="select-gratitude-entry-user"
              >
                <option value="" key="0" data-testid="option-user-empty" />
                {users
                  ? users.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id} data-testid={`option-user-${otherEntity.id}`}>
                        {otherEntity.login}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <div className="d-flex justify-content-between" data-testid="gratitude-entry-form-actions">
                <Button
                  tag={Link}
                  id="cancel-save"
                  data-cy="entityCreateCancelButton"
                  to="/gratitude-entry"
                  replace
                  color="info"
                  data-testid="btn-cancel-gratitude-entry"
                >
                  <FontAwesomeIcon icon="arrow-left" />
                  &nbsp;
                  <span className="d-none d-md-inline">Back</span>
                </Button>
                <Button
                  color="primary"
                  id="save-entity"
                  data-cy="entityCreateSaveButton"
                  type="submit"
                  disabled={updating}
                  data-testid="btn-save-gratitude-entry"
                >
                  <FontAwesomeIcon icon="save" />
                  &nbsp; {updating ? 'Saving...' : 'Save'}
                </Button>
              </div>
            </ValidatedForm>
          )}
        </Col>
      </Row>
    </div>
  );
};

export default GratitudeEntryUpdate;
