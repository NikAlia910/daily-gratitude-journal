import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { deleteEntity, getEntity } from './gratitude-entry.reducer';

export const GratitudeEntryDeleteDialog = () => {
  const dispatch = useAppDispatch();
  const pageLocation = useLocation();
  const navigate = useNavigate();
  const { id } = useParams<'id'>();

  const [loadModal, setLoadModal] = useState(false);

  useEffect(() => {
    dispatch(getEntity(id));
    setLoadModal(true);
  }, []);

  const gratitudeEntryEntity = useAppSelector(state => state.gratitudeEntry.entity);
  const updateSuccess = useAppSelector(state => state.gratitudeEntry.updateSuccess);
  const loading = useAppSelector(state => state.gratitudeEntry.loading);

  const handleClose = () => {
    navigate(`/gratitude-entry${pageLocation.search}`);
  };

  useEffect(() => {
    if (updateSuccess && loadModal) {
      handleClose();
      setLoadModal(false);
    }
  }, [updateSuccess]);

  const confirmDelete = () => {
    dispatch(deleteEntity(gratitudeEntryEntity.id));
  };

  return (
    <Modal isOpen toggle={handleClose} data-testid="gratitude-entry-delete-modal">
      <ModalHeader toggle={handleClose} data-cy="gratitudeEntryDeleteDialogHeading" data-testid="gratitude-entry-delete-modal-header">
        Confirm delete operation
      </ModalHeader>
      <ModalBody id="dailyGratitudeJournalApp.gratitudeEntry.delete.question" data-testid="gratitude-entry-delete-modal-body">
        {loading ? (
          <div data-testid="gratitude-entry-delete-loading" className="text-center p-3">
            <FontAwesomeIcon icon="spinner" spin /> Loading gratitude entry...
          </div>
        ) : gratitudeEntryEntity ? (
          <div data-testid="gratitude-entry-delete-content">
            <p>
              Are you sure you want to delete the gratitude entry for{' '}
              <strong data-testid="gratitude-entry-delete-date">
                {gratitudeEntryEntity.date ? new Date(gratitudeEntryEntity.date).toLocaleDateString() : 'Unknown Date'}
              </strong>
              ?
            </p>
            <div className="alert alert-warning" data-testid="gratitude-entry-delete-warning">
              <FontAwesomeIcon icon="exclamation-triangle" /> This action cannot be undone.
            </div>
            {gratitudeEntryEntity.entry && (
              <div data-testid="gratitude-entry-delete-preview">
                <strong>Entry preview:</strong>
                <p className="text-muted">{gratitudeEntryEntity.entry.substring(0, 100)}...</p>
              </div>
            )}
          </div>
        ) : (
          <div data-testid="gratitude-entry-delete-not-found" className="alert alert-danger">
            <FontAwesomeIcon icon="exclamation-triangle" /> Gratitude entry not found.
          </div>
        )}
      </ModalBody>
      <ModalFooter data-testid="gratitude-entry-delete-modal-footer">
        <Button color="secondary" onClick={handleClose} data-testid="btn-cancel-gratitude-entry-delete">
          <FontAwesomeIcon icon="ban" />
          &nbsp; Cancel
        </Button>
        <Button
          id="jhi-confirm-delete-gratitudeEntry"
          data-cy="entityConfirmDeleteButton"
          color="danger"
          onClick={confirmDelete}
          disabled={loading || !gratitudeEntryEntity}
          data-testid="btn-confirm-gratitude-entry-delete"
        >
          <FontAwesomeIcon icon="trash" />
          &nbsp; Delete
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default GratitudeEntryDeleteDialog;
