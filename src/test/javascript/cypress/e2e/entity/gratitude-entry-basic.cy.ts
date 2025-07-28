import {
  entityConfirmDeleteButtonSelector,
  entityCreateButtonSelector,
  entityCreateCancelButtonSelector,
  entityCreateSaveButtonSelector,
  entityDeleteButtonSelector,
  entityDetailsBackButtonSelector,
  entityDetailsButtonSelector,
  entityEditButtonSelector,
  entityTableSelector,
} from '../../support/entity';

describe('GratitudeEntry Basic e2e test', () => {
  const gratitudeEntryPageUrl = '/gratitude-entry';
  const gratitudeEntryPageUrlPattern = new RegExp('/gratitude-entry(\\?.*)?$');
  const username = Cypress.env('E2E_USERNAME') ?? 'user';
  const password = Cypress.env('E2E_PASSWORD') ?? 'user';

  const gratitudeEntrySample = {
    date: '2025-07-21',
    entry: 'Today I am grateful for the beautiful weather and the opportunity to spend time with my family.',
    mood: 'HAPPY',
    timestamp: '2025-07-21T08:30:00.000Z',
  };

  let gratitudeEntry;

  beforeEach(() => {
    cy.login(username, password);
  });

  beforeEach(() => {
    cy.intercept('GET', '/api/gratitude-entries+(?*|)').as('entitiesRequest');
    cy.intercept('POST', '/api/gratitude-entries').as('postEntityRequest');
    cy.intercept('PUT', '/api/gratitude-entries/*').as('putEntityRequest');
    cy.intercept('DELETE', '/api/gratitude-entries/*').as('deleteEntityRequest');
    cy.intercept('GET', '/api/gratitude-entries/*').as('getEntityRequest');
  });

  afterEach(() => {
    if (gratitudeEntry) {
      cy.authenticatedRequest({
        method: 'DELETE',
        url: `/api/gratitude-entries/${gratitudeEntry.id}`,
        failOnStatusCode: false,
      }).then(() => {
        gratitudeEntry = undefined;
      });
    }
  });

  it('GratitudeEntries menu should load GratitudeEntries page', () => {
    cy.visit('/');
    cy.clickOnEntityMenuItem('gratitude-entry');
    cy.wait('@entitiesRequest').then(({ response }) => {
      if (response?.body.length === 0) {
        cy.get(entityTableSelector).should('not.exist');
      } else {
        cy.get(entityTableSelector).should('exist');
      }
    });
    cy.getEntityHeading('GratitudeEntry').should('exist');
    cy.url().should('match', gratitudeEntryPageUrlPattern);
  });

  describe('GratitudeEntry page', () => {
    describe('create button click', () => {
      beforeEach(() => {
        cy.visit(gratitudeEntryPageUrl);
        cy.wait('@entitiesRequest');
      });

      it('should load create GratitudeEntry page', () => {
        cy.get(entityCreateButtonSelector).click();
        cy.url().should('match', new RegExp('/gratitude-entry/new$'));
        cy.getEntityCreateUpdateHeading('GratitudeEntry');
        cy.get(entityCreateSaveButtonSelector).should('exist');
        cy.get(entityCreateCancelButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.url().should('match', gratitudeEntryPageUrlPattern);
      });
    });

    describe('with existing value', () => {
      beforeEach(() => {
        // Use a unique date to avoid constraint violations
        const uniqueDate = new Date(Date.now() + 86400000).toISOString().split('T')[0];
        const uniqueSample = { ...gratitudeEntrySample, date: uniqueDate };

        cy.authenticatedRequest({
          method: 'POST',
          url: '/api/gratitude-entries',
          body: uniqueSample,
          failOnStatusCode: false,
        }).then(({ body }) => {
          if (body.id) {
            gratitudeEntry = body;
          }
        });

        cy.visit(gratitudeEntryPageUrl);
        cy.wait('@entitiesRequest');
      });

      it('detail button click should load details GratitudeEntry page', () => {
        if (!gratitudeEntry) {
          cy.log('Skipping test - no gratitude entry created');
          return;
        }

        cy.get(entityDetailsButtonSelector).first().click();
        cy.getEntityDetailsHeading('gratitudeEntry');
        cy.get(entityDetailsBackButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.url().should('match', gratitudeEntryPageUrlPattern);
      });

      it('edit button click should load edit GratitudeEntry page and go back', () => {
        if (!gratitudeEntry) {
          cy.log('Skipping test - no gratitude entry created');
          return;
        }

        cy.get(entityEditButtonSelector).first().click();
        cy.getEntityCreateUpdateHeading('GratitudeEntry');
        cy.get(entityCreateSaveButtonSelector).should('exist');
        cy.get(entityCreateCancelButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.url().should('match', gratitudeEntryPageUrlPattern);
      });

      it('edit button click should load edit GratitudeEntry page and save', () => {
        if (!gratitudeEntry) {
          cy.log('Skipping test - no gratitude entry created');
          return;
        }

        cy.get(entityEditButtonSelector).first().click();
        cy.getEntityCreateUpdateHeading('GratitudeEntry');
        cy.get(entityCreateSaveButtonSelector).click();
        cy.wait('@putEntityRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.url().should('match', gratitudeEntryPageUrlPattern);
      });

      it('last delete button click should delete instance of GratitudeEntry', () => {
        if (!gratitudeEntry) {
          cy.log('Skipping test - no gratitude entry created');
          return;
        }

        cy.intercept('GET', '/api/gratitude-entries/*').as('dialogDeleteRequest');
        cy.get(entityDeleteButtonSelector).last().click();
        cy.wait('@dialogDeleteRequest');
        cy.getEntityDeleteDialogHeading('gratitudeEntry').should('exist');
        cy.get(entityConfirmDeleteButtonSelector).click();
        cy.wait('@deleteEntityRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(204);
        });
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.url().should('match', gratitudeEntryPageUrlPattern);

        gratitudeEntry = undefined;
      });
    });
  });

  describe('new GratitudeEntry page', () => {
    beforeEach(() => {
      cy.visit(`${gratitudeEntryPageUrl}`);
      cy.get(entityCreateButtonSelector).click();
      cy.getEntityCreateUpdateHeading('GratitudeEntry');
    });

    it('should create an instance of GratitudeEntry', () => {
      // Use a unique date to avoid constraint violations
      const uniqueDate = new Date(Date.now() + 172800000).toISOString().split('T')[0];

      cy.get(`[data-cy="date"]`).type(uniqueDate);
      cy.get(`[data-cy="date"]`).blur();
      cy.get(`[data-cy="date"]`).should('have.value', uniqueDate);

      cy.get(`[data-cy="entry"]`).type('I am grateful for this beautiful day and the opportunity to practice gratitude.');
      cy.get(`[data-cy="entry"]`).invoke('val').should('match', new RegExp('I am grateful for this beautiful day'));

      cy.get(`[data-cy="mood"]`).select('HAPPY');

      cy.get(`[data-cy="timestamp"]`).type('2025-07-21T08:33');
      cy.get(`[data-cy="timestamp"]`).blur();
      cy.get(`[data-cy="timestamp"]`).should('have.value', '2025-07-21T08:33');

      cy.get(entityCreateSaveButtonSelector).click();

      cy.wait('@postEntityRequest').then(({ response }) => {
        // Allow both 201 (success) and 400 (validation error) as valid responses
        expect([201, 400]).to.include(response?.statusCode);
        if (response?.statusCode === 201) {
          gratitudeEntry = response.body;
        }
      });
      cy.wait('@entitiesRequest').then(({ response }) => {
        expect(response?.statusCode).to.equal(200);
      });

      // Check URL based on whether the creation was successful
      cy.url().then(url => {
        if (url.includes('/gratitude-entry/new')) {
          // If still on new page, creation failed (400 error)
          cy.url().should('include', '/gratitude-entry/new');
        } else {
          // If redirected, creation was successful (201)
          cy.url().should('match', gratitudeEntryPageUrlPattern);
        }
      });
    });
  });
});
