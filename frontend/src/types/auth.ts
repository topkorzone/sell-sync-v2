export interface SignupRequest {
  email: string;
  password: string;
  companyName: string;
  businessNumber?: string;
  contactName?: string;
  contactPhone?: string;
}

export interface SignupResponse {
  userId: string;
  email: string;
  tenantId: string;
  message: string;
  emailConfirmationRequired: boolean;
}

export interface AuthUser {
  id: string;
  email: string;
  role: string;
  tenantId: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  expiresAt: number;
  user: AuthUser;
}
