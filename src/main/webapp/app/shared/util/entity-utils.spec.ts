import { cleanEntity, mapIdList, overrideSortStateWithQueryParams, overridePaginationStateWithQueryParams } from './entity-utils';

describe('Entity utils', () => {
  describe('cleanEntity', () => {
    it('should not remove fields with an id', () => {
      const entityA = {
        a: {
          id: 5,
        },
      };
      const entityB = {
        a: {
          id: '5',
        },
      };

      expect(cleanEntity({ ...entityA })).toEqual(entityA);
      expect(cleanEntity({ ...entityB })).toEqual(entityB);
    });

    it('should remove fields with an empty id', () => {
      const entity = {
        a: {
          id: '',
        },
      };

      expect(cleanEntity({ ...entity })).toEqual({});
    });

    it('should not remove fields that are not objects', () => {
      const entity = {
        a: '',
        b: 5,
        c: [],
        d: '5',
      };

      expect(cleanEntity({ ...entity })).toEqual(entity);
    });
  });

  describe('mapIdList', () => {
    it("should map ids no matter the element's type", () => {
      const ids = ['jhipster', '', 1, { key: 'value' }];

      expect(mapIdList(ids)).toEqual([{ id: 'jhipster' }, { id: 1 }, { id: { key: 'value' } }]);
    });

    it('should return an empty array', () => {
      const ids = [];

      expect(mapIdList(ids)).toEqual([]);
    });
  });

  describe('overrideSortStateWithQueryParams', () => {
    it('should override sort state with query parameters', () => {
      const paginationState = { sort: 'id', order: 'asc' };
      const locationSearch = '?sort=name,desc';

      const result = overrideSortStateWithQueryParams(paginationState, locationSearch);

      expect(result.sort).toBe('name');
      expect(result.order).toBe('desc');
    });

    it('should not modify state when no sort parameter present', () => {
      const paginationState = { sort: 'id', order: 'asc' };
      const locationSearch = '?page=1&size=20';

      const result = overrideSortStateWithQueryParams(paginationState, locationSearch);

      expect(result.sort).toBe('id');
      expect(result.order).toBe('asc');
    });

    it('should handle empty location search', () => {
      const paginationState = { sort: 'id', order: 'asc' };
      const locationSearch = '';

      const result = overrideSortStateWithQueryParams(paginationState, locationSearch);

      expect(result.sort).toBe('id');
      expect(result.order).toBe('asc');
    });
  });

  describe('overridePaginationStateWithQueryParams', () => {
    it('should override pagination state with query parameters', () => {
      const paginationState = { activePage: 1, itemsPerPage: 20, sort: 'id', order: 'asc' };
      const locationSearch = '?page=3&sort=name,desc';

      const result = overridePaginationStateWithQueryParams(paginationState, locationSearch);

      expect(result.activePage).toBe(3);
      expect(result.sort).toBe('name');
      expect(result.order).toBe('desc');
    });

    it('should handle only page parameter', () => {
      const paginationState = { activePage: 1, itemsPerPage: 20, sort: 'id', order: 'asc' };
      const locationSearch = '?page=5';

      const result = overridePaginationStateWithQueryParams(paginationState, locationSearch);

      expect(result.activePage).toBe(5);
      expect(result.sort).toBe('id');
      expect(result.order).toBe('asc');
    });

    it('should handle only sort parameter', () => {
      const paginationState = { activePage: 1, itemsPerPage: 20, sort: 'id', order: 'asc' };
      const locationSearch = '?sort=created,desc';

      const result = overridePaginationStateWithQueryParams(paginationState, locationSearch);

      expect(result.activePage).toBe(1);
      expect(result.sort).toBe('created');
      expect(result.order).toBe('desc');
    });

    it('should not modify state when no relevant parameters present', () => {
      const paginationState = { activePage: 1, itemsPerPage: 20, sort: 'id', order: 'asc' };
      const locationSearch = '?filter=active';

      const result = overridePaginationStateWithQueryParams(paginationState, locationSearch);

      expect(result.activePage).toBe(1);
      expect(result.sort).toBe('id');
      expect(result.order).toBe('asc');
    });
  });
});
