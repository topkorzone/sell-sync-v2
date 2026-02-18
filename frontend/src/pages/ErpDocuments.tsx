import { useEffect, useState, useCallback } from "react";
import { toast } from "sonner";
import {
  RefreshCw,
  RefreshCcw,
  Send,
  Trash2,
  Eye,
  Loader2,
  CheckCircle2,
  Clock,
  XCircle,
  FileText,
  FilePlus,
  Ban,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
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
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import api from "@/lib/api";
import type {
  ErpSalesDocument,
  ErpDocumentStatus,
  ErpDocumentCounts,
  ErpBatchSendResponse,
  ErpPendingOrder,
  PageResponse,
  ApiResponse,
  MarketplaceType,
  SalesDocumentLine,
  OrderStatus,
} from "@/types";
import { ErpDocumentStatusLabels } from "@/types";

const marketplaceLabels: Record<MarketplaceType, string> = {
  NAVER: "스마트스토어",
  COUPANG: "쿠팡",
  ELEVEN_ST: "11번가",
  GMARKET: "G마켓",
  AUCTION: "옥션",
  WEMAKEPRICE: "위메프",
  TMON: "티몬",
};

const orderStatusLabels: Record<OrderStatus, string> = {
  PAYMENT_COMPLETE: "결제완료",
  PREPARING: "상품준비중",
  SHIPPING_READY: "배송지시",
  SHIPPING: "배송중",
  DELIVERED: "배송완료",
  CANCELLED: "취소",
  RETURNED: "반품",
  PURCHASE_CONFIRMED: "구매확정",
};

const statusConfig: Record<
  ErpDocumentStatus,
  { icon: typeof Clock; className: string; badgeClass: string }
> = {
  PENDING: {
    icon: Clock,
    className: "text-yellow-600",
    badgeClass: "bg-yellow-100 text-yellow-700 border-yellow-200",
  },
  SENT: {
    icon: CheckCircle2,
    className: "text-green-600",
    badgeClass: "bg-green-100 text-green-700 border-green-200",
  },
  FAILED: {
    icon: XCircle,
    className: "text-red-600",
    badgeClass: "bg-red-100 text-red-700 border-red-200",
  },
  CANCELLED: {
    icon: XCircle,
    className: "text-gray-400",
    badgeClass: "bg-gray-100 text-gray-500 border-gray-200",
  },
};

// 날짜 포맷팅
function formatDate(dateStr?: string | null): string {
  if (!dateStr) return "-";
  try {
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  } catch {
    return dateStr;
  }
}

// 금액 포맷팅
function formatCurrency(amount?: number | null): string {
  if (amount == null) return "-";
  return amount.toLocaleString();
}

// documentLines 정규화 (기존 BulkDatas 래퍼 형식과 새 형식 모두 처리)
function normalizeDocumentLines(lines: unknown[]): SalesDocumentLine[] {
  if (!lines || !Array.isArray(lines)) return [];
  return lines.map((item) => {
    // BulkDatas 래퍼가 있는 경우 (기존 형식)
    if (item && typeof item === "object" && "BulkDatas" in item) {
      return (item as { BulkDatas: SalesDocumentLine }).BulkDatas;
    }
    // 직접 데이터인 경우 (새 형식)
    return item as SalesDocumentLine;
  });
}

export default function ErpDocuments() {
  const [activeTab, setActiveTab] = useState<"pending-orders" | "documents">("pending-orders");

  // 전표 미생성 주문 상태
  const [pendingOrders, setPendingOrders] = useState<ErpPendingOrder[]>([]);
  const [pendingOrdersLoading, setPendingOrdersLoading] = useState(true);
  const [pendingOrdersTotal, setPendingOrdersTotal] = useState(0);
  const [pendingOrdersPage, setPendingOrdersPage] = useState(0);
  const [selectedOrderIds, setSelectedOrderIds] = useState<Set<string>>(new Set());
  const [generating, setGenerating] = useState(false);

  // 생성된 전표 상태
  const [documents, setDocuments] = useState<ErpSalesDocument[]>([]);
  const [documentsLoading, setDocumentsLoading] = useState(true);
  const [documentsTotal, setDocumentsTotal] = useState(0);
  const [documentsPage, setDocumentsPage] = useState(0);
  const [documentStatusFilter, setDocumentStatusFilter] = useState<ErpDocumentStatus | "ALL">("PENDING");
  const [selectedDocIds, setSelectedDocIds] = useState<Set<string>>(new Set());
  const [sending, setSending] = useState(false);
  const [sendingId, setSendingId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [regeneratingId, setRegeneratingId] = useState<string | null>(null);

  // 공통 상태
  const [counts, setCounts] = useState<ErpDocumentCounts & { NEED_DOCUMENT?: number }>({
    PENDING: 0,
    SENT: 0,
    FAILED: 0,
    CANCELLED: 0,
  });
  const [detailDocument, setDetailDocument] = useState<ErpSalesDocument | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

  // 전표 미생성 주문 조회
  const fetchPendingOrders = useCallback(async () => {
    setPendingOrdersLoading(true);
    try {
      const { data } = await api.get<{ data: PageResponse<ErpPendingOrder> }>(
        "/api/v1/erp/documents/pending-orders",
        { params: { page: pendingOrdersPage, size: 20 } }
      );
      setPendingOrders(data.data?.content ?? []);
      setPendingOrdersTotal(data.data?.totalElements ?? 0);
    } catch {
      toast.error("주문 목록을 불러오지 못했습니다.");
    } finally {
      setPendingOrdersLoading(false);
    }
  }, [pendingOrdersPage]);

  // 생성된 전표 조회
  const fetchDocuments = useCallback(async () => {
    setDocumentsLoading(true);
    try {
      const params: Record<string, string | number> = { page: documentsPage, size: 20 };
      if (documentStatusFilter !== "ALL") {
        params.status = documentStatusFilter;
      }
      const { data } = await api.get<{ data: PageResponse<ErpSalesDocument> }>(
        "/api/v1/erp/documents",
        { params }
      );
      setDocuments(data.data?.content ?? []);
      setDocumentsTotal(data.data?.totalElements ?? 0);
    } catch {
      toast.error("전표 목록을 불러오지 못했습니다.");
    } finally {
      setDocumentsLoading(false);
    }
  }, [documentsPage, documentStatusFilter]);

  // 카운트 조회
  const fetchCounts = useCallback(async () => {
    try {
      const { data } = await api.get<ApiResponse<ErpDocumentCounts & { NEED_DOCUMENT?: number }>>(
        "/api/v1/erp/documents/counts"
      );
      if (data.data) {
        setCounts(data.data);
      }
    } catch {
      // 조용히 실패
    }
  }, []);

  useEffect(() => {
    fetchPendingOrders();
  }, [fetchPendingOrders]);

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  useEffect(() => {
    fetchCounts();
  }, [fetchCounts]);

  // 선택된 주문에 대해 전표 생성
  const handleGenerateDocuments = async () => {
    if (selectedOrderIds.size === 0) {
      toast.warning("전표를 생성할 주문을 선택해주세요.");
      return;
    }
    setGenerating(true);
    try {
      const { data } = await api.post<ApiResponse<{ successCount: number; failCount: number }>>(
        "/api/v1/erp/documents/generate-batch",
        Array.from(selectedOrderIds)
      );
      const result = data.data;
      if (result) {
        toast.success(`전표 생성: 성공 ${result.successCount}건 / 실패 ${result.failCount}건`);
      }
      setSelectedOrderIds(new Set());
      fetchPendingOrders();
      fetchDocuments();
      fetchCounts();
    } catch {
      toast.error("전표 생성에 실패했습니다.");
    } finally {
      setGenerating(false);
    }
  };

  // 개별 전표 전송
  const handleSend = async (documentId: string) => {
    setSendingId(documentId);
    try {
      const { data } = await api.post<ApiResponse<ErpSalesDocument>>(
        `/api/v1/erp/documents/${documentId}/send`
      );
      if (data.data?.status === "SENT") {
        toast.success("전표가 전송되었습니다.");
        // 전송 완료된 전표는 선택에서 제거
        setSelectedDocIds((prev) => {
          const newSet = new Set(prev);
          newSet.delete(documentId);
          return newSet;
        });
      } else {
        toast.error(data.data?.errorMessage || "전표 전송에 실패했습니다.");
      }
      fetchDocuments();
      fetchCounts();
    } catch {
      toast.error("전표 전송에 실패했습니다.");
    } finally {
      setSendingId(null);
    }
  };

  // 선택 전표 전송
  const handleSendSelected = async () => {
    if (selectedDocIds.size === 0) {
      toast.warning("전송할 전표를 선택해주세요.");
      return;
    }
    setSending(true);
    try {
      const { data } = await api.post<ApiResponse<ErpBatchSendResponse>>(
        "/api/v1/erp/documents/send-selected",
        Array.from(selectedDocIds)
      );
      const result = data.data;
      if (result) {
        toast.success(`전표 전송: 성공 ${result.successCount}건 / 실패 ${result.failCount}건`);
      }
      setSelectedDocIds(new Set());
      fetchDocuments();
      fetchCounts();
    } catch {
      toast.error("전표 전송에 실패했습니다.");
    } finally {
      setSending(false);
    }
  };

  // 전표 삭제
  const handleDelete = async (documentId: string) => {
    setDeletingId(documentId);
    try {
      await api.delete(`/api/v1/erp/documents/${documentId}`);
      toast.success("전표가 삭제되었습니다.");
      fetchDocuments();
      fetchCounts();
    } catch {
      toast.error("전표 삭제에 실패했습니다.");
    } finally {
      setDeletingId(null);
      setDeleteConfirmId(null);
    }
  };

  // 전표 재생성 (취소된 전표)
  const handleRegenerate = async (orderId: string) => {
    setRegeneratingId(orderId);
    try {
      await api.post<ApiResponse<ErpSalesDocument>>(
        `/api/v1/erp/documents/regenerate/${orderId}`
      );
      toast.success("전표가 재생성되었습니다.");
      fetchDocuments();
      fetchPendingOrders();
      fetchCounts();
    } catch {
      toast.error("전표 재생성에 실패했습니다.");
    } finally {
      setRegeneratingId(null);
    }
  };

  // 주문 체크박스 토글
  const toggleOrderSelect = (id: string) => {
    const newSelected = new Set(selectedOrderIds);
    if (newSelected.has(id)) {
      newSelected.delete(id);
    } else {
      newSelected.add(id);
    }
    setSelectedOrderIds(newSelected);
  };

  const toggleOrderSelectAll = () => {
    if (pendingOrders.every((o) => selectedOrderIds.has(o.id))) {
      setSelectedOrderIds(new Set());
    } else {
      setSelectedOrderIds(new Set(pendingOrders.map((o) => o.id)));
    }
  };

  // 전표 체크박스 토글
  const toggleDocSelect = (id: string) => {
    const newSelected = new Set(selectedDocIds);
    if (newSelected.has(id)) {
      newSelected.delete(id);
    } else {
      newSelected.add(id);
    }
    setSelectedDocIds(newSelected);
  };

  const toggleDocSelectAll = () => {
    const selectableDocs = documents.filter((d) => d.status === "PENDING" || d.status === "FAILED");
    if (selectableDocs.every((d) => selectedDocIds.has(d.id))) {
      setSelectedDocIds(new Set());
    } else {
      setSelectedDocIds(new Set(selectableDocs.map((d) => d.id)));
    }
  };

  const pendingOrdersTotalPages = Math.ceil(pendingOrdersTotal / 20);
  const documentsTotalPages = Math.ceil(documentsTotal / 20);
  const allOrdersSelected = pendingOrders.length > 0 && pendingOrders.every((o) => selectedOrderIds.has(o.id));
  const selectableDocs = documents.filter((d) => d.status === "PENDING" || d.status === "FAILED");
  const allDocsSelected = selectableDocs.length > 0 && selectableDocs.every((d) => selectedDocIds.has(d.id));

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-xl font-semibold tracking-tight">ERP 전표 관리</h2>
        <Button
          variant="outline"
          onClick={() => {
            fetchPendingOrders();
            fetchDocuments();
            fetchCounts();
          }}
        >
          <RefreshCw className="mr-2 h-4 w-4" />
          새로고침
        </Button>
      </div>

      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as typeof activeTab)}>
        <TabsList className="mb-4">
          <TabsTrigger value="pending-orders" className="gap-2">
            <FileText className="h-4 w-4" />
            전표 미생성 주문
            {(counts.NEED_DOCUMENT ?? 0) > 0 && (
              <Badge variant="secondary" className="ml-1">
                {counts.NEED_DOCUMENT}
              </Badge>
            )}
          </TabsTrigger>
          <TabsTrigger value="documents" className="gap-2">
            <FilePlus className="h-4 w-4" />
            생성된 전표
            {counts.PENDING > 0 && (
              <Badge variant="secondary" className="ml-1">
                {counts.PENDING}
              </Badge>
            )}
          </TabsTrigger>
        </TabsList>

        {/* 전표 미생성 주문 탭 */}
        <TabsContent value="pending-orders">
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <CardTitle className="text-base">배송중/배송완료 주문 (상품매핑 완료, 전표 미생성)</CardTitle>
                {selectedOrderIds.size > 0 && (
                  <Button onClick={handleGenerateDocuments} disabled={generating}>
                    {generating ? (
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    ) : (
                      <FilePlus className="mr-2 h-4 w-4" />
                    )}
                    전표 생성 ({selectedOrderIds.size}건)
                  </Button>
                )}
              </div>
            </CardHeader>
            <CardContent>
              {pendingOrdersLoading ? (
                <div className="flex justify-center py-12">
                  <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                </div>
              ) : pendingOrders.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
                  <CheckCircle2 className="mb-3 h-10 w-10" />
                  <p>전표 생성이 필요한 주문이 없습니다.</p>
                </div>
              ) : (
                <>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-[40px]">
                          <Checkbox
                            checked={allOrdersSelected}
                            onCheckedChange={toggleOrderSelectAll}
                          />
                        </TableHead>
                        <TableHead className="w-[160px]">주문번호</TableHead>
                        <TableHead className="w-[90px]">마켓</TableHead>
                        <TableHead className="w-[80px]">상태</TableHead>
                        <TableHead className="w-[100px]">수취인</TableHead>
                        <TableHead className="w-[100px] text-right">금액</TableHead>
                        <TableHead className="w-[130px]">주문일시</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {pendingOrders.map((order) => (
                        <TableRow key={order.id}>
                          <TableCell>
                            <Checkbox
                              checked={selectedOrderIds.has(order.id)}
                              onCheckedChange={() => toggleOrderSelect(order.id)}
                            />
                          </TableCell>
                          <TableCell className="font-mono text-xs">{order.marketplaceOrderId}</TableCell>
                          <TableCell className="text-sm">
                            {marketplaceLabels[order.marketplaceType] || order.marketplaceType}
                          </TableCell>
                          <TableCell>
                            <Badge
                              variant="outline"
                              className={`text-xs border ${
                                order.status === "SHIPPING"
                                  ? "bg-orange-100 text-orange-700 border-orange-200"
                                  : "bg-green-100 text-green-700 border-green-200"
                              }`}
                            >
                              {orderStatusLabels[order.status] || order.status}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-sm">{order.receiverName}</TableCell>
                          <TableCell className="text-right text-sm">
                            {formatCurrency(order.totalAmount)}
                          </TableCell>
                          <TableCell className="text-sm whitespace-nowrap">
                            {formatDate(order.orderedAt)}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                  <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                    <span>총 {pendingOrdersTotal}건</span>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={pendingOrdersPage === 0}
                        onClick={() => setPendingOrdersPage((p) => p - 1)}
                      >
                        이전
                      </Button>
                      <span className="flex items-center px-2">
                        {pendingOrdersPage + 1} / {pendingOrdersTotalPages || 1}
                      </span>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={pendingOrdersPage + 1 >= pendingOrdersTotalPages}
                        onClick={() => setPendingOrdersPage((p) => p + 1)}
                      >
                        다음
                      </Button>
                    </div>
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* 생성된 전표 탭 */}
        <TabsContent value="documents">
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div className="flex gap-2">
                  <Button
                    variant={documentStatusFilter === "ALL" ? "default" : "outline"}
                    size="sm"
                    onClick={() => {
                      setDocumentStatusFilter("ALL");
                      setDocumentsPage(0);
                      setSelectedDocIds(new Set());
                    }}
                  >
                    전체
                  </Button>
                  <Button
                    variant={documentStatusFilter === "PENDING" ? "default" : "outline"}
                    size="sm"
                    onClick={() => {
                      setDocumentStatusFilter("PENDING");
                      setDocumentsPage(0);
                      setSelectedDocIds(new Set());
                    }}
                  >
                    <Clock className="mr-1 h-3 w-3" />
                    미전송 ({counts.PENDING})
                  </Button>
                  <Button
                    variant={documentStatusFilter === "SENT" ? "default" : "outline"}
                    size="sm"
                    onClick={() => {
                      setDocumentStatusFilter("SENT");
                      setDocumentsPage(0);
                      setSelectedDocIds(new Set());
                    }}
                  >
                    <CheckCircle2 className="mr-1 h-3 w-3" />
                    전송완료 ({counts.SENT})
                  </Button>
                  <Button
                    variant={documentStatusFilter === "FAILED" ? "default" : "outline"}
                    size="sm"
                    onClick={() => {
                      setDocumentStatusFilter("FAILED");
                      setDocumentsPage(0);
                      setSelectedDocIds(new Set());
                    }}
                  >
                    <XCircle className="mr-1 h-3 w-3" />
                    실패 ({counts.FAILED})
                  </Button>
                  <Button
                    variant={documentStatusFilter === "CANCELLED" ? "default" : "outline"}
                    size="sm"
                    onClick={() => {
                      setDocumentStatusFilter("CANCELLED");
                      setDocumentsPage(0);
                      setSelectedDocIds(new Set());
                    }}
                  >
                    <Ban className="mr-1 h-3 w-3" />
                    취소 ({counts.CANCELLED ?? 0})
                  </Button>
                </div>
                {selectedDocIds.size > 0 && (
                  <Button onClick={handleSendSelected} disabled={sending}>
                    {sending ? (
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    ) : (
                      <Send className="mr-2 h-4 w-4" />
                    )}
                    ERP 전송 ({selectedDocIds.size}건)
                  </Button>
                )}
              </div>
            </CardHeader>
            <CardContent>
              {documentsLoading ? (
                <div className="flex justify-center py-12">
                  <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                </div>
              ) : documents.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
                  <FileText className="mb-3 h-10 w-10" />
                  <p>생성된 전표가 없습니다.</p>
                </div>
              ) : (
                <>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-[40px]">
                          <Checkbox
                            checked={allDocsSelected}
                            onCheckedChange={toggleDocSelectAll}
                            disabled={selectableDocs.length === 0}
                          />
                        </TableHead>
                        <TableHead className="w-[100px]">전표일자</TableHead>
                        <TableHead className="w-[160px]">주문번호</TableHead>
                        <TableHead className="w-[90px]">마켓</TableHead>
                        <TableHead className="w-[100px]">거래처</TableHead>
                        <TableHead className="w-[100px] text-right">금액</TableHead>
                        <TableHead className="w-[80px]">상태</TableHead>
                        <TableHead className="w-[100px]">전표번호</TableHead>
                        <TableHead className="w-[100px]">액션</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {documents.map((doc) => {
                        const config = statusConfig[doc.status];
                        const canSend = doc.status === "PENDING" || doc.status === "FAILED";
                        const canDelete = doc.status === "PENDING" || doc.status === "FAILED";
                        const canRegenerate = doc.status === "CANCELLED";

                        return (
                          <TableRow key={doc.id}>
                            <TableCell>
                              <Checkbox
                                checked={selectedDocIds.has(doc.id)}
                                onCheckedChange={() => toggleDocSelect(doc.id)}
                                disabled={!canSend}
                              />
                            </TableCell>
                            <TableCell className="text-sm">{doc.documentDate}</TableCell>
                            <TableCell className="font-mono text-xs truncate max-w-[160px]">
                              {doc.marketplaceOrderId || "-"}
                            </TableCell>
                            <TableCell className="text-sm">
                              {marketplaceLabels[doc.marketplaceType] || doc.marketplaceType}
                            </TableCell>
                            <TableCell className="text-sm truncate max-w-[100px]">
                              {doc.customerName || doc.customerCode || "-"}
                            </TableCell>
                            <TableCell className="text-right text-sm">
                              {formatCurrency(doc.totalAmount)}
                            </TableCell>
                            <TableCell>
                              <Badge variant="outline" className={`text-xs border ${config.badgeClass}`}>
                                {ErpDocumentStatusLabels[doc.status]}
                              </Badge>
                            </TableCell>
                            <TableCell className="text-sm font-mono">
                              {doc.erpDocumentId || "-"}
                            </TableCell>
                            <TableCell>
                              <div className="flex gap-1">
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="h-7 w-7"
                                  onClick={() => setDetailDocument(doc)}
                                  title="상세보기"
                                >
                                  <Eye className="h-4 w-4" />
                                </Button>
                                {canSend && (
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-7 w-7 text-blue-600 hover:text-blue-700"
                                    onClick={() => handleSend(doc.id)}
                                    disabled={sendingId === doc.id}
                                    title="전송"
                                  >
                                    {sendingId === doc.id ? (
                                      <Loader2 className="h-4 w-4 animate-spin" />
                                    ) : (
                                      <Send className="h-4 w-4" />
                                    )}
                                  </Button>
                                )}
                                {canDelete && (
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-7 w-7 text-red-600 hover:text-red-700"
                                    onClick={() => setDeleteConfirmId(doc.id)}
                                    disabled={deletingId === doc.id}
                                    title="삭제"
                                  >
                                    {deletingId === doc.id ? (
                                      <Loader2 className="h-4 w-4 animate-spin" />
                                    ) : (
                                      <Trash2 className="h-4 w-4" />
                                    )}
                                  </Button>
                                )}
                                {canRegenerate && (
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-7 w-7 text-green-600 hover:text-green-700"
                                    onClick={() => handleRegenerate(doc.orderId)}
                                    disabled={regeneratingId === doc.orderId}
                                    title="재생성"
                                  >
                                    {regeneratingId === doc.orderId ? (
                                      <Loader2 className="h-4 w-4 animate-spin" />
                                    ) : (
                                      <RefreshCcw className="h-4 w-4" />
                                    )}
                                  </Button>
                                )}
                              </div>
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                  <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                    <span>총 {documentsTotal}건</span>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={documentsPage === 0}
                        onClick={() => setDocumentsPage((p) => p - 1)}
                      >
                        이전
                      </Button>
                      <span className="flex items-center px-2">
                        {documentsPage + 1} / {documentsTotalPages || 1}
                      </span>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={documentsPage + 1 >= documentsTotalPages}
                        onClick={() => setDocumentsPage((p) => p + 1)}
                      >
                        다음
                      </Button>
                    </div>
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* 상세 다이얼로그 */}
      <Dialog open={!!detailDocument} onOpenChange={() => setDetailDocument(null)}>
        <DialogContent className="sm:max-w-5xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>전표 상세</DialogTitle>
          </DialogHeader>
          {detailDocument && (
            <div className="space-y-4">
              {/* 기본 정보 */}
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-muted-foreground">전표일자:</span>{" "}
                  <span className="font-medium">{detailDocument.documentDate}</span>
                </div>
                <div>
                  <span className="text-muted-foreground">마켓:</span>{" "}
                  <span className="font-medium">
                    {marketplaceLabels[detailDocument.marketplaceType]}
                  </span>
                </div>
                <div>
                  <span className="text-muted-foreground">주문번호:</span>{" "}
                  <span className="font-mono">{detailDocument.marketplaceOrderId}</span>
                </div>
                <div>
                  <span className="text-muted-foreground">거래처:</span>{" "}
                  <span className="font-medium">
                    {detailDocument.customerCode} - {detailDocument.customerName}
                  </span>
                </div>
                <div>
                  <span className="text-muted-foreground">상태:</span>{" "}
                  <Badge
                    variant="outline"
                    className={`text-xs border ${statusConfig[detailDocument.status].badgeClass}`}
                  >
                    {ErpDocumentStatusLabels[detailDocument.status]}
                  </Badge>
                </div>
                <div>
                  <span className="text-muted-foreground">ERP 전표번호:</span>{" "}
                  <span className="font-mono">{detailDocument.erpDocumentId || "-"}</span>
                </div>
                {detailDocument.sentAt && (
                  <div>
                    <span className="text-muted-foreground">전송일시:</span>{" "}
                    <span>{formatDate(detailDocument.sentAt)}</span>
                  </div>
                )}
                {detailDocument.errorMessage && (
                  <div className="col-span-2">
                    <span className="text-muted-foreground">오류:</span>{" "}
                    <span className="text-red-600">{detailDocument.errorMessage}</span>
                  </div>
                )}
              </div>

              {/* 전표 라인 */}
              <div>
                <h4 className="font-medium mb-2">전표 라인</h4>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-[50px]">행</TableHead>
                      <TableHead className="w-[100px]">품목코드</TableHead>
                      <TableHead>품목명</TableHead>
                      <TableHead className="w-[60px] text-right">수량</TableHead>
                      <TableHead className="w-[90px] text-right">공급가</TableHead>
                      <TableHead className="w-[70px] text-right">VAT</TableHead>
                      <TableHead className="w-[90px] text-right">합계</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {normalizeDocumentLines(detailDocument.documentLines as unknown[]).map((line, idx) => (
                      <TableRow key={idx}>
                        <TableCell className="text-sm">{line.LINE_NO}</TableCell>
                        <TableCell className="font-mono text-xs">{line.PROD_CD || "-"}</TableCell>
                        <TableCell className="text-sm">{line.PROD_DES}</TableCell>
                        <TableCell className="text-right text-sm">{line.QTY}</TableCell>
                        <TableCell className="text-right text-sm">
                          {formatCurrency(parseInt(line.SUPPLY_AMT || "0"))}
                        </TableCell>
                        <TableCell className="text-right text-sm">
                          {formatCurrency(parseInt(line.VAT_AMT || "0"))}
                        </TableCell>
                        <TableCell className="text-right text-sm font-medium">
                          {formatCurrency(parseInt(line.PRICE || "0"))}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {/* 합계 */}
              <div className="flex justify-end">
                <div className="text-right">
                  <span className="text-muted-foreground">총액:</span>{" "}
                  <span className="text-lg font-semibold">
                    {formatCurrency(
                      normalizeDocumentLines(detailDocument.documentLines as unknown[])
                        .reduce((sum, line) => sum + parseInt(line.PRICE || "0"), 0)
                    )}원
                  </span>
                </div>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* 삭제 확인 다이얼로그 */}
      <AlertDialog open={!!deleteConfirmId} onOpenChange={() => setDeleteConfirmId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>전표 삭제</AlertDialogTitle>
            <AlertDialogDescription>
              이 전표를 삭제하시겠습니까? 삭제된 전표는 복구할 수 없습니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              className="bg-red-600 hover:bg-red-700"
              onClick={() => deleteConfirmId && handleDelete(deleteConfirmId)}
            >
              삭제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
