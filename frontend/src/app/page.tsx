'use client';

import React, { useEffect, useState } from 'react';
import { Row, Col, Card, Statistic, Table, Typography, Spin } from 'antd';
import {
  ShoppingCartOutlined,
  CarOutlined,
  ClockCircleOutlined,
  DollarOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import api from '@/lib/api';
import type { DashboardOverview, Order } from '@/types';

const { Title } = Typography;

const orderColumns: ColumnsType<Order> = [
  { title: '주문번호', dataIndex: 'marketplaceOrderId', key: 'orderId', width: 180 },
  { title: '마켓', dataIndex: 'marketplaceType', key: 'marketplace', width: 100 },
  { title: '수취인', dataIndex: 'receiverName', key: 'receiver', width: 100 },
  { title: '상태', dataIndex: 'status', key: 'status', width: 120 },
  {
    title: '주문금액',
    dataIndex: 'totalAmount',
    key: 'amount',
    width: 120,
    render: (v: number) => `${v?.toLocaleString()}원`,
  },
  { title: '주문일', dataIndex: 'orderedAt', key: 'orderedAt', width: 120 },
];

export default function DashboardPage() {
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const { data } = await api.get('/api/v1/dashboard/overview');
        setOverview(data.data);
      } catch {
        // Dashboard data unavailable — show empty state
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', paddingTop: 100 }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>
        대시보드
      </Title>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="오늘 주문"
              value={overview?.todayOrders ?? 0}
              prefix={<ShoppingCartOutlined />}
              suffix="건"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="오늘 발송"
              value={overview?.todayShipments ?? 0}
              prefix={<CarOutlined />}
              suffix="건"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="미처리 주문"
              value={overview?.pendingOrders ?? 0}
              prefix={<ClockCircleOutlined />}
              suffix="건"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="이번 달 매출"
              value={overview?.monthlyRevenue ?? 0}
              prefix={<DollarOutlined />}
              suffix="원"
              formatter={(value) => Number(value).toLocaleString()}
            />
          </Card>
        </Col>
      </Row>
      <Card title="최근 주문">
        <Table
          columns={orderColumns}
          dataSource={overview?.recentOrders ?? []}
          rowKey="id"
          pagination={false}
          size="small"
          locale={{ emptyText: '주문 데이터가 없습니다.' }}
        />
      </Card>
    </div>
  );
}
