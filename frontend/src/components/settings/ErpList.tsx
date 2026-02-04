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
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { MoreHorizontal, Plus, Wifi, Pencil, Trash2, Server, Loader2, Database } from "lucide-react";
import { toast } from "sonner";
import api from "@/lib/api";
import ErpFormDialog from "./ErpFormDialog";
import type {
  ErpConfigResponse,
  ConnectionTestResponse,
  ErpItemSyncResponse,
  ApiResponse,
} from "@/types";

const ERP_LABELS: Record<string, string> = {
  ICOUNT: "이카운트 ERP",
  ECOUNT: "이카운트 ERP (ECount)",
};

export default function ErpList() {
  const [configs, setConfigs] = useState<ErpConfigResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<ErpConfigResponse | null>(null);
  const [testingId, setTestingId] = useState<string | null>(null);
  const [syncingId, setSyncingId] = useState<string | null>(null);

  const fetchConfigs = useCallback(async () => {
    try {
      const { data } = await api.get<ApiResponse<ErpConfigResponse[]>>(
        "/api/v1/settings/erp"
      );
      setConfigs(data.data ?? []);
    } catch {
      toast.error("ERP 목록을 불러오는데 실패했습니다");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchConfigs();
  }, [fetchConfigs]);

  const handleAdd = () => {
    setEditTarget(null);
    setDialogOpen(true);
  };

  const handleEdit = (cfg: ErpConfigResponse) => {
    setEditTarget(cfg);
    setDialogOpen(true);
  };

  const handleDelete = async (cfg: ErpConfigResponse) => {
    if (!window.confirm(`${ERP_LABELS[cfg.erpType] ?? cfg.erpType} 설정을 삭제하시겠습니까?`)) {
      return;
    }
    try {
      await api.delete(`/api/v1/settings/erp/${cfg.id}`);
      toast.success("ERP 설정이 삭제되었습니다");
      fetchConfigs();
    } catch {
      toast.error("삭제 중 오류가 발생했습니다");
    }
  };

  const handleTestConnection = async (cfg: ErpConfigResponse) => {
    setTestingId(cfg.id);
    try {
      const { data } = await api.post<ApiResponse<ConnectionTestResponse>>(
        `/api/v1/settings/erp/${cfg.id}/test-connection`
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

  const handleSyncItems = async (cfg: ErpConfigResponse) => {
    setSyncingId(cfg.id);
    try {
      const { data } = await api.post<ApiResponse<ErpItemSyncResponse>>(
        `/api/v1/settings/erp/${cfg.id}/items/sync`
      );
      if (data.data?.success) {
        toast.success(data.data.message || `품목 동기화 완료: ${data.data.syncedCount}건`);
      } else {
        toast.error(data.data?.message || "품목 동기화 실패");
      }
    } catch {
      toast.error("품목 동기화 중 오류가 발생했습니다");
    } finally {
      setSyncingId(null);
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
          연동된 ERP 시스템의 설정을 관리합니다.
        </p>
        <Button size="sm" onClick={handleAdd}>
          <Plus className="mr-2 h-4 w-4" />
          ERP 추가
        </Button>
      </div>

      {configs.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-12 text-muted-foreground">
          <Server className="mb-3 h-10 w-10" />
          <p className="mb-1 font-medium">등록된 ERP가 없습니다</p>
          <p className="mb-4 text-sm">ERP를 추가하여 시스템을 연동하세요.</p>
          <Button size="sm" variant="outline" onClick={handleAdd}>
            <Plus className="mr-2 h-4 w-4" />
            ERP 추가
          </Button>
        </div>
      ) : (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ERP 타입</TableHead>
                <TableHead>회사 코드</TableHead>
                <TableHead>사용자 ID</TableHead>
                <TableHead>상태</TableHead>
                <TableHead>등록일</TableHead>
                <TableHead className="w-[80px]" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {configs.map((cfg) => (
                <TableRow key={cfg.id}>
                  <TableCell className="font-medium">
                    {ERP_LABELS[cfg.erpType] ?? cfg.erpType}
                  </TableCell>
                  <TableCell>{cfg.companyCode}</TableCell>
                  <TableCell>{cfg.userId}</TableCell>
                  <TableCell>
                    <Badge variant={cfg.active ? "default" : "secondary"}>
                      {cfg.active ? "활성" : "비활성"}
                    </Badge>
                  </TableCell>
                  <TableCell>{formatDate(cfg.createdAt)}</TableCell>
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
                          onClick={() => handleTestConnection(cfg)}
                          disabled={testingId === cfg.id || syncingId === cfg.id}
                        >
                          {testingId === cfg.id ? (
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          ) : (
                            <Wifi className="mr-2 h-4 w-4" />
                          )}
                          연결 테스트
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => handleSyncItems(cfg)}
                          disabled={testingId === cfg.id || syncingId === cfg.id}
                        >
                          {syncingId === cfg.id ? (
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          ) : (
                            <Database className="mr-2 h-4 w-4" />
                          )}
                          품목 동기화
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem onClick={() => handleEdit(cfg)}>
                          <Pencil className="mr-2 h-4 w-4" />
                          수정
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => handleDelete(cfg)}
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

      <ErpFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        config={editTarget}
        onSuccess={fetchConfigs}
      />
    </div>
  );
}
