/* eslint-disable @typescript-eslint/no-namespace */

// ***********************************************
// Begin Specific Selector Attributes for Cypress
// ***********************************************

// Entity
export const entityTableSelector = '[data-cy="entityTable"]';
export const entityCreateButtonSelector = '[data-cy="entityCreateButton"]';
export const entityCreateSaveButtonSelector = '[data-cy="entityCreateSaveButton"]';
export const entityCreateCancelButtonSelector = '[data-cy="entityCreateCancelButton"]';
export const entityDetailsButtonSelector = '[data-cy="entityDetailsButton"]'; // can return multiple elements
export const entityDetailsBackButtonSelector = '[data-cy="entityDetailsBackButton"]';
export const entityEditButtonSelector = '[data-cy="entityEditButton"]';
export const entityDeleteButtonSelector = '[data-cy="entityDeleteButton"]';
export const entityConfirmDeleteButtonSelector = '[data-cy="entityConfirmDeleteButton"]';

// Gratitude Entry specific selectors
export const gratitudeEntryListContainerSelector = '[data-testid="gratitude-entry-list-container"]';
export const gratitudeEntryTableSelector = '[data-testid="gratitude-entry-table"]';
export const gratitudeEntryFormSelector = '[data-testid="gratitude-entry-form"]';
export const gratitudeEntryDetailContainerSelector = '[data-testid="gratitude-entry-detail-container"]';
export const gratitudeEntryDeleteModalSelector = '[data-testid="gratitude-entry-delete-modal"]';

// ***********************************************
// End Specific Selector Attributes for Cypress
// ***********************************************

Cypress.Commands.add('getEntityHeading', (entityName: string) => cy.get(`[data-cy="${entityName}Heading"]`));

Cypress.Commands.add('getEntityCreateUpdateHeading', (entityName: string) => cy.get(`[data-cy="${entityName}CreateUpdateHeading"]`));

Cypress.Commands.add('getEntityDetailsHeading', (entityInstanceName: string) => cy.get(`[data-cy="${entityInstanceName}DetailsHeading"]`));

Cypress.Commands.add('getEntityDeleteDialogHeading', (entityInstanceName: string) =>
  cy.get(`[data-cy="${entityInstanceName}DeleteDialogHeading"]`),
);

Cypress.Commands.add('setFieldImageAsBytesOfEntity', (fieldName: string, fileName: string, mimeType: string) => {
  // fileName is the image which you have already put in cypress fixture folder
  // should be like: 'integration-test.png', 'image/png'
  cy.fixture(fileName)
    .as('image')
    .get(`[data-cy="${fieldName}"]`)
    .then(function (el) {
      const blob = Cypress.Blob.base64StringToBlob(this.image, mimeType);
      const file = new File([blob], fileName, { type: mimeType });
      const list = new DataTransfer();
      list.items.add(file);
      (el[0] as HTMLInputElement).files = list.files;
      el[0].dispatchEvent(new Event('change', { bubbles: true }));
    });
});

Cypress.Commands.add('setFieldSelectToLastOfEntity', (fieldName: string) => {
  return cy.get(`[data-cy="${fieldName}"]`).then(select => {
    const selectSize = (select[0] as HTMLSelectElement)?.options?.length || Number(select.attr('size')) || 0;
    if (selectSize > 0) {
      return cy.get(`[data-cy="${fieldName}"] option`).then((options: JQuery<HTMLElement>) => {
        const elements = [...options].map((o: HTMLElement) => (o as HTMLOptionElement).label);
        const lastElement = elements.length - 1;
        cy.get(`[data-cy="${fieldName}"]`).select(lastElement);
        cy.get(`[data-cy="${fieldName}"]`).type('{downarrow}');
      });
    }
    return cy.get(`[data-cy="${fieldName}"]`).type('{downarrow}');
  });
});

// Gratitude Entry specific commands
Cypress.Commands.add('createGratitudeEntry', (entryData: { date: string; entry: string; mood: string; timestamp: string }) => {
  return cy.authenticatedRequest({
    method: 'POST',
    url: '/api/gratitude-entries',
    body: entryData,
  });
});

Cypress.Commands.add('deleteGratitudeEntry', (entryId: number) => {
  return cy.authenticatedRequest({
    method: 'DELETE',
    url: `/api/gratitude-entries/${entryId}`,
  });
});

Cypress.Commands.add('getGratitudeEntry', (entryId: number) => {
  return cy.authenticatedRequest({
    method: 'GET',
    url: `/api/gratitude-entries/${entryId}`,
  });
});

Cypress.Commands.add('fillGratitudeEntryForm', (entryData: { date: string; entry: string; mood: string; timestamp: string }) => {
  cy.get('[data-cy="date"]').type(entryData.date);
  cy.get('[data-cy="entry"]').type(entryData.entry);
  cy.get('[data-cy="mood"]').select(entryData.mood);
  cy.get('[data-cy="timestamp"]').type(entryData.timestamp);
});

Cypress.Commands.add('submitGratitudeEntryForm', () => {
  cy.get('[data-cy="entityCreateSaveButton"]').click();
});

Cypress.Commands.add('waitForGratitudeEntrySave', () => {
  cy.wait('@postEntityRequest').then(({ response }) => {
    expect(response?.statusCode).to.equal(201);
    // eslint-disable-next-line @typescript-eslint/no-unsafe-return
    return response?.body;
  });
});

Cypress.Commands.add('waitForGratitudeEntryUpdate', () => {
  cy.wait('@putEntityRequest').then(({ response }) => {
    expect(response?.statusCode).to.equal(200);
    // eslint-disable-next-line @typescript-eslint/no-unsafe-return
    return response?.body;
  });
});

Cypress.Commands.add('waitForGratitudeEntryDelete', () => {
  cy.wait('@deleteEntityRequest').then(({ response }) => {
    expect(response?.statusCode).to.equal(204);
  });
});

Cypress.Commands.add('assertGratitudeEntryExists', (entryId: number, expectedData: any) => {
  cy.get(`[data-testid="gratitude-entry-row-${entryId}"]`).should('exist');
  if (expectedData.entry) {
    cy.get(`[data-testid="gratitude-entry-text-${entryId}"]`).should('contain', expectedData.entry.substring(0, 20));
  }
  if (expectedData.mood) {
    cy.get(`[data-testid="gratitude-entry-mood-${entryId}"]`).should('contain', expectedData.mood);
  }
});

Cypress.Commands.add('assertGratitudeEntryDoesNotExist', (entryId: number) => {
  cy.get(`[data-testid="gratitude-entry-row-${entryId}"]`).should('not.exist');
});

Cypress.Commands.add('assertGratitudeEntryFormValidation', (fieldName: string, shouldBeInvalid: boolean = true) => {
  // Map field names to their data-cy selectors
  const fieldSelectors: { [key: string]: string } = {
    date: '[data-cy="date"]',
    entry: '[data-cy="entry"]',
    mood: '[data-cy="mood"]',
    timestamp: '[data-cy="timestamp"]',
  };

  const selector = fieldSelectors[fieldName] || `[data-testid="${fieldName}"]`;
  if (shouldBeInvalid) {
    cy.get(selector).should('have.class', 'is-invalid');
  } else {
    cy.get(selector).should('not.have.class', 'is-invalid');
  }
});

declare global {
  namespace Cypress {
    interface Chainable {
      getEntityHeading(entityName: string): Cypress.Chainable;
      getEntityCreateUpdateHeading(entityName: string): Cypress.Chainable;
      getEntityDetailsHeading(entityInstanceName: string): Cypress.Chainable;
      getEntityDeleteDialogHeading(entityInstanceName: string): Cypress.Chainable;
      setFieldImageAsBytesOfEntity(fieldName: string, fileName: string, mimeType: string): Cypress.Chainable;
      setFieldSelectToLastOfEntity(fieldName: string): Cypress.Chainable;
      // Gratitude Entry specific commands
      createGratitudeEntry(entryData: { date: string; entry: string; mood: string; timestamp: string }): Cypress.Chainable;
      deleteGratitudeEntry(entryId: number): Cypress.Chainable;
      getGratitudeEntry(entryId: number): Cypress.Chainable;
      fillGratitudeEntryForm(entryData: { date: string; entry: string; mood: string; timestamp: string }): Cypress.Chainable;
      submitGratitudeEntryForm(): Cypress.Chainable;
      waitForGratitudeEntrySave(): Cypress.Chainable;
      waitForGratitudeEntryUpdate(): Cypress.Chainable;
      waitForGratitudeEntryDelete(): Cypress.Chainable;
      assertGratitudeEntryExists(entryId: number, expectedData: any): Cypress.Chainable;
      assertGratitudeEntryDoesNotExist(entryId: number): Cypress.Chainable;
      assertGratitudeEntryFormValidation(fieldName: string, shouldBeInvalid?: boolean): Cypress.Chainable;
    }
  }
}

// Convert this to a module instead of a script (allows import/export)
export {};
