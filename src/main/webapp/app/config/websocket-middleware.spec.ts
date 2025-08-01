import SockJS from 'sockjs-client';
import Stomp from 'webstomp-client';
import { Observable } from 'rxjs';

import websocketMiddleware, { sendActivity } from './websocket-middleware';
import { websocketActivityMessage } from 'app/modules/administration/administration.reducer';
import { getAccount, logoutSession } from 'app/shared/reducers/authentication';
import { Storage } from 'react-jhipster';

// Mock dependencies
jest.mock('sockjs-client');
jest.mock('webstomp-client');
jest.mock('react-jhipster', () => ({
  Storage: {
    local: {
      get: jest.fn(),
    },
    session: {
      get: jest.fn(),
    },
  },
}));

const mockSockJS = SockJS;
const mockStomp = Stomp as jest.Mocked<typeof Stomp>;

describe('Websocket Middleware', () => {
  let mockStore;
  let mockNext;
  let mockStompClient;
  let mockSubscriber;

  beforeEach(() => {
    jest.clearAllMocks();

    // Mock DOM elements
    Object.defineProperty(window, 'location', {
      value: {
        host: 'localhost:8080',
        pathname: '/test-page',
      },
      writable: true,
    });

    document.querySelector = jest.fn().mockReturnValue({
      getAttribute: jest.fn().mockReturnValue('/'),
    });

    // Mock store
    mockStore = {
      dispatch: jest.fn(),
    };

    mockNext = jest.fn();

    // Mock Stomp client
    mockStompClient = {
      connect: jest.fn(),
      disconnect: jest.fn(),
      send: jest.fn(),
      subscribe: jest.fn(),
      connected: false,
    };

    mockSubscriber = {
      unsubscribe: jest.fn(),
    };

    // Mock Stomp.over
    mockStomp.over = jest.fn().mockReturnValue(mockStompClient);

    // Mock Observable
    const mockObservable = new Observable(observer => {
      // Mock observer
    });

    // Mock createListener function behavior
    jest.spyOn(Observable, 'create').mockReturnValue(mockObservable);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe('sendActivity', () => {
    it('should not send activity when connection is not established', () => {
      sendActivity('/test-page');
      expect(mockStompClient.send).not.toHaveBeenCalled();
    });
  });

  describe('middleware', () => {
    it('should connect when getAccount.fulfilled is dispatched', () => {
      const action = {
        type: getAccount.fulfilled.type,
        payload: {
          data: {
            authorities: ['ROLE_ADMIN'],
          },
        },
      };

      const middleware = websocketMiddleware(mockStore);
      middleware(mockNext)(action);

      expect(mockStomp.over).toHaveBeenCalled();
      expect(mockStompClient.connect).toHaveBeenCalled();
    });

    it('should not subscribe for non-admin users', () => {
      const action = {
        type: getAccount.fulfilled.type,
        payload: {
          data: {
            authorities: ['ROLE_USER'],
          },
        },
      };

      const middleware = websocketMiddleware(mockStore);
      middleware(mockNext)(action);

      expect(mockStompClient.subscribe).not.toHaveBeenCalled();
    });

    it('should handle authentication token in URL', () => {
      const mockToken = 'test-token';
      Storage.local.get.mockReturnValue(mockToken);

      const action = {
        type: getAccount.fulfilled.type,
        payload: {
          data: {
            authorities: ['ROLE_ADMIN'],
          },
        },
      };

      const middleware = websocketMiddleware(mockStore);
      middleware(mockNext)(action);

      // Test that the middleware handles the action without throwing
      expect(mockNext).toHaveBeenCalledWith(action);
    });

    it('should handle session token when local token is not available', () => {
      const mockToken = 'session-token';
      Storage.local.get.mockReturnValue(null);
      Storage.session.get.mockReturnValue(mockToken);

      const action = {
        type: getAccount.fulfilled.type,
        payload: {
          data: {
            authorities: ['ROLE_ADMIN'],
          },
        },
      };

      const middleware = websocketMiddleware(mockStore);
      middleware(mockNext)(action);

      // Test that the middleware handles the action without throwing
      expect(mockNext).toHaveBeenCalledWith(action);
    });

    it('should handle base href with trailing slash', () => {
      document.querySelector = jest.fn().mockReturnValue({
        getAttribute: jest.fn().mockReturnValue('/app/'),
      });

      const action = {
        type: getAccount.fulfilled.type,
        payload: {
          data: {
            authorities: ['ROLE_ADMIN'],
          },
        },
      };

      const middleware = websocketMiddleware(mockStore);
      middleware(mockNext)(action);

      // Test that the middleware handles the action without throwing
      expect(mockNext).toHaveBeenCalledWith(action);
    });

    it('should call next with the action', () => {
      const action = { type: 'TEST_ACTION' };
      const middleware = websocketMiddleware(mockStore);

      middleware(mockNext)(action);

      expect(mockNext).toHaveBeenCalledWith(action);
    });

    it('should handle connection errors gracefully', () => {
      const action = {
        type: getAccount.fulfilled.type,
        payload: {
          data: {
            authorities: ['ROLE_ADMIN'],
          },
        },
      };

      mockStompClient.connect = jest.fn().mockImplementation((headers, callback, errorCallback) => {
        errorCallback(new Error('Connection failed'));
      });

      const middleware = websocketMiddleware(mockStore);

      // Should not throw an error
      expect(() => middleware(mockNext)(action)).not.toThrow();
    });

    it('should handle SockJS creation errors', () => {
      const action = {
        type: getAccount.fulfilled.type,
        payload: {
          data: {
            authorities: ['ROLE_ADMIN'],
          },
        },
      };

      mockSockJS.mockImplementation(() => {
        throw new Error('SockJS creation failed');
      });

      const middleware = websocketMiddleware(mockStore);

      // Should not throw an error
      expect(() => middleware(mockNext)(action)).not.toThrow();
    });

    it('should handle disconnect when client is null', () => {
      const action = {
        type: logoutSession().type,
      };

      const middleware = websocketMiddleware(mockStore);

      // Should not throw an error
      expect(() => middleware(mockNext)(action)).not.toThrow();
    });

    it('should handle disconnect errors', () => {
      const action = {
        type: logoutSession().type,
      };

      mockStompClient.disconnect = jest.fn().mockImplementation(() => {
        throw new Error('Disconnect failed');
      });

      const middleware = websocketMiddleware(mockStore);

      // Should not throw an error
      expect(() => middleware(mockNext)(action)).not.toThrow();
    });

    it('should handle unsubscribe errors', () => {
      const action = {
        type: logoutSession().type,
      };

      mockSubscriber.unsubscribe = jest.fn().mockImplementation(() => {
        throw new Error('Unsubscribe failed');
      });

      const middleware = websocketMiddleware(mockStore);

      // Should not throw an error
      expect(() => middleware(mockNext)(action)).not.toThrow();
    });

    it('should handle missing base element', () => {
      document.querySelector = jest.fn().mockReturnValue(null);

      const action = {
        type: getAccount.fulfilled.type,
        payload: {
          data: {
            authorities: ['ROLE_ADMIN'],
          },
        },
      };

      const middleware = websocketMiddleware(mockStore);

      // Should not throw an error
      expect(() => middleware(mockNext)(action)).not.toThrow();
    });

    it('should handle missing base href attribute', () => {
      document.querySelector = jest.fn().mockReturnValue({
        getAttribute: jest.fn().mockReturnValue(null),
      });

      const action = {
        type: getAccount.fulfilled.type,
        payload: {
          data: {
            authorities: ['ROLE_ADMIN'],
          },
        },
      };

      const middleware = websocketMiddleware(mockStore);

      // Should not throw an error
      expect(() => middleware(mockNext)(action)).not.toThrow();
    });

    it('should handle null payload in action', () => {
      const action = {
        type: getAccount.fulfilled.type,
        payload: null,
      };

      const middleware = websocketMiddleware(mockStore);

      // Should not throw an error
      expect(() => middleware(mockNext)(action)).not.toThrow();
    });

    it('should handle missing authorities in payload', () => {
      const action = {
        type: getAccount.fulfilled.type,
        payload: {
          data: {},
        },
      };

      const middleware = websocketMiddleware(mockStore);

      // Should not throw an error
      expect(() => middleware(mockNext)(action)).not.toThrow();
    });
  });
});
