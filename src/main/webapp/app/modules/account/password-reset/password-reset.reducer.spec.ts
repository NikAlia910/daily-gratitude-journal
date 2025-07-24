import axios from 'axios';
import { configureStore } from '@reduxjs/toolkit';

import { handlePasswordResetInit, handlePasswordResetFinish, reset, PasswordResetSlice } from './password-reset.reducer';

// Mock axios
jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('Password reset reducer', () => {
  let store;

  beforeEach(() => {
    store = configureStore({
      reducer: {
        passwordReset: PasswordResetSlice.reducer,
      },
    });
    jest.clearAllMocks();
  });

  describe('Initial state', () => {
    it('should return initial state', () => {
      const expectedState = {
        loading: false,
        resetPasswordSuccess: false,
        resetPasswordFailure: false,
        successMessage: null,
      };

      expect(PasswordResetSlice.reducer(undefined, { type: '' })).toEqual(expectedState);
    });
  });

  describe('Reset action', () => {
    it('should reset state to initial values', () => {
      const modifiedState = {
        loading: true,
        resetPasswordSuccess: true,
        resetPasswordFailure: true,
        successMessage: 'Some message' as any,
      };

      const result = PasswordResetSlice.reducer(modifiedState, reset());

      expect(result).toEqual({
        loading: false,
        resetPasswordSuccess: false,
        resetPasswordFailure: false,
        successMessage: null,
      });
    });
  });

  describe('Password reset init async actions', () => {
    it('should set loading state when init is pending', () => {
      const initialState = {
        loading: false,
        resetPasswordSuccess: false,
        resetPasswordFailure: false,
        successMessage: null,
      };

      const action = { type: handlePasswordResetInit.pending.type };
      const result = PasswordResetSlice.reducer(initialState, action);

      expect(result.loading).toBe(true);
    });

    it('should handle successful password reset init', async () => {
      const email = 'test@example.com';
      mockedAxios.post.mockResolvedValue({ data: {} });

      await store.dispatch(handlePasswordResetInit(email));
      const state = store.getState().passwordReset;

      expect(state.loading).toBe(false);
      expect(state.resetPasswordSuccess).toBe(true);
      expect(state.resetPasswordFailure).toBe(false);
      expect(state.successMessage).toBe('Check your email for details on how to reset your password.');
    });

    it('should handle failed password reset init', async () => {
      const email = 'test@example.com';
      mockedAxios.post.mockRejectedValue({
        message: 'Email not found',
      });

      await store.dispatch(handlePasswordResetInit(email));
      const state = store.getState().passwordReset;

      expect(state.loading).toBe(false);
      expect(state.resetPasswordSuccess).toBe(false);
      expect(state.resetPasswordFailure).toBe(true);
      expect(state.successMessage).toBeNull();
    });
  });

  describe('Password reset finish async actions', () => {
    it('should set loading state when finish is pending', () => {
      const initialState = {
        loading: false,
        resetPasswordSuccess: false,
        resetPasswordFailure: false,
        successMessage: null,
      };

      const action = { type: handlePasswordResetFinish.pending.type };
      const result = PasswordResetSlice.reducer(initialState, action);

      expect(result.loading).toBe(true);
    });

    it('should handle successful password reset finish', async () => {
      const resetData = { key: 'reset-key', newPassword: 'newPassword123' };
      mockedAxios.post.mockResolvedValue({ data: {} });

      await store.dispatch(handlePasswordResetFinish(resetData));
      const state = store.getState().passwordReset;

      expect(state.loading).toBe(false);
      expect(state.resetPasswordSuccess).toBe(true);
      expect(state.resetPasswordFailure).toBe(false);
      expect(state.successMessage).toBe("Your password couldn't be reset. Remember a password request is only valid for 24 hours.");
    });

    it('should handle failed password reset finish', async () => {
      const resetData = { key: 'invalid-key', newPassword: 'newPassword123' };
      mockedAxios.post.mockRejectedValue({
        message: 'Invalid reset key',
      });

      await store.dispatch(handlePasswordResetFinish(resetData));
      const state = store.getState().passwordReset;

      expect(state.loading).toBe(false);
      expect(state.resetPasswordSuccess).toBe(false);
      expect(state.resetPasswordFailure).toBe(true);
      expect(state.successMessage).toBeNull();
    });
  });

  describe('Request headers', () => {
    it('should send init request with correct content-type', async () => {
      const email = 'test@example.com';
      mockedAxios.post.mockResolvedValue({ data: {} });

      await store.dispatch(handlePasswordResetInit(email));

      expect(mockedAxios.post).toHaveBeenCalledWith('api/account/reset-password/init', email, {
        headers: { 'Content-Type': 'text/plain' },
      });
    });
  });
});
