import { useState, useEffect, useCallback } from "react";
import { toast } from "sonner";
import { Search, Loader2, Check } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import api from "@/lib/api";
import type { UnmappedProduct, MarketplaceType, ErpItem, PageResponse } from "@/types";

interface UnmappedMappingDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  product: UnmappedProduct | null;
  onSave: () => void;
}

const marketplaceLabels: Record<MarketplaceType, string> = {
  NAVER: "스마트스토어",
  COUPANG: "쿠팡",
  ELEVEN_ST: "11번가",
  GMARKET: "G마켓",
  AUCTION: "옥션",
  WEMAKEPRICE: "위메프",
  TMON: "티몬",
};

export default function UnmappedMappingDialog({
  open,
  onOpenChange,
  product,
  onSave,
}: UnmappedMappingDialogProps) {
  const [selectedErpItem, setSelectedErpItem] = useState<ErpItem | null>(null);
  const [erpItems, setErpItems] = useState<ErpItem[]>([]);
  const [erpSearchKeyword, setErpSearchKeyword] = useState("");
  const [loadingErpItems, setLoadingErpItems] = useState(false);
  const [saving, setSaving] = useState(false);

  // 다이얼로그 열릴 때 초기화
  useEffect(() => {
    if (open) {
      setSelectedErpItem(null);
      setErpSearchKeyword("");
      setErpItems([]);
    }
  }, [open, product]);

  const searchErpItems = useCallback(async (keyword: string) => {
    setLoadingErpItems(true);
    try {
      const params: Record<string, string | number> = { page: 0, size: 50 };
      if (keyword) params.keyword = keyword;
      const { data } = await api.get<{ data: PageResponse<ErpItem> }>("/api/v1/erp/items", { params });
      setErpItems(data.data?.content ?? []);
    } catch {
      toast.error("ERP 품목 목록을 불러오지 못했습니다.");
    } finally {
      setLoadingErpItems(false);
    }
  }, []);

  const handleSave = async () => {
    if (!product || !selectedErpItem) {
      toast.error("ERP 품목을 선택해주세요.");
      return;
    }

    setSaving(true);
    try {
      await api.post("/api/v1/product-mappings", {
        marketplaceType: product.marketplaceType,
        marketplaceProductId: product.marketplaceProductId,
        marketplaceSku: product.marketplaceSku || null,
        marketplaceProductName: product.productName || null,
        marketplaceOptionName: product.optionName || null,
        erpItemId: selectedErpItem.id || null,
        erpProdCd: selectedErpItem.prodCd,
      });
      toast.success("매핑이 등록되었습니다.");
      onOpenChange(false);
      onSave();
    } catch {
      toast.error("매핑 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  if (!product) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl w-[85vw] max-h-[80vh] flex flex-col p-0 gap-0 overflow-hidden">
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
                  {marketplaceLabels[product.marketplaceType] || product.marketplaceType}
                </Badge>
              </div>
            </div>
            <div>
              <Label className="text-muted-foreground text-xs">상품 ID</Label>
              <div className="mt-1 font-mono">{product.marketplaceProductId}</div>
            </div>
            <div>
              <Label className="text-muted-foreground text-xs">상품명</Label>
              <div className="mt-1 truncate" title={product.productName || "-"}>
                {product.productName || "-"}
              </div>
            </div>
            <div>
              <Label className="text-muted-foreground text-xs">SKU / 옵션</Label>
              <div className="mt-1">
                {product.marketplaceSku || "-"}
                {product.optionName && (
                  <span className="text-muted-foreground ml-2">({product.optionName})</span>
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
                value={erpSearchKeyword}
                onChange={(e) => setErpSearchKeyword(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && searchErpItems(erpSearchKeyword)}
                autoFocus
              />
            </div>
            <Button onClick={() => searchErpItems(erpSearchKeyword)} disabled={loadingErpItems} className="h-10 px-6">
              {loadingErpItems ? <Loader2 className="h-4 w-4 animate-spin" /> : "검색"}
            </Button>
          </div>
        </div>

        {/* ERP 품목 목록 */}
        <div className="flex-1 overflow-y-auto">
          {loadingErpItems ? (
            <div className="flex justify-center py-12">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : erpItems.length === 0 ? (
            <div className="py-12 text-center text-sm text-muted-foreground">
              {erpSearchKeyword ? "검색 결과가 없습니다." : "품목코드 또는 품명으로 검색하세요."}
            </div>
          ) : (
            <table className="w-full">
              <thead className="sticky top-0 bg-background border-b">
                <tr className="text-left text-sm text-muted-foreground">
                  <th className="px-6 py-3 font-medium w-28">품목코드</th>
                  <th className="px-4 py-3 font-medium">품명</th>
                  <th className="px-4 py-3 font-medium w-24">규격</th>
                  <th className="px-6 py-3 font-medium w-20"></th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {erpItems.map((erpItem) => {
                  const isSelected = selectedErpItem?.prodCd === erpItem.prodCd;
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
          <Button onClick={handleSave} disabled={saving || !selectedErpItem}>
            {saving ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
            매핑 저장
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
