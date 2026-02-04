import { useEffect, useState, useCallback } from "react";
import { toast } from "sonner";
import { Search, Loader2, RefreshCw, Package, CheckCircle, XCircle } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import api from "@/lib/api";
import type { CoupangSellerProduct, CoupangSellerProductSyncResponse, PageResponse } from "@/types";

export default function CoupangProducts() {
  const [products, setProducts] = useState<CoupangSellerProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [brandFilter, setBrandFilter] = useState<string>("");

  // 필터 옵션
  const [statuses, setStatuses] = useState<string[]>([]);
  const [brands, setBrands] = useState<string[]>([]);

  // 동기화 상태
  const [syncing, setSyncing] = useState(false);
  const [syncResult, setSyncResult] = useState<CoupangSellerProductSyncResponse | null>(null);
  const [syncDialogOpen, setSyncDialogOpen] = useState(false);

  // 상품 수
  const [productCount, setProductCount] = useState(0);

  // 상품 목록 조회
  const fetchProducts = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string | number> = { page, size: 20 };
      if (keyword) params.keyword = keyword;
      if (statusFilter) params.status = statusFilter;
      if (brandFilter) params.brand = brandFilter;

      const { data } = await api.get<{ data: PageResponse<CoupangSellerProduct> }>(
        "/api/v1/coupang/seller-products",
        { params }
      );
      setProducts(data.data?.content ?? []);
      setTotal(data.data?.totalElements ?? 0);
    } catch {
      toast.error("상품 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [page, keyword, statusFilter, brandFilter]);

  // 상품 수 조회
  const fetchProductCount = useCallback(async () => {
    try {
      const { data } = await api.get<{ data: number }>("/api/v1/coupang/seller-products/count");
      setProductCount(data.data ?? 0);
    } catch {
      // 무시
    }
  }, []);

  // 필터 옵션 조회
  const fetchFilterOptions = useCallback(async () => {
    try {
      const [statusRes, brandRes] = await Promise.all([
        api.get<{ data: string[] }>("/api/v1/coupang/seller-products/statuses"),
        api.get<{ data: string[] }>("/api/v1/coupang/seller-products/brands"),
      ]);
      setStatuses(statusRes.data.data ?? []);
      setBrands(brandRes.data.data ?? []);
    } catch {
      // 무시
    }
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  useEffect(() => {
    fetchProductCount();
    fetchFilterOptions();
  }, [fetchProductCount, fetchFilterOptions]);

  // 동기화 실행
  const handleSync = async () => {
    setSyncing(true);
    setSyncResult(null);
    setSyncDialogOpen(true);

    try {
      const { data } = await api.post<{ data: CoupangSellerProductSyncResponse }>(
        "/api/v1/coupang/seller-products/sync"
      );
      setSyncResult(data.data);

      if (data.data?.success) {
        toast.success(`동기화 완료: ${data.data.totalCount}개 상품`);
        fetchProducts();
        fetchProductCount();
        fetchFilterOptions();
      } else {
        toast.error(`동기화 실패: ${data.data?.errorMessage}`);
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : "동기화에 실패했습니다.";
      toast.error(errorMessage);
      setSyncResult({
        success: false,
        totalCount: 0,
        insertedCount: 0,
        updatedCount: 0,
        syncStartedAt: new Date().toISOString(),
        syncCompletedAt: new Date().toISOString(),
        durationMs: 0,
        errorMessage: errorMessage,
      });
    } finally {
      setSyncing(false);
    }
  };

  const totalPages = Math.ceil(total / 20);

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return "-";
    return new Date(dateStr).toLocaleDateString("ko-KR");
  };

  const statusColors: Record<string, string> = {
    APPROVED: "bg-green-100 text-green-800",
    REJECTED: "bg-red-100 text-red-800",
    PENDING: "bg-yellow-100 text-yellow-800",
    STOPPED: "bg-gray-100 text-gray-800",
  };

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold tracking-tight">쿠팡 등록상품</h2>
          <p className="text-sm text-muted-foreground mt-1">
            쿠팡에 등록된 상품을 동기화하고 관리합니다
          </p>
        </div>
        <div className="flex items-center gap-3">
          <div className="text-sm text-muted-foreground">
            총 <span className="font-semibold text-foreground">{productCount.toLocaleString()}</span>개 상품
          </div>
          <Button onClick={handleSync} disabled={syncing}>
            {syncing ? (
              <Loader2 className="h-4 w-4 animate-spin mr-2" />
            ) : (
              <RefreshCw className="h-4 w-4 mr-2" />
            )}
            동기화
          </Button>
        </div>
      </div>

      <Card>
        <CardContent className="pt-6">
          <div className="mb-4 flex flex-wrap items-center gap-3">
            <Select
              value={statusFilter}
              onValueChange={(v) => {
                setStatusFilter(v === "all" ? "" : v);
                setPage(0);
              }}
            >
              <SelectTrigger className="w-[140px]">
                <SelectValue placeholder="상태 선택" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">전체 상태</SelectItem>
                {statuses.map((status) => (
                  <SelectItem key={status} value={status}>
                    {status}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select
              value={brandFilter}
              onValueChange={(v) => {
                setBrandFilter(v === "all" ? "" : v);
                setPage(0);
              }}
            >
              <SelectTrigger className="w-[160px]">
                <SelectValue placeholder="브랜드 선택" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">전체 브랜드</SelectItem>
                {brands.map((brand) => (
                  <SelectItem key={brand} value={brand}>
                    {brand}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="상품명, 브랜드 검색"
                className="w-[260px] pl-9"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    setPage(0);
                    fetchProducts();
                  }
                }}
              />
            </div>
          </div>

          {loading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : products.length === 0 ? (
            <div className="py-12 text-center">
              <Package className="mx-auto h-12 w-12 text-muted-foreground/50" />
              <div className="mt-4 text-sm text-muted-foreground">
                {productCount === 0 ? (
                  <>
                    <p>동기화된 상품이 없습니다</p>
                    <p className="mt-1">위의 "동기화" 버튼을 클릭하여 쿠팡 상품을 가져오세요</p>
                  </>
                ) : (
                  <p>검색 결과가 없습니다</p>
                )}
              </div>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[120px]">상품 ID</TableHead>
                    <TableHead className="min-w-[200px]">상품명</TableHead>
                    <TableHead className="w-[120px]">브랜드</TableHead>
                    <TableHead className="w-[100px]">상태</TableHead>
                    <TableHead className="w-[100px]">판매시작</TableHead>
                    <TableHead className="w-[100px]">판매종료</TableHead>
                    <TableHead className="w-[100px]">동기화</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {products.map((product) => (
                    <TableRow key={product.id}>
                      <TableCell className="font-mono text-sm">
                        {product.sellerProductId}
                      </TableCell>
                      <TableCell>
                        <div className="truncate max-w-[300px]" title={product.sellerProductName || "-"}>
                          {product.sellerProductName || "-"}
                        </div>
                      </TableCell>
                      <TableCell>
                        {product.brand || "-"}
                      </TableCell>
                      <TableCell>
                        {product.statusName ? (
                          <Badge
                            variant="outline"
                            className={statusColors[product.statusName] || ""}
                          >
                            {product.statusName}
                          </Badge>
                        ) : (
                          "-"
                        )}
                      </TableCell>
                      <TableCell className="text-sm">
                        {formatDate(product.saleStartedAt)}
                      </TableCell>
                      <TableCell className="text-sm">
                        {formatDate(product.saleEndedAt)}
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {formatDate(product.syncedAt)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                <span>총 {total.toLocaleString()}건</span>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 0}
                    onClick={() => setPage((p) => p - 1)}
                  >
                    이전
                  </Button>
                  <span className="flex items-center px-2">
                    {page + 1} / {totalPages || 1}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page + 1 >= totalPages}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    다음
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* 동기화 결과 다이얼로그 */}
      <Dialog open={syncDialogOpen} onOpenChange={setSyncDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {syncing ? (
                <>
                  <Loader2 className="h-5 w-5 animate-spin" />
                  동기화 진행 중...
                </>
              ) : syncResult?.success ? (
                <>
                  <CheckCircle className="h-5 w-5 text-green-500" />
                  동기화 완료
                </>
              ) : (
                <>
                  <XCircle className="h-5 w-5 text-red-500" />
                  동기화 실패
                </>
              )}
            </DialogTitle>
            <DialogDescription>
              {syncing ? (
                "쿠팡 API에서 등록상품을 가져오고 있습니다. 상품 수에 따라 시간이 걸릴 수 있습니다."
              ) : syncResult?.success ? (
                "쿠팡 등록상품 동기화가 완료되었습니다."
              ) : (
                syncResult?.errorMessage || "동기화 중 오류가 발생했습니다."
              )}
            </DialogDescription>
          </DialogHeader>

          {!syncing && syncResult && (
            <div className="mt-4 space-y-3">
              <div className="grid grid-cols-2 gap-4">
                <div className="rounded-lg border p-3">
                  <div className="text-2xl font-bold">{syncResult.totalCount.toLocaleString()}</div>
                  <div className="text-sm text-muted-foreground">총 상품 수</div>
                </div>
                <div className="rounded-lg border p-3">
                  <div className="text-2xl font-bold text-green-600">{syncResult.insertedCount.toLocaleString()}</div>
                  <div className="text-sm text-muted-foreground">신규 등록</div>
                </div>
                <div className="rounded-lg border p-3">
                  <div className="text-2xl font-bold text-blue-600">{syncResult.updatedCount.toLocaleString()}</div>
                  <div className="text-sm text-muted-foreground">업데이트</div>
                </div>
                <div className="rounded-lg border p-3">
                  <div className="text-2xl font-bold">{(syncResult.durationMs / 1000).toFixed(1)}s</div>
                  <div className="text-sm text-muted-foreground">소요 시간</div>
                </div>
              </div>

              <div className="flex justify-end">
                <Button onClick={() => setSyncDialogOpen(false)}>
                  확인
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
