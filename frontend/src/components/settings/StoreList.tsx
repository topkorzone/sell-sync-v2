import { useState, useEffect, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  MoreHorizontal,
  Plus,
  Wifi,
  Pencil,
  Trash2,
  Store,
  Loader2,
  RefreshCw,
  CheckCircle,
  XCircle,
  Package,
} from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { toast } from "sonner";
import api from "@/lib/api";
import StoreFormDialog from "./StoreFormDialog";
import type {
  MarketplaceCredentialResponse,
  ConnectionTestResponse,
  CoupangSellerProductSyncResponse,
  ApiResponse,
} from "@/types";

const MARKETPLACE_LABELS: Record<string, string> = {
  COUPANG: "쿠팡",
  NAVER: "네이버 스마트스토어",
  ELEVEN_ST: "11번가",
  GMARKET: "G마켓",
  AUCTION: "옥션",
  WEMAKEPRICE: "위메프",
  TMON: "티몬",
};

export default function StoreList() {
  const [credentials, setCredentials] = useState<MarketplaceCredentialResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<MarketplaceCredentialResponse | null>(null);
  const [testingId, setTestingId] = useState<string | null>(null);

  // 쿠팡 상품 동기화 상태
  const [syncing, setSyncing] = useState(false);
  const [syncDialogOpen, setSyncDialogOpen] = useState(false);
  const [syncResult, setSyncResult] = useState<CoupangSellerProductSyncResponse | null>(null);

  const fetchCredentials = useCallback(async () => {
    try {
      const { data } = await api.get<ApiResponse<MarketplaceCredentialResponse[]>>(
        "/api/v1/settings/marketplaces"
      );
      setCredentials(data.data ?? []);
    } catch {
      toast.error("스토어 목록을 불러오는데 실패했습니다");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCredentials();
  }, [fetchCredentials]);

  const handleAdd = () => {
    setEditTarget(null);
    setDialogOpen(true);
  };

  const handleEdit = (cred: MarketplaceCredentialResponse) => {
    setEditTarget(cred);
    setDialogOpen(true);
  };

  const handleDelete = async (cred: MarketplaceCredentialResponse) => {
    if (!window.confirm(`${MARKETPLACE_LABELS[cred.marketplaceType] ?? cred.marketplaceType} 스토어를 삭제하시겠습니까?`)) {
      return;
    }
    try {
      await api.delete(`/api/v1/settings/marketplaces/${cred.id}`);
      toast.success("스토어가 삭제되었습니다");
      fetchCredentials();
    } catch {
      toast.error("삭제 중 오류가 발생했습니다");
    }
  };

  const handleTestConnection = async (cred: MarketplaceCredentialResponse) => {
    setTestingId(cred.id);
    try {
      const { data } = await api.post<ApiResponse<ConnectionTestResponse>>(
        `/api/v1/settings/marketplaces/${cred.id}/test-connection`
      );
      if (data.data?.connected) {
        toast.success(data.data.message || "연결 성공");
      } else {
        toast.error(data.data?.message || "연결 실패");
      }
    } catch {
      toast.error("연결 테스트 중 오류가 발생했습니다");
    } finally {
      setTestingId(null);
    }
  };

  // 쿠팡 상품 동기화
  const handleSyncCoupangProducts = async () => {
    setSyncing(true);
    setSyncResult(null);
    setSyncDialogOpen(true);

    try {
      // 동기화는 시간이 오래 걸릴 수 있으므로 타임아웃 5분으로 설정
      const response = await api.post<ApiResponse<CoupangSellerProductSyncResponse>>(
        "/api/v1/coupang/seller-products/sync",
        {},
        { timeout: 300000 }
      );

      // API 응답 구조: { success: boolean, data: CoupangSellerProductSyncResponse, error: ... }
      const apiResponse = response.data;
      const syncData = apiResponse.data;

      if (apiResponse.success && syncData) {
        setSyncResult(syncData);
        if (syncData.success) {
          toast.success(`동기화 완료: ${syncData.totalCount}개 상품`);
        } else {
          toast.error(`동기화 실패: ${syncData.errorMessage || "알 수 없는 오류"}`);
        }
      } else {
        // API 레벨 오류
        const errorMsg = "동기화 요청에 실패했습니다.";
        toast.error(errorMsg);
        setSyncResult({
          success: false,
          totalCount: 0,
          insertedCount: 0,
          updatedCount: 0,
          syncStartedAt: new Date().toISOString(),
          syncCompletedAt: new Date().toISOString(),
          durationMs: 0,
          errorMessage: errorMsg,
        });
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

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          연동된 마켓플레이스의 API 자격증명을 관리합니다.
        </p>
        <Button size="sm" onClick={handleAdd}>
          <Plus className="mr-2 h-4 w-4" />
          스토어 추가
        </Button>
      </div>

      {credentials.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-12 text-muted-foreground">
          <Store className="mb-3 h-10 w-10" />
          <p className="mb-1 font-medium">등록된 마켓플레이스가 없습니다</p>
          <p className="mb-4 text-sm">스토어를 추가하여 마켓플레이스를 연동하세요.</p>
          <Button size="sm" variant="outline" onClick={handleAdd}>
            <Plus className="mr-2 h-4 w-4" />
            스토어 추가
          </Button>
        </div>
      ) : (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>마켓플레이스</TableHead>
                <TableHead>판매자 ID</TableHead>
                <TableHead>상태</TableHead>
                <TableHead>등록일</TableHead>
                <TableHead className="w-[80px]" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {credentials.map((cred) => (
                <TableRow key={cred.id}>
                  <TableCell className="font-medium">
                    {MARKETPLACE_LABELS[cred.marketplaceType] ?? cred.marketplaceType}
                  </TableCell>
                  <TableCell>{cred.sellerId}</TableCell>
                  <TableCell>
                    <Badge variant={cred.active ? "default" : "secondary"}>
                      {cred.active ? "활성" : "비활성"}
                    </Badge>
                  </TableCell>
                  <TableCell>{formatDate(cred.createdAt)}</TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreHorizontal className="h-4 w-4" />
                          <span className="sr-only">메뉴 열기</span>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem
                          onClick={() => handleTestConnection(cred)}
                          disabled={testingId === cred.id}
                        >
                          {testingId === cred.id ? (
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          ) : (
                            <Wifi className="mr-2 h-4 w-4" />
                          )}
                          연결 테스트
                        </DropdownMenuItem>
                        {cred.marketplaceType === "COUPANG" && (
                          <DropdownMenuItem
                            onClick={handleSyncCoupangProducts}
                            disabled={syncing}
                          >
                            {syncing ? (
                              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                            ) : (
                              <RefreshCw className="mr-2 h-4 w-4" />
                            )}
                            상품 동기화
                          </DropdownMenuItem>
                        )}
                        <DropdownMenuItem onClick={() => handleEdit(cred)}>
                          <Pencil className="mr-2 h-4 w-4" />
                          수정
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => handleDelete(cred)}
                          className="text-destructive focus:text-destructive"
                        >
                          <Trash2 className="mr-2 h-4 w-4" />
                          삭제
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <StoreFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        credential={editTarget}
        onSuccess={fetchCredentials}
      />

      {/* 쿠팡 상품 동기화 결과 다이얼로그 */}
      <Dialog open={syncDialogOpen} onOpenChange={setSyncDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {syncing ? (
                <>
                  <Loader2 className="h-5 w-5 animate-spin" />
                  상품 동기화 진행 중...
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

              {syncResult.success && (
                <div className="flex items-center gap-2 rounded-lg bg-muted p-3 text-sm">
                  <Package className="h-4 w-4" />
                  <span>동기화된 상품은 "쿠팡 상품" 메뉴에서 확인할 수 있습니다.</span>
                </div>
              )}

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
