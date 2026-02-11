import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import api from '@/lib/api';

export interface Profile {
  userId: string;
  email: string;
  role: string;
  tenantId: string | null;
  companyName?: string;
  businessNumber?: string;
  contactName?: string;
  contactEmail?: string;
  contactPhone?: string;
}

interface AuthContextType {
  profile: Profile | null;
  loading: boolean;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchProfile = async () => {
    try {
      const { data } = await api.get('/api/v1/profile');
      setProfile(data.data);
    } catch (error) {
      console.error('Failed to fetch profile:', error);
      setProfile(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const refreshProfile = async () => {
    await fetchProfile();
  };

  return (
    <AuthContext.Provider value={{ profile, loading, refreshProfile }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
