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
import { MoreHorizontal, Plus, Wifi, Pencil, Trash2, Store, Loader2 } from "lucide-react";
import { toast } from "sonner";
import api from "@/lib/api";
import StoreFormDialog from "./StoreFormDialog";
import type {
  MarketplaceCredentialResponse,
  ConnectionTestResponse,
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
    </div>
  );
}
