import loggerMiddleware from './logger-middleware';

// Mock console methods
const consoleGroupCollapsed = jest.spyOn(console, 'groupCollapsed').mockImplementation();
const consoleLog = jest.spyOn(console, 'log').mockImplementation();
const consoleGroupEnd = jest.spyOn(console, 'groupEnd').mockImplementation();

// Mock DEVELOPMENT constant
const originalDevelopment = (global as any).DEVELOPMENT;

describe('logger-middleware', () => {
  const next = jest.fn();
  const store = {};

  beforeEach(() => {
    jest.clearAllMocks();
    next.mockReturnValue('next-result');
  });

  afterEach(() => {
    (global as any).DEVELOPMENT = originalDevelopment;
  });

  describe('in development mode', () => {
    beforeEach(() => {
      (global as any).DEVELOPMENT = true;
    });

    it('should log action details without error', () => {
      const middleware = loggerMiddleware()(next);
      const action = {
        type: 'TEST_ACTION',
        payload: { data: 'test data' },
        meta: { requestId: '123' },
      };

      const result = middleware(action);

      expect(consoleGroupCollapsed).toHaveBeenCalledWith('TEST_ACTION');
      expect(consoleLog).toHaveBeenCalledWith('Payload:', { data: 'test data' });
      expect(consoleLog).toHaveBeenCalledWith('Meta:', { requestId: '123' });
      expect(consoleLog).not.toHaveBeenCalledWith('Error:', expect.anything());
      expect(consoleGroupEnd).toHaveBeenCalled();
      expect(next).toHaveBeenCalledWith(action);
      expect(result).toBe('next-result');
    });

    it('should log action details with error', () => {
      const middleware = loggerMiddleware()(next);
      const error = { message: 'Something went wrong' };
      const action = {
        type: 'ERROR_ACTION',
        payload: null,
        meta: { timestamp: Date.now() },
        error,
      };

      middleware(action);

      expect(consoleGroupCollapsed).toHaveBeenCalledWith('ERROR_ACTION');
      expect(consoleLog).toHaveBeenCalledWith('Payload:', null);
      expect(consoleLog).toHaveBeenCalledWith('Error:', error);
      expect(consoleLog).toHaveBeenCalledWith('Meta:', { timestamp: expect.any(Number) });
      expect(consoleGroupEnd).toHaveBeenCalled();
    });

    it('should handle actions with minimal properties', () => {
      const middleware = loggerMiddleware()(next);
      const action = { type: 'MINIMAL_ACTION' };

      middleware(action);

      expect(consoleGroupCollapsed).toHaveBeenCalledWith('MINIMAL_ACTION');
      expect(consoleLog).toHaveBeenCalledWith('Payload:', undefined);
      expect(consoleLog).toHaveBeenCalledWith('Meta:', undefined);
      expect(consoleGroupEnd).toHaveBeenCalled();
    });
  });

  describe('in production mode', () => {
    beforeEach(() => {
      (global as any).DEVELOPMENT = false;
    });

    it('should not log anything in production', () => {
      const middleware = loggerMiddleware()(next);
      const action = {
        type: 'PRODUCTION_ACTION',
        payload: { sensitive: 'data' },
        error: { message: 'Error in production' },
      };

      const result = middleware(action);

      expect(consoleGroupCollapsed).not.toHaveBeenCalled();
      expect(consoleLog).not.toHaveBeenCalled();
      expect(consoleGroupEnd).not.toHaveBeenCalled();
      expect(next).toHaveBeenCalledWith(action);
      expect(result).toBe('next-result');
    });
  });
});
