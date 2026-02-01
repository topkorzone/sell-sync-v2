'use client';

import React from 'react';
import { Card, Typography, Empty } from 'antd';

const { Title } = Typography;

export default function SettlementsPage() {
  return (
    <div>
      <Title level={4}>정산 관리</Title>
      <Card>
        <Empty description="정산 관리 기능이 곧 추가됩니다." />
      </Card>
    </div>
  );
}
