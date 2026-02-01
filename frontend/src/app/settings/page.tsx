'use client';

import React from 'react';
import { Tabs, Card, Typography, Empty } from 'antd';

const { Title } = Typography;

const tabItems = [
  {
    key: 'marketplace',
    label: '마켓플레이스 연동',
    children: (
      <Card>
        <Empty description="마켓플레이스 연동 설정이 곧 추가됩니다." />
      </Card>
    ),
  },
  {
    key: 'courier',
    label: '택배사 설정',
    children: (
      <Card>
        <Empty description="택배사 설정이 곧 추가됩니다." />
      </Card>
    ),
  },
  {
    key: 'erp',
    label: 'ERP 연동',
    children: (
      <Card>
        <Empty description="ERP 연동 설정이 곧 추가됩니다." />
      </Card>
    ),
  },
];

export default function SettingsPage() {
  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>설정</Title>
      <Tabs items={tabItems} />
    </div>
  );
}
