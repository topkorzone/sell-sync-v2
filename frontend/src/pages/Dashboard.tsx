import { useEffect, useState } from "react";
import {
  ShoppingCart,
  Clock,
  DollarSign,
  Loader2,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import api from "@/lib/api";
import type { DashboardOverview } from "@/types";

const statCards = [
  { key: "totalOrders" as const, title: "전체 주문", icon: ShoppingCart, suffix: "건" },
  { key: "todayOrders" as const, title: "오늘 주문", icon: ShoppingCart, suffix: "건" },
  { key: "pendingOrders" as const, title: "미처리 주문", icon: Clock, suffix: "건" },
  { key: "monthlyRevenue" as const, title: "이번 달 매출", icon: DollarSign, suffix: "원", format: true },
];

export default function Dashboard() {
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const response = await api.get("/api/v1/dashboard/overview");
        console.log("Dashboard API full response:", response);
        console.log("Dashboard API data:", response.data);
        console.log("Dashboard API data.data:", response.data?.data);

        if (!response.data?.success) {
          setError(response.data?.message || "API 응답 실패");
          return;
        }

        const dashboardData = response.data.data;
        console.log("Setting overview:", dashboardData);
        setOverview(dashboardData);
        setError(null);
      } catch (err: unknown) {
        console.error("Dashboard API error:", err);
        const errorMessage = err instanceof Error ? err.message : "알 수 없는 오류";
        setError(`대시보드 데이터를 불러오는데 실패했습니다: ${errorMessage}`);
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center pt-24">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center pt-24 text-muted-foreground">
        <p className="text-red-500">{error}</p>
        <button
          onClick={() => window.location.reload()}
          className="mt-4 text-sm underline"
        >
          다시 시도
        </button>
      </div>
    );
  }

  return (
    <div>
      <h2 className="mb-4 text-xl font-semibold tracking-tight">대시보드</h2>
      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {statCards.map((stat) => {
          const value = overview?.[stat.key] ?? 0;
          const display = stat.format
            ? Number(value).toLocaleString()
            : value;
          return (
            <Card key={stat.key}>
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  {stat.title}
                </CardTitle>
                <stat.icon className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  {display}
                  <span className="ml-1 text-sm font-normal text-muted-foreground">
                    {stat.suffix}
                  </span>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>
      <Card>
        <CardHeader>
          <CardTitle>최근 주문</CardTitle>
        </CardHeader>
        <CardContent>
          {overview?.recentOrders?.length ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[180px]">주문번호</TableHead>
                  <TableHead className="w-[100px]">마켓</TableHead>
                  <TableHead className="w-[100px]">수취인</TableHead>
                  <TableHead className="w-[120px]">상태</TableHead>
                  <TableHead className="w-[120px] text-right">주문금액</TableHead>
                  <TableHead className="w-[120px]">주문일</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {overview.recentOrders.map((order) => (
                  <TableRow key={order.id}>
                    <TableCell className="font-mono text-sm">
                      {order.marketplaceOrderId}
                    </TableCell>
                    <TableCell>{order.marketplaceType}</TableCell>
                    <TableCell>{order.receiverName}</TableCell>
                    <TableCell>{order.status}</TableCell>
                    <TableCell className="text-right">
                      {order.totalAmount?.toLocaleString()}원
                    </TableCell>
                    <TableCell>{order.orderedAt}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <p className="py-8 text-center text-sm text-muted-foreground">
              주문 데이터가 없습니다.
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
