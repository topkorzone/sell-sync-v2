import api from './api';
import { getAccessToken, setTokens, clearTokens } from './token';
import type { SignupRequest, SignupResponse } from '@/types/auth';

const DEV_MOCK = import.meta.env.DEV && import.meta.env.VITE_MOCK_AUTH === 'true';

const MOCK_USER = {
  id: '00000000-0000-0000-0000-000000000000',
  email: 'dev@example.com',
  role: 'admin',
  tenantId: '00000000-0000-0000-0000-000000000000',
};

export async function signUp(request: SignupRequest): Promise<SignupResponse> {
  const { data } = await api.post('/api/v1/auth/signup', request);
  return data.data;
}

export async function signIn(email: string, password: string) {
  if (DEV_MOCK) {
    setTokens('mock-access-token', 'mock-refresh-token');
    return { ...MOCK_USER, accessToken: 'mock-access-token', refreshToken: 'mock-refresh-token' };
  }
  const { data } = await api.post('/api/v1/auth/login', { email, password });
  const { accessToken, refreshToken } = data.data;
  setTokens(accessToken, refreshToken);
  return data.data;
}

export async function signOut() {
  if (DEV_MOCK) {
    clearTokens();
    return;
  }
  try {
    await api.post('/api/v1/auth/logout');
  } finally {
    clearTokens();
  }
}

export async function getSession() {
  if (DEV_MOCK) return MOCK_USER;

  const token = getAccessToken();
  if (!token) return null;

  try {
    const { data } = await api.get('/api/v1/auth/me');
    return data.data;
  } catch {
    return null;
  }
}
