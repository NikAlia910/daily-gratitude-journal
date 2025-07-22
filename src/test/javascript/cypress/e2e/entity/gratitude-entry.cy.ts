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

describe('GratitudeEntry e2e test', () => {
  const gratitudeEntryPageUrl = '/gratitude-entry';
  const gratitudeEntryPageUrlPattern = new RegExp('/gratitude-entry(\\?.*)?$');
  const username = Cypress.env('E2E_USERNAME') ?? 'user';
  const password = Cypress.env('E2E_PASSWORD') ?? 'user';
  const gratitudeEntrySample = {
    date: '2025-07-21',
    entry: 'Li4vZmFrZS1kYXRhL2Jsb2IvaGlwc3Rlci50eHQ=',
    timestamp: '2025-07-22T00:57:04.413Z',
  };

  let gratitudeEntry;

  beforeEach(() => {
    cy.login(username, password);
  });

  beforeEach(() => {
    cy.intercept('GET', '/api/gratitude-entries+(?*|)').as('entitiesRequest');
    cy.intercept('POST', '/api/gratitude-entries').as('postEntityRequest');
    cy.intercept('DELETE', '/api/gratitude-entries/*').as('deleteEntityRequest');
  });

  afterEach(() => {
    if (gratitudeEntry) {
      cy.authenticatedRequest({
        method: 'DELETE',
        url: `/api/gratitude-entries/${gratitudeEntry.id}`,
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
        cy.authenticatedRequest({
          method: 'POST',
          url: '/api/gratitude-entries',
          body: gratitudeEntrySample,
        }).then(({ body }) => {
          gratitudeEntry = body;

          cy.intercept(
            {
              method: 'GET',
              url: '/api/gratitude-entries+(?*|)',
              times: 1,
            },
            {
              statusCode: 200,
              headers: {
                link: '<http://localhost/api/gratitude-entries?page=0&size=20>; rel="last",<http://localhost/api/gratitude-entries?page=0&size=20>; rel="first"',
              },
              body: [gratitudeEntry],
            },
          ).as('entitiesRequestInternal');
        });

        cy.visit(gratitudeEntryPageUrl);

        cy.wait('@entitiesRequestInternal');
      });

      it('detail button click should load details GratitudeEntry page', () => {
        cy.get(entityDetailsButtonSelector).first().click();
        cy.getEntityDetailsHeading('gratitudeEntry');
        cy.get(entityDetailsBackButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.url().should('match', gratitudeEntryPageUrlPattern);
      });

      it('edit button click should load edit GratitudeEntry page and go back', () => {
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
        cy.get(entityEditButtonSelector).first().click();
        cy.getEntityCreateUpdateHeading('GratitudeEntry');
        cy.get(entityCreateSaveButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.url().should('match', gratitudeEntryPageUrlPattern);
      });

      it('last delete button click should delete instance of GratitudeEntry', () => {
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
      cy.get(`[data-cy="date"]`).type('2025-07-22');
      cy.get(`[data-cy="date"]`).blur();
      cy.get(`[data-cy="date"]`).should('have.value', '2025-07-22');

      cy.get(`[data-cy="entry"]`).type('../fake-data/blob/hipster.txt');
      cy.get(`[data-cy="entry"]`).invoke('val').should('match', new RegExp('../fake-data/blob/hipster.txt'));

      cy.get(`[data-cy="mood"]`).select('REFLECTIVE');

      cy.get(`[data-cy="timestamp"]`).type('2025-07-21T08:33');
      cy.get(`[data-cy="timestamp"]`).blur();
      cy.get(`[data-cy="timestamp"]`).should('have.value', '2025-07-21T08:33');

      cy.get(entityCreateSaveButtonSelector).click();

      cy.wait('@postEntityRequest').then(({ response }) => {
        expect(response?.statusCode).to.equal(201);
        gratitudeEntry = response.body;
      });
      cy.wait('@entitiesRequest').then(({ response }) => {
        expect(response?.statusCode).to.equal(200);
      });
      cy.url().should('match', gratitudeEntryPageUrlPattern);
    });
  });
});
