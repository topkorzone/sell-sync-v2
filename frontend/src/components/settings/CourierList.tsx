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
  Pencil,
  Trash2,
  Truck,
  Loader2,
} from "lucide-react";
import { toast } from "sonner";
import api from "@/lib/api";
import CourierFormDialog from "./CourierFormDialog";
import type { CourierConfigResponse, ApiResponse } from "@/types";

const COURIER_LABELS: Record<string, string> = {
  CJ: "CJ대한통운",
  HANJIN: "한진택배",
  LOGEN: "로젠택배",
  LOTTE: "롯데택배",
  POST: "우체국택배",
};

export default function CourierList() {
  const [configs, setConfigs] = useState<CourierConfigResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<CourierConfigResponse | null>(null);

  const fetchConfigs = useCallback(async () => {
    try {
      const { data } = await api.get<ApiResponse<CourierConfigResponse[]>>(
        "/api/v1/settings/couriers"
      );
      setConfigs(data.data ?? []);
    } catch {
      toast.error("택배사 목록을 불러오는데 실패했습니다");
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

  const handleEdit = (config: CourierConfigResponse) => {
    setEditTarget(config);
    setDialogOpen(true);
  };

  const handleDelete = async (config: CourierConfigResponse) => {
    if (
      !window.confirm(
        `${COURIER_LABELS[config.courierType] ?? config.courierType} 설정을 삭제하시겠습니까?`
      )
    ) {
      return;
    }
    try {
      await api.delete(`/api/v1/settings/couriers/${config.id}`);
      toast.success("택배사 설정이 삭제되었습니다");
      fetchConfigs();
    } catch {
      toast.error("삭제 중 오류가 발생했습니다");
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
          택배사 API 연동 설정을 관리합니다.
        </p>
        <Button size="sm" onClick={handleAdd}>
          <Plus className="mr-2 h-4 w-4" />
          택배사 추가
        </Button>
      </div>

      {configs.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-12 text-muted-foreground">
          <Truck className="mb-3 h-10 w-10" />
          <p className="mb-1 font-medium">등록된 택배사가 없습니다</p>
          <p className="mb-4 text-sm">택배사를 추가하여 택배 접수를 시작하세요.</p>
          <Button size="sm" variant="outline" onClick={handleAdd}>
            <Plus className="mr-2 h-4 w-4" />
            택배사 추가
          </Button>
        </div>
      ) : (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>택배사</TableHead>
                <TableHead>고객코드</TableHead>
                <TableHead>보내는분</TableHead>
                <TableHead>연락처</TableHead>
                <TableHead>상태</TableHead>
                <TableHead>등록일</TableHead>
                <TableHead className="w-[80px]" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {configs.map((config) => (
                <TableRow key={config.id}>
                  <TableCell className="font-medium">
                    {COURIER_LABELS[config.courierType] ?? config.courierType}
                  </TableCell>
                  <TableCell>{config.contractCode || "-"}</TableCell>
                  <TableCell>{config.senderName || "-"}</TableCell>
                  <TableCell>{config.senderPhone || "-"}</TableCell>
                  <TableCell>
                    <Badge variant={config.active ? "default" : "secondary"}>
                      {config.active ? "활성" : "비활성"}
                    </Badge>
                  </TableCell>
                  <TableCell>{config.createdAt ? formatDate(config.createdAt) : "-"}</TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreHorizontal className="h-4 w-4" />
                          <span className="sr-only">메뉴 열기</span>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => handleEdit(config)}>
                          <Pencil className="mr-2 h-4 w-4" />
                          수정
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => handleDelete(config)}
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

      <CourierFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        config={editTarget}
        onSuccess={fetchConfigs}
      />
    </div>
  );
}
