import { useState, useEffect, useCallback } from "react";
import { toast } from "sonner";
import { Search, Loader2, X, Check, ChevronDown } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Label } from "@/components/ui/label";
import api from "@/lib/api";
import type { Order, OrderItem, ErpItem, PageResponse } from "@/types";

interface MappingDialogProps {
  order: Order;
  initialItemId?: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onMappingComplete: () => void;
}

const marketplaceLabels: Record<string, string> = {
  NAVER: "스마트스토어",
  COUPANG: "쿠팡",
  ELEVEN_ST: "11번가",
  GMARKET: "G마켓",
  AUCTION: "옥션",
  WEMAKEPRICE: "위메프",
  TMON: "티몬",
};

export default function MappingDialog({
  order,
  initialItemId,
  open,
  onOpenChange,
  onMappingComplete,
}: MappingDialogProps) {
  const [selectedOrderItem, setSelectedOrderItem] = useState<OrderItem | null>(null);
  const [selectedErpItem, setSelectedErpItem] = useState<ErpItem | null>(null);
  const [erpItems, setErpItems] = useState<ErpItem[]>([]);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  // 품목 검색 (재고 정보 포함)
  const searchErpItems = useCallback(async (keyword: string) => {
    setLoading(true);
    try {
      const params: Record<string, string | number> = { page: 0, size: 50 };
      if (keyword) params.keyword = keyword;
      const { data } = await api.get<{ data: PageResponse<ErpItem> }>("/api/v1/erp/items", { params });
      setErpItems(data.data?.content ?? []);
    } catch {
      toast.error("ERP 품목 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  // 다이얼로그 열릴 때 초기화
  useEffect(() => {
    if (open && order.items && order.items.length > 0) {
      const targetItem = initialItemId
        ? order.items.find(i => i.id === initialItemId) || order.items[0]
        : order.items[0];
      setSelectedOrderItem(targetItem);
      // 기존 매핑이 있으면 선택된 상태로
      if (targetItem.erpProdCd) {
        setSelectedErpItem({
          id: targetItem.erpItemId || "",
          prodCd: targetItem.erpProdCd,
          prodDes: "",
          sizeDes: null,
          unit: null,
          prodType: null,
          inPrice: null,
          outPrice: null,
          barCode: null,
          classCd: null,
          classCd2: null,
          classCd3: null,
          setFlag: false,
          balFlag: true,
          lastSyncedAt: "",
          createdAt: "",
          updatedAt: "",
        } as ErpItem);
      } else {
        setSelectedErpItem(null);
      }
      setSearchKeyword("");
      setErpItems([]);
    }
  }, [open, order.items, initialItemId]);

  const handleSelectOrderItem = (item: OrderItem) => {
    setSelectedOrderItem(item);
    if (item.erpProdCd) {
      setSelectedErpItem({
        id: item.erpItemId || "",
        prodCd: item.erpProdCd,
        prodDes: "",
        sizeDes: null,
        unit: null,
        prodType: null,
        inPrice: null,
        outPrice: null,
        barCode: null,
        classCd: null,
        classCd2: null,
        classCd3: null,
        setFlag: false,
        balFlag: true,
        lastSyncedAt: "",
        createdAt: "",
        updatedAt: "",
      } as ErpItem);
    } else {
      setSelectedErpItem(null);
    }
    setSearchKeyword("");
    setErpItems([]);
  };

  const handleSaveMapping = async () => {
    if (!selectedOrderItem || !selectedErpItem) return;
    setSaving(true);
    try {
      // 재고가 가장 많은 창고의 창고코드 선택
      const inventoryList = selectedErpItem.inventoryBalances || [];
      const erpWhCd = inventoryList.length > 0 ? inventoryList[0].whCd : null;

      await api.post(`/api/v1/orders/${order.id}/items/${selectedOrderItem.id}/mapping`, {
        erpItemId: selectedErpItem.id,
        erpProdCd: selectedErpItem.prodCd,
        erpWhCd,
      });
      toast.success(`"${selectedErpItem.prodCd}" 매핑 완료`);
      setSelectedOrderItem((prev) => prev ? { ...prev, erpItemId: selectedErpItem.id, erpProdCd: selectedErpItem.prodCd } : null);
      onMappingComplete();
      onOpenChange(false);
    } catch {
      toast.error("매핑 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handleClearMapping = async () => {
    if (!selectedOrderItem) return;
    setSaving(true);
    try {
      await api.delete(`/api/v1/orders/${order.id}/items/${selectedOrderItem.id}/mapping`);
      toast.success("매핑 해제 완료");
      setSelectedOrderItem((prev) => prev ? { ...prev, erpItemId: null, erpProdCd: null } : null);
      setSelectedErpItem(null);
      onMappingComplete();
    } catch {
      toast.error("매핑 해제에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const hasMultipleItems = (order.items?.length || 0) > 1;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="!w-[800px] !max-w-[95vw] max-h-[70vh] flex flex-col p-0 gap-0 overflow-hidden">
        {/* 헤더 */}
        <DialogHeader className="px-6 py-4 border-b shrink-0 pr-12">
          <DialogTitle>상품 매핑</DialogTitle>
        </DialogHeader>

        {/* 상품 정보 (읽기 전용) */}
        <div className="px-6 py-4 border-b shrink-0 bg-muted/30">
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <Label className="text-muted-foreground text-xs">마켓플레이스</Label>
              <div className="mt-1">
                <Badge variant="outline">
                  {marketplaceLabels[order.marketplaceType] || order.marketplaceType}
                </Badge>
              </div>
            </div>
            <div>
              <Label className="text-muted-foreground text-xs">주문번호</Label>
              <div className="mt-1 font-mono">{order.marketplaceOrderId}</div>
            </div>
            <div className="col-span-2">
              <Label className="text-muted-foreground text-xs">상품</Label>
              <div className="mt-1">
                {hasMultipleItems ? (
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="outline" className="h-auto py-1.5 px-3 font-normal justify-start w-full">
                        <div className="flex-1 text-left truncate">
                          <span className="font-medium">{selectedOrderItem?.productName}</span>
                          {selectedOrderItem?.optionName && (
                            <span className="text-muted-foreground ml-1">({selectedOrderItem.optionName})</span>
                          )}
                        </div>
                        <ChevronDown className="h-4 w-4 ml-2 shrink-0" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="start" className="w-[400px]">
                      {order.items?.map((item) => (
                        <DropdownMenuItem
                          key={item.id}
                          onClick={() => handleSelectOrderItem(item)}
                          className="flex items-center justify-between"
                        >
                          <div className="truncate flex-1">
                            <span>{item.productName}</span>
                            {item.optionName && (
                              <span className="text-muted-foreground ml-1">({item.optionName})</span>
                            )}
                          </div>
                          {item.erpProdCd ? (
                            <Badge variant="default" className="ml-2 text-xs">{item.erpProdCd}</Badge>
                          ) : (
                            <Badge variant="outline" className="ml-2 text-xs text-orange-600">미매핑</Badge>
                          )}
                        </DropdownMenuItem>
                      ))}
                    </DropdownMenuContent>
                  </DropdownMenu>
                ) : (
                  <div className="truncate" title={selectedOrderItem?.productName || "-"}>
                    {selectedOrderItem?.productName || "-"}
                    {selectedOrderItem?.optionName && (
                      <span className="text-muted-foreground ml-1">({selectedOrderItem.optionName})</span>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* ERP 품목 선택 영역 */}
        <div className="px-6 py-3 border-b shrink-0">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Label className="text-base font-medium">ERP 품목 선택</Label>
              {selectedErpItem && (
                <Badge variant="default" className="font-mono">
                  {selectedErpItem.prodCd}
                  {selectedErpItem.prodDes && ` - ${selectedErpItem.prodDes}`}
                </Badge>
              )}
            </div>
            {selectedOrderItem?.erpProdCd && (
              <Button
                variant="ghost"
                size="sm"
                className="h-7 text-destructive hover:text-destructive hover:bg-destructive/10"
                onClick={handleClearMapping}
                disabled={saving}
              >
                <X className="h-4 w-4 mr-1" />
                매핑 해제
              </Button>
            )}
          </div>
        </div>

        {/* ERP 품목 검색 */}
        <div className="px-6 py-3 border-b shrink-0">
          <div className="flex items-center gap-3">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="품목코드, 품명, 바코드로 검색..."
                className="pl-9 h-10"
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && searchErpItems(searchKeyword)}
                autoFocus
              />
            </div>
            <Button onClick={() => searchErpItems(searchKeyword)} disabled={loading} className="h-10 px-6">
              {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : "검색"}
            </Button>
          </div>
        </div>

        {/* ERP 품목 목록 */}
        <div className="flex-1 overflow-y-auto">
          {loading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : erpItems.length === 0 ? (
            <div className="py-12 text-center text-sm text-muted-foreground">
              {searchKeyword ? "검색 결과가 없습니다." : "품목코드 또는 품명으로 검색하세요."}
            </div>
          ) : (
            <table className="w-full">
              <thead className="sticky top-0 bg-background border-b">
                <tr className="text-left text-sm text-muted-foreground">
                  <th className="px-6 py-3 font-medium w-24">품목코드</th>
                  <th className="px-4 py-3 font-medium">품명</th>
                  <th className="px-4 py-3 font-medium w-20">규격</th>
                  <th className="px-4 py-3 font-medium w-24">창고</th>
                  <th className="px-4 py-3 font-medium w-20 text-right">재고</th>
                  <th className="px-6 py-3 font-medium w-16"></th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {erpItems.map((erpItem) => {
                  const isSelected = selectedErpItem?.prodCd === erpItem.prodCd;
                  const inventoryList = erpItem.inventoryBalances || [];
                  return (
                    <tr
                      key={erpItem.id}
                      className={`hover:bg-muted/50 cursor-pointer ${isSelected ? "bg-green-50 dark:bg-green-950/20" : ""}`}
                      onClick={() => setSelectedErpItem(erpItem)}
                    >
                      <td className="px-6 py-3">
                        <span className="font-mono text-sm font-semibold text-primary">
                          {erpItem.prodCd}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm">
                        {erpItem.prodDes}
                      </td>
                      <td className="px-4 py-3 text-sm text-muted-foreground">
                        {erpItem.sizeDes || "-"}
                      </td>
                      <td className="px-4 py-3 text-sm">
                        {inventoryList.length === 0 ? (
                          <span className="text-muted-foreground">-</span>
                        ) : (
                          <div className="space-y-0.5">
                            {inventoryList.map((inv, idx) => (
                              <div key={idx} className="text-xs text-muted-foreground">
                                {inv.whDes || inv.whCd}
                              </div>
                            ))}
                          </div>
                        )}
                      </td>
                      <td className="px-4 py-3 text-sm text-right">
                        {inventoryList.length === 0 ? (
                          <span className="text-muted-foreground">-</span>
                        ) : (
                          <div className="space-y-0.5">
                            {inventoryList.map((inv, idx) => (
                              <div key={idx} className="text-xs font-medium">
                                {inv.balQty.toLocaleString()}
                              </div>
                            ))}
                          </div>
                        )}
                      </td>
                      <td className="px-6 py-3">
                        {isSelected && (
                          <Check className="h-5 w-5 text-green-600" />
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>

        {/* 푸터 */}
        <div className="px-6 py-4 border-t shrink-0 flex justify-end gap-3">
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={saving}>
            취소
          </Button>
          <Button onClick={handleSaveMapping} disabled={saving || !selectedErpItem}>
            {saving ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
            매핑 저장
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
