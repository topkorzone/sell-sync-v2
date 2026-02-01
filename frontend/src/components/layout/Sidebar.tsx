'use client';

import React from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { Layout, Menu } from 'antd';
import {
  DashboardOutlined,
  ShoppingCartOutlined,
  CarOutlined,
  DollarOutlined,
  SettingOutlined,
} from '@ant-design/icons';

const { Sider } = Layout;

const menuItems = [
  { key: '/', icon: <DashboardOutlined />, label: '대시보드' },
  { key: '/orders', icon: <ShoppingCartOutlined />, label: '주문 관리' },
  { key: '/shipments', icon: <CarOutlined />, label: '배송 관리' },
  { key: '/settlements', icon: <DollarOutlined />, label: '정산 관리' },
  { key: '/settings', icon: <SettingOutlined />, label: '설정' },
];

export default function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();

  const selectedKey = menuItems
    .filter((item) => item.key !== '/')
    .find((item) => pathname.startsWith(item.key))?.key ?? '/';

  return (
    <Sider
      width={220}
      style={{
        overflow: 'auto',
        height: '100vh',
        position: 'fixed',
        left: 0,
        top: 0,
        bottom: 0,
      }}
    >
      <div
        style={{
          height: 64,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          borderBottom: '1px solid rgba(255,255,255,0.1)',
        }}
      >
        <h2 style={{ color: '#fff', margin: 0, fontSize: 18 }}>MarketHub</h2>
      </div>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[selectedKey]}
        items={menuItems}
        onClick={({ key }) => router.push(key)}
        style={{ marginTop: 8 }}
      />
    </Sider>
  );
}
