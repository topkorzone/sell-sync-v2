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
import api from "@/lib/api";
import type { Order, OrderItem, ErpItem, PageResponse } from "@/types";

interface MappingDialogProps {
  order: Order;
  initialItemId?: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onMappingComplete: () => void;
}

export default function MappingDialog({
  order,
  initialItemId,
  open,
  onOpenChange,
  onMappingComplete,
}: MappingDialogProps) {
  const [selectedItem, setSelectedItem] = useState<OrderItem | null>(null);
  const [erpItems, setErpItems] = useState<ErpItem[]>([]);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

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

  useEffect(() => {
    if (open && order.items && order.items.length > 0) {
      const targetItem = initialItemId
        ? order.items.find(i => i.id === initialItemId) || order.items[0]
        : order.items[0];
      setSelectedItem(targetItem);
      const keyword = targetItem.productName?.slice(0, 10) || "";
      setSearchKeyword(keyword);
      searchErpItems(keyword);
    }
  }, [open, order.items, initialItemId, searchErpItems]);

  const handleSelectOrderItem = (item: OrderItem) => {
    setSelectedItem(item);
    const keyword = item.productName?.slice(0, 10) || "";
    setSearchKeyword(keyword);
    searchErpItems(keyword);
  };

  const handleSaveMapping = async (erpItem: ErpItem) => {
    if (!selectedItem) return;
    setSaving(true);
    try {
      await api.post(`/api/v1/orders/${order.id}/items/${selectedItem.id}/mapping`, {
        erpItemId: erpItem.id,
        erpProdCd: erpItem.prodCd,
      });
      toast.success(`"${erpItem.prodCd}" 매핑 완료`);
      setSelectedItem((prev) => prev ? { ...prev, erpItemId: erpItem.id, erpProdCd: erpItem.prodCd } : null);
      onMappingComplete();
    } catch {
      toast.error("매핑 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handleClearMapping = async () => {
    if (!selectedItem) return;
    setSaving(true);
    try {
      await api.delete(`/api/v1/orders/${order.id}/items/${selectedItem.id}/mapping`);
      toast.success("매핑 해제 완료");
      setSelectedItem((prev) => prev ? { ...prev, erpItemId: null, erpProdCd: null } : null);
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
      <DialogContent className="max-w-4xl w-[90vw] h-[80vh] flex flex-col p-0 gap-0 overflow-hidden">
        {/* 헤더 */}
        <DialogHeader className="px-6 py-4 border-b shrink-0 pr-12">
          <DialogTitle className="flex items-center gap-3">
            <span>ERP 품목 매핑</span>
            <span className="text-sm font-normal text-muted-foreground">
              #{order.marketplaceOrderId}
            </span>
          </DialogTitle>
        </DialogHeader>

        {/* 매핑 대상 상품 */}
        {selectedItem && (
          <div className="px-6 py-3 bg-blue-50 dark:bg-blue-950/30 border-b shrink-0">
            <div className="flex items-center gap-3">
              <div className="text-sm text-muted-foreground shrink-0">매핑 대상:</div>

              {hasMultipleItems ? (
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="outline" className="h-auto py-1.5 px-3 font-normal justify-start">
                      <div className="flex-1 text-left truncate">
                        <span className="font-medium">{selectedItem.productName}</span>
                        {selectedItem.optionName && (
                          <span className="text-muted-foreground ml-1">({selectedItem.optionName})</span>
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
                <div className="flex-1 min-w-0 truncate">
                  <span className="font-medium">{selectedItem.productName}</span>
                  {selectedItem.optionName && (
                    <span className="text-muted-foreground ml-1">({selectedItem.optionName})</span>
                  )}
                </div>
              )}

              {selectedItem.erpProdCd && (
                <div className="flex items-center gap-2 shrink-0">
                  <Badge variant="default">{selectedItem.erpProdCd}</Badge>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 text-destructive hover:text-destructive hover:bg-destructive/10"
                    onClick={handleClearMapping}
                    disabled={saving}
                  >
                    <X className="h-4 w-4 mr-1" />
                    해제
                  </Button>
                </div>
              )}
            </div>
          </div>
        )}

        {/* 검색 영역 */}
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
              검색 결과가 없습니다.
            </div>
          ) : (
            <table className="w-full">
              <thead className="sticky top-0 bg-background border-b">
                <tr className="text-left text-sm text-muted-foreground">
                  <th className="px-6 py-3 font-medium w-24">품목코드</th>
                  <th className="px-4 py-3 font-medium">품명</th>
                  <th className="px-4 py-3 font-medium w-20">규격</th>
                  <th className="px-6 py-3 font-medium w-20"></th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {erpItems.map((erpItem) => {
                  const isSelected = selectedItem?.erpProdCd === erpItem.prodCd;
                  return (
                    <tr
                      key={erpItem.id}
                      className={`hover:bg-muted/50 ${isSelected ? "bg-green-50 dark:bg-green-950/20" : ""}`}
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
                      <td className="px-6 py-3">
                        <Button
                          variant={isSelected ? "default" : "outline"}
                          size="sm"
                          className="w-full"
                          onClick={() => handleSaveMapping(erpItem)}
                          disabled={saving || !selectedItem}
                        >
                          {isSelected ? (
                            <>
                              <Check className="h-4 w-4 mr-1" />
                              매핑됨
                            </>
                          ) : (
                            "선택"
                          )}
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
