import errorMiddleware from './error-middleware';

// Mock console methods
const consoleSpy = jest.spyOn(console, 'error').mockImplementation();

// Mock DEVELOPMENT constant
const originalDevelopment = (global as any).DEVELOPMENT;

describe('error-middleware', () => {
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

    it('should pass through actions without errors', () => {
      const middleware = errorMiddleware(store)(next);
      const action = { type: 'TEST_ACTION', payload: 'test' };

      const result = middleware(action);

      expect(next).toHaveBeenCalledWith(action);
      expect(result).toBe('next-result');
      expect(consoleSpy).not.toHaveBeenCalled();
    });

    it('should log simple error messages', () => {
      const middleware = errorMiddleware(store)(next);
      const error = { message: 'Simple error message' };
      const action = { type: 'TEST_ACTION', error };

      middleware(action);

      expect(consoleSpy).toHaveBeenCalledWith('TEST_ACTION caught at middleware with reason: "Simple error message".');
      expect(next).toHaveBeenCalledWith(action);
    });

    it('should log error with response data', () => {
      const middleware = errorMiddleware(store)(next);
      const error = {
        message: 'Request failed',
        response: {
          data: { message: 'Server error message' },
        },
      };
      const action = { type: 'API_ERROR', error };

      middleware(action);

      expect(consoleSpy).toHaveBeenCalledWith('API_ERROR caught at middleware with reason: "Request failed".');
      expect(consoleSpy).toHaveBeenCalledWith('Actual cause: Server error message');
    });

    it('should format field errors correctly', () => {
      const middleware = errorMiddleware(store)(next);
      const error = {
        message: 'Validation failed',
        response: {
          data: {
            message: 'Validation error',
            fieldErrors: [
              { field: 'email', objectName: 'User', message: 'Invalid email format' },
              { field: 'password', objectName: 'User', message: 'Password too short' },
            ],
          },
        },
      };
      const action = { type: 'VALIDATION_ERROR', error };

      middleware(action);

      const expectedMessage =
        'Validation error\nfield: email,  Object: User, message: Invalid email format\n\nfield: password,  Object: User, message: Password too short\n';
      expect(consoleSpy).toHaveBeenCalledWith(`Actual cause: ${expectedMessage}`);
    });

    it('should handle errors without response data', () => {
      const middleware = errorMiddleware(store)(next);
      const error = {
        message: 'Network error',
        response: null,
      };
      const action = { type: 'NETWORK_ERROR', error };

      middleware(action);

      expect(consoleSpy).toHaveBeenCalledWith('NETWORK_ERROR caught at middleware with reason: "Network error".');
      expect(consoleSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('in production mode', () => {
    beforeEach(() => {
      (global as any).DEVELOPMENT = false;
    });

    it('should not log errors in production', () => {
      const middleware = errorMiddleware(store)(next);
      const error = { message: 'Some error' };
      const action = { type: 'TEST_ACTION', error };

      const result = middleware(action);

      expect(next).toHaveBeenCalledWith(action);
      expect(result).toBe('next-result');
      expect(consoleSpy).not.toHaveBeenCalled();
    });
  });
});
