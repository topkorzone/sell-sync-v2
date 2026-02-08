import { useState, useEffect, useCallback } from "react";
import { toast } from "sonner";
import { Search, Loader2, Check } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import api from "@/lib/api";
import type { ProductMapping, MarketplaceType, ErpItem, PageResponse } from "@/types";

interface ProductMappingDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mapping: ProductMapping | null;
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

export default function ProductMappingDialog({
  open,
  onOpenChange,
  mapping,
  onSave,
}: ProductMappingDialogProps) {
  const isEditMode = !!mapping;

  const [marketplaceType, setMarketplaceType] = useState<MarketplaceType | "">("");
  const [marketplaceProductId, setMarketplaceProductId] = useState("");
  const [marketplaceSku, setMarketplaceSku] = useState("");
  const [marketplaceProductName, setMarketplaceProductName] = useState("");
  const [marketplaceOptionName, setMarketplaceOptionName] = useState("");
  const [selectedErpItem, setSelectedErpItem] = useState<ErpItem | null>(null);
  const [erpItems, setErpItems] = useState<ErpItem[]>([]);
  const [erpSearchKeyword, setErpSearchKeyword] = useState("");
  const [loadingErpItems, setLoadingErpItems] = useState(false);
  const [saving, setSaving] = useState(false);

  // 다이얼로그 열릴 때 초기화
  useEffect(() => {
    if (open) {
      if (mapping) {
        setMarketplaceType(mapping.marketplaceType);
        setMarketplaceProductId(mapping.marketplaceProductId);
        setMarketplaceSku(mapping.marketplaceSku || "");
        setMarketplaceProductName(mapping.marketplaceProductName || "");
        setMarketplaceOptionName(mapping.marketplaceOptionName || "");
        // 기존 매핑의 ERP 품목 정보로 가상 ErpItem 생성
        if (mapping.erpProdCd) {
          setSelectedErpItem({
            id: mapping.erpItemId || "",
            prodCd: mapping.erpProdCd,
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
        }
        setErpSearchKeyword(mapping.erpProdCd || "");
        searchErpItems(mapping.erpProdCd || "");
      } else {
        setMarketplaceType("");
        setMarketplaceProductId("");
        setMarketplaceSku("");
        setMarketplaceProductName("");
        setMarketplaceOptionName("");
        setSelectedErpItem(null);
        setErpSearchKeyword("");
        setErpItems([]);
      }
    }
  }, [open, mapping]);

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
    if (!marketplaceType || !marketplaceProductId || !selectedErpItem) {
      toast.error("필수 항목을 입력해주세요.");
      return;
    }

    setSaving(true);
    try {
      await api.post("/api/v1/product-mappings", {
        marketplaceType,
        marketplaceProductId,
        marketplaceSku: marketplaceSku || null,
        marketplaceProductName: marketplaceProductName || null,
        marketplaceOptionName: marketplaceOptionName || null,
        erpItemId: selectedErpItem.id || null,
        erpProdCd: selectedErpItem.prodCd,
      });
      toast.success(isEditMode ? "매핑이 수정되었습니다." : "매핑이 등록되었습니다.");
      onOpenChange(false);
      onSave();
    } catch {
      toast.error("매핑 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="!w-[800px] !max-w-[95vw] max-h-[70vh] flex flex-col p-0 gap-0 overflow-hidden">
        {/* 헤더 */}
        <DialogHeader className="px-6 py-4 border-b shrink-0 pr-12">
          <DialogTitle>
            {isEditMode ? "상품 매핑 수정" : "상품 매핑 추가"}
          </DialogTitle>
        </DialogHeader>

        {/* 상단: 마켓플레이스 정보 입력 */}
        <div className="px-6 py-4 border-b shrink-0 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="marketplace">마켓플레이스 *</Label>
              <Select
                value={marketplaceType}
                onValueChange={(v) => setMarketplaceType(v as MarketplaceType)}
                disabled={isEditMode}
              >
                <SelectTrigger id="marketplace">
                  <SelectValue placeholder="마켓 선택" />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(marketplaceLabels).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="productId">상품 ID *</Label>
              <Input
                id="productId"
                placeholder="마켓플레이스 상품 ID"
                value={marketplaceProductId}
                onChange={(e) => setMarketplaceProductId(e.target.value)}
                disabled={isEditMode}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="sku">SKU / 옵션코드</Label>
              <Input
                id="sku"
                placeholder="옵션별 매핑 시 입력"
                value={marketplaceSku}
                onChange={(e) => setMarketplaceSku(e.target.value)}
                disabled={isEditMode}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="productName">상품명 (참조용)</Label>
              <Input
                id="productName"
                placeholder="상품명"
                value={marketplaceProductName}
                onChange={(e) => setMarketplaceProductName(e.target.value)}
              />
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="optionName">옵션명 (참조용)</Label>
            <Input
              id="optionName"
              placeholder="옵션명"
              value={marketplaceOptionName}
              onChange={(e) => setMarketplaceOptionName(e.target.value)}
            />
          </div>
        </div>

        {/* ERP 품목 선택 영역 */}
        <div className="px-6 py-3 border-b shrink-0 bg-muted/30">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Label className="text-base">ERP 품목 선택 *</Label>
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
          <Button onClick={handleSave} disabled={saving || !marketplaceType || !marketplaceProductId || !selectedErpItem}>
            {saving ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
            {isEditMode ? "수정" : "저장"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
