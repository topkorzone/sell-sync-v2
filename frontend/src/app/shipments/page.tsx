'use client';

import React from 'react';
import { Card, Typography, Empty } from 'antd';

const { Title } = Typography;

export default function ShipmentsPage() {
  return (
    <div>
      <Title level={4}>배송 관리</Title>
      <Card>
        <Empty description="배송 관리 기능이 곧 추가됩니다." />
      </Card>
    </div>
  );
}
