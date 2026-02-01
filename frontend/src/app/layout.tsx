'use client';

import React, { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { Layout, ConfigProvider, Spin } from 'antd';
import koKR from 'antd/locale/ko_KR';
import Sidebar from '@/components/layout/Sidebar';
import Header from '@/components/layout/Header';
import { getSession } from '@/lib/auth';
import './globals.css';

const { Content } = Layout;

const publicPaths = ['/login'];

export default function RootLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const isPublicPage = publicPaths.includes(pathname);

  useEffect(() => {
    const checkAuth = async () => {
      try {
        const session = await getSession();
        if (!session && !isPublicPage) {
          router.push('/login');
        }
      } catch {
        if (!isPublicPage) {
          router.push('/login');
        }
      } finally {
        setLoading(false);
      }
    };
    checkAuth();
  }, [pathname, isPublicPage, router]);

  if (loading) {
    return (
      <html lang="ko">
        <body>
          <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
            <Spin size="large" />
          </div>
        </body>
      </html>
    );
  }

  if (isPublicPage) {
    return (
      <html lang="ko">
        <body>
          <ConfigProvider locale={koKR}>{children}</ConfigProvider>
        </body>
      </html>
    );
  }

  return (
    <html lang="ko">
      <body>
        <ConfigProvider locale={koKR}>
          <Layout hasSider>
            <Sidebar />
            <Layout style={{ marginLeft: 220 }}>
              <Header />
              <Content style={{ margin: 24, minHeight: 'calc(100vh - 112px)' }}>
                {children}
              </Content>
            </Layout>
          </Layout>
        </ConfigProvider>
      </body>
    </html>
  );
}
