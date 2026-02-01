'use client';

import React from 'react';
import { Typography } from 'antd';

const { Title } = Typography;

interface PageContainerProps {
  title: string;
  children: React.ReactNode;
}

export default function PageContainer({ title, children }: PageContainerProps) {
  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>
        {title}
      </Title>
      {children}
    </div>
  );
}
