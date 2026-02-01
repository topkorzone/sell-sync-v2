'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { Card, Table, Select, Input, Button, Space, Tag, Typography, message } from 'antd';
import { SyncOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import api from '@/lib/api';
import type { Order, MarketplaceType, OrderStatus, PageResponse } from '@/types';

const { Title } = Typography;

const statusColors: Record<OrderStatus, string> = {
  COLLECTED: 'blue',
  CONFIRMED: 'cyan',
  READY_TO_SHIP: 'gold',
  SHIPPING: 'orange',
  DELIVERED: 'green',
  CANCELLED: 'red',
  RETURNED: 'volcano',
  EXCHANGED: 'purple',
  PURCHASE_CONFIRMED: 'geekblue',
};

const statusLabels: Record<OrderStatus, string> = {
  COLLECTED: '수집완료',
  CONFIRMED: '확인',
  READY_TO_SHIP: '발송준비',
  SHIPPING: '배송중',
  DELIVERED: '배송완료',
  CANCELLED: '취소',
  RETURNED: '반품',
  EXCHANGED: '교환',
  PURCHASE_CONFIRMED: '구매확정',
};

const marketplaceLabels: Record<MarketplaceType, string> = {
  NAVER: '스마트스토어',
  COUPANG: '쿠팡',
  ELEVEN_ST: '11번가',
  GMARKET: 'G마켓',
  AUCTION: '옥션',
  WEMAKEPRICE: '위메프',
  TMON: '티몬',
};

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<OrderStatus | undefined>();
  const [marketplaceFilter, setMarketplaceFilter] = useState<MarketplaceType | undefined>();
  const [search, setSearch] = useState('');

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string | number> = { page, size: 20 };
      if (statusFilter) params.status = statusFilter;
      if (marketplaceFilter) params.marketplace = marketplaceFilter;
      if (search) params.search = search;
      const { data } = await api.get<{ data: PageResponse<Order> }>('/api/v1/orders', { params });
      setOrders(data.data?.content ?? []);
      setTotal(data.data?.totalElements ?? 0);
    } catch {
      message.error('주문 목록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter, marketplaceFilter, search]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  const handleSync = async () => {
    setSyncing(true);
    try {
      await api.post('/api/v1/orders/sync');
      message.success('동기화 요청이 완료되었습니다.');
      fetchOrders();
    } catch {
      message.error('동기화 요청에 실패했습니다.');
    } finally {
      setSyncing(false);
    }
  };

  const columns: ColumnsType<Order> = [
    { title: '주문번호', dataIndex: 'marketplaceOrderId', key: 'orderId', width: 180, ellipsis: true },
    {
      title: '마켓',
      dataIndex: 'marketplaceType',
      key: 'marketplace',
      width: 110,
      render: (v: MarketplaceType) => marketplaceLabels[v] || v,
    },
    { title: '수취인', dataIndex: 'receiverName', key: 'receiver', width: 100 },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (v: OrderStatus) => <Tag color={statusColors[v]}>{statusLabels[v] || v}</Tag>,
    },
    {
      title: '주문금액',
      dataIndex: 'totalAmount',
      key: 'amount',
      width: 120,
      align: 'right',
      render: (v: number) => `${v?.toLocaleString()}원`,
    },
    { title: '주문일', dataIndex: 'orderedAt', key: 'orderedAt', width: 120 },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>주문 관리</Title>
        <Button type="primary" icon={<SyncOutlined spin={syncing} />} loading={syncing} onClick={handleSync}>
          주문 동기화
        </Button>
      </div>
      <Card>
        <Space style={{ marginBottom: 16 }} wrap>
          <Select
            placeholder="마켓 선택"
            allowClear
            style={{ width: 150 }}
            value={marketplaceFilter}
            onChange={(v) => { setMarketplaceFilter(v); setPage(0); }}
            options={Object.entries(marketplaceLabels).map(([value, label]) => ({ value, label }))}
          />
          <Select
            placeholder="상태 선택"
            allowClear
            style={{ width: 130 }}
            value={statusFilter}
            onChange={(v) => { setStatusFilter(v); setPage(0); }}
            options={Object.entries(statusLabels).map(([value, label]) => ({ value, label }))}
          />
          <Input
            placeholder="주문번호 / 수취인 검색"
            prefix={<SearchOutlined />}
            style={{ width: 240 }}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onPressEnter={() => { setPage(0); fetchOrders(); }}
            allowClear
          />
        </Space>
        <Table
          columns={columns}
          dataSource={orders}
          rowKey="id"
          loading={loading}
          size="small"
          pagination={{
            current: page + 1,
            pageSize: 20,
            total,
            showSizeChanger: false,
            showTotal: (t) => `총 ${t}건`,
            onChange: (p) => setPage(p - 1),
          }}
          locale={{ emptyText: '주문 데이터가 없습니다.' }}
        />
      </Card>
    </div>
  );
}
