import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { ChevronDown } from "lucide-react";
import { useState } from "react";
import type { SalesLineTemplate, MarketplaceType } from "@/types";
import type { PresetId } from "./SalesTemplatePresets";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { HelpCircle } from "lucide-react";
import ErpItemSearchInput from "./ErpItemSearchInput";

// VAT 옵션
const VAT_OPTIONS = [
  { value: "SUPPLY_DIV_11", label: "공급가/11 (과세)", help: "총액의 1/11을 부가세로 계산" },
  { value: "NO_VAT", label: "VAT 없음 (면세)", help: "부가세 없이 전액을 공급가로 처리" },
];

// 마켓플레이스 라벨
const MARKETPLACE_LABELS: Record<MarketplaceType, string> = {
  COUPANG: "쿠팡",
  NAVER: "스마트스토어",
  ELEVEN_ST: "11번가",
  GMARKET: "G마켓",
  AUCTION: "옥션",
  WEMAKEPRICE: "위메프",
  TMON: "티몬",
};

interface LineConfig {
  label: string;
  enabled: boolean;
  line: SalesLineTemplate;
  setLine: (line: SalesLineTemplate) => void;
  showProdCd?: boolean;
  prodCdHelp?: string;
  prodDesHelp?: string;
  useErpSearch?: boolean;
  showMarketplaceSettings?: boolean; // 마켓별 설정 표시 여부
}

interface SalesLineDetailFormProps {
  selectedPreset: PresetId;
  lineProductSale: SalesLineTemplate;
  setLineProductSale: (line: SalesLineTemplate) => void;
  lineDeliveryFee: SalesLineTemplate;
  setLineDeliveryFee: (line: SalesLineTemplate) => void;
  lineSalesCommission: SalesLineTemplate;
  setLineSalesCommission: (line: SalesLineTemplate) => void;
  lineDeliveryCommission: SalesLineTemplate;
  setLineDeliveryCommission: (line: SalesLineTemplate) => void;
  enabledMarketplaces?: Set<MarketplaceType>; // 활성화된 마켓플레이스
}

function HelpTooltip({ text }: { text: string }) {
  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <HelpCircle className="h-3.5 w-3.5 text-muted-foreground inline-block ml-1 cursor-help" />
        </TooltipTrigger>
        <TooltipContent side="top" className="max-w-xs text-xs">
          {text}
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}

function LineSettingRow({
  config,
  enabledMarketplaces
}: {
  config: LineConfig;
  enabledMarketplaces?: Set<MarketplaceType>;
}) {
  const [marketplaceOpen, setMarketplaceOpen] = useState(false);

  if (!config.enabled) return null;

  const handleErpItemSelect = (prodCd: string, prodDes: string) => {
    config.setLine({
      ...config.line,
      prodCd,
      prodDes: prodDes || config.line.prodDes,
    });
  };

  const handleMarketplaceErpItemSelect = (marketplace: string, prodCd: string, prodDes: string) => {
    const currentMarketplaceProdCds = config.line.marketplaceProdCds || {};
    config.setLine({
      ...config.line,
      marketplaceProdCds: {
        ...currentMarketplaceProdCds,
        [marketplace]: { prodCd, prodDes },
      },
    });
  };

  const getMarketplaceProdCd = (marketplace: string) => {
    return config.line.marketplaceProdCds?.[marketplace]?.prodCd || "";
  };

  const getMarketplaceProdDes = (marketplace: string) => {
    return config.line.marketplaceProdCds?.[marketplace]?.prodDes || "";
  };

  const marketplaceList = enabledMarketplaces ? Array.from(enabledMarketplaces) : [];
  const hasMarketplaceSettings = config.showMarketplaceSettings && marketplaceList.length > 1;

  return (
    <div className="space-y-3 rounded-lg border p-4 bg-card">
      <h4 className="font-medium text-sm flex items-center gap-2">
        {config.label}
        {config.line.negateAmount && (
          <span className="text-xs text-destructive font-normal">(마이너스 처리)</span>
        )}
      </h4>

      {/* 기본 설정 */}
      <div className="grid grid-cols-2 gap-3">
        {config.showProdCd && (
          <div className="grid gap-1">
            <Label className="text-xs">
              품목코드 {hasMarketplaceSettings && "(기본값)"}
              {config.prodCdHelp && <HelpTooltip text={config.prodCdHelp} />}
            </Label>
            {config.useErpSearch ? (
              <ErpItemSearchInput
                value={config.line.prodCd}
                prodDes={config.line.prodDes}
                onChange={handleErpItemSelect}
                placeholder="ERP 품목 검색 또는 직접 입력"
              />
            ) : (
              <Input
                value={config.line.prodCd}
                onChange={(e) =>
                  config.setLine({ ...config.line, prodCd: e.target.value })
                }
                placeholder="비워두면 품목코드 없이 기록"
                className="h-8 text-sm"
              />
            )}
          </div>
        )}
        <div className="grid gap-1">
          <Label className="text-xs">
            품목명 {hasMarketplaceSettings && "(기본값)"}
            {config.prodDesHelp && <HelpTooltip text={config.prodDesHelp} />}
          </Label>
          <Input
            value={config.line.prodDes}
            onChange={(e) =>
              config.setLine({ ...config.line, prodDes: e.target.value })
            }
            placeholder="전표에 표시될 품목명"
            className="h-8 text-sm"
          />
        </div>
        <div className="grid gap-1">
          <Label className="text-xs">
            부가세 계산
            <HelpTooltip text="전표 금액에서 부가세를 어떻게 계산할지 선택합니다" />
          </Label>
          <Select
            value={config.line.vatCalculation}
            onValueChange={(v) =>
              config.setLine({ ...config.line, vatCalculation: v })
            }
          >
            <SelectTrigger className="h-8 text-sm">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {VAT_OPTIONS.map((o) => (
                <SelectItem key={o.value} value={o.value}>
                  {o.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* 마켓별 품목코드 설정 */}
      {hasMarketplaceSettings && (
        <Collapsible open={marketplaceOpen} onOpenChange={setMarketplaceOpen}>
          <CollapsibleTrigger className="flex w-full items-center justify-between rounded border px-3 py-2 text-xs hover:bg-muted/50 transition-colors">
            <span className="text-muted-foreground">마켓별 품목코드 설정 (선택사항)</span>
            <ChevronDown
              className={`h-3.5 w-3.5 text-muted-foreground transition-transform ${
                marketplaceOpen ? "rotate-180" : ""
              }`}
            />
          </CollapsibleTrigger>
          <CollapsibleContent className="pt-2 space-y-2">
            <p className="text-xs text-muted-foreground px-1">
              마켓별로 다른 품목코드를 사용하려면 설정하세요. 비워두면 기본값이 사용됩니다.
            </p>
            {marketplaceList.map((marketplace) => (
              <div key={marketplace} className="grid grid-cols-[100px_1fr_1fr] gap-2 items-center">
                <span className="text-xs font-medium text-muted-foreground">
                  {MARKETPLACE_LABELS[marketplace]}
                </span>
                <ErpItemSearchInput
                  value={getMarketplaceProdCd(marketplace)}
                  prodDes={getMarketplaceProdDes(marketplace)}
                  onChange={(prodCd, prodDes) =>
                    handleMarketplaceErpItemSelect(marketplace, prodCd, prodDes)
                  }
                  placeholder="품목코드"
                />
                <Input
                  value={getMarketplaceProdDes(marketplace)}
                  onChange={(e) =>
                    handleMarketplaceErpItemSelect(
                      marketplace,
                      getMarketplaceProdCd(marketplace),
                      e.target.value
                    )
                  }
                  placeholder="품목명"
                  className="h-8 text-sm"
                />
              </div>
            ))}
          </CollapsibleContent>
        </Collapsible>
      )}

      <div className="flex items-center gap-4 pt-1">
        <label className="flex items-center gap-2 text-xs cursor-pointer">
          <Checkbox
            checked={config.line.skipIfZero}
            onCheckedChange={(v) =>
              config.setLine({ ...config.line, skipIfZero: v === true })
            }
          />
          금액이 0원이면 제외
        </label>
      </div>
    </div>
  );
}

export default function SalesLineDetailForm({
  selectedPreset,
  lineProductSale,
  setLineProductSale,
  lineDeliveryFee,
  setLineDeliveryFee,
  lineSalesCommission,
  setLineSalesCommission,
  lineDeliveryCommission,
  setLineDeliveryCommission,
  enabledMarketplaces,
}: SalesLineDetailFormProps) {
  const [isOpen, setIsOpen] = useState(selectedPreset === "CUSTOM");

  // 프리셋별 활성화된 행 판단
  const enabledLines: LineConfig[] = [];

  // 상품판매는 항상 표시 (FROM_MAPPING 사용, ERP 검색 불필요)
  enabledLines.push({
    label: "상품판매",
    enabled: true,
    line: lineProductSale,
    setLine: setLineProductSale,
    showProdCd: true,
    prodCdHelp: "FROM_MAPPING으로 설정하면 상품매핑에서 자동으로 가져옵니다",
    prodDesHelp: "상품명을 직접 입력하거나 비워두면 주문상품명 사용",
    useErpSearch: false,
    showMarketplaceSettings: false,
  });

  // 배송비 (ERP 품목 검색 지원)
  enabledLines.push({
    label: "배송비",
    enabled: true,
    line: lineDeliveryFee,
    setLine: setLineDeliveryFee,
    showProdCd: true,
    prodCdHelp: "ERP에서 배송비 품목을 검색하거나 직접 입력하세요",
    prodDesHelp: "전표에 표시될 배송비 항목명",
    useErpSearch: true,
    showMarketplaceSettings: false, // 배송비는 마켓별 설정 불필요
  });

  // 판매수수료 (프리셋에 따라, ERP 품목 검색 지원, 마켓별 설정 가능)
  if (selectedPreset === "WITH_COMMISSION" || selectedPreset === "FULL_SETTLEMENT" || selectedPreset === "CUSTOM") {
    enabledLines.push({
      label: "판매수수료",
      enabled: lineSalesCommission.priceSource === "COMMISSION_AMOUNT",
      line: lineSalesCommission,
      setLine: setLineSalesCommission,
      showProdCd: true,
      prodCdHelp: "ERP에서 수수료 품목을 검색하거나 직접 입력하세요",
      prodDesHelp: "전표에 표시될 수수료 항목명",
      useErpSearch: true,
      showMarketplaceSettings: true, // 마켓별 설정 가능
    });
  }

  // 배송수수료 (프리셋에 따라, ERP 품목 검색 지원, 마켓별 설정 가능)
  if (selectedPreset === "FULL_SETTLEMENT" || selectedPreset === "CUSTOM") {
    enabledLines.push({
      label: "배송수수료",
      enabled: lineDeliveryCommission.priceSource === "DELIVERY_COMMISSION",
      line: lineDeliveryCommission,
      setLine: setLineDeliveryCommission,
      showProdCd: true,
      prodCdHelp: "ERP에서 배송수수료 품목을 검색하거나 직접 입력하세요",
      prodDesHelp: "전표에 표시될 배송수수료 항목명",
      useErpSearch: true,
      showMarketplaceSettings: true, // 마켓별 설정 가능
    });
  }

  // CUSTOM 프리셋인 경우 항상 펼침
  if (selectedPreset === "CUSTOM") {
    return (
      <div className="space-y-3">
        <div className="text-sm font-medium">전표 행 설정</div>
        <p className="text-xs text-muted-foreground">
          각 행별로 품목코드와 품목명을 설정합니다. 품목코드를 비워두면 품목코드 없이 기록됩니다.
        </p>
        {enabledLines
          .filter((c) => c.enabled)
          .map((config) => (
            <LineSettingRow
              key={config.label}
              config={config}
              enabledMarketplaces={enabledMarketplaces}
            />
          ))}
      </div>
    );
  }

  // 다른 프리셋은 접이식
  return (
    <Collapsible open={isOpen} onOpenChange={setIsOpen}>
      <CollapsibleTrigger className="flex w-full items-center justify-between rounded-lg border px-4 py-3 text-sm font-medium hover:bg-muted/50 transition-colors">
        <span>세부 설정 (선택사항)</span>
        <ChevronDown
          className={`h-4 w-4 text-muted-foreground transition-transform ${
            isOpen ? "rotate-180" : ""
          }`}
        />
      </CollapsibleTrigger>
      <CollapsibleContent className="pt-3 space-y-3">
        <p className="text-xs text-muted-foreground px-1">
          각 행별로 품목코드와 품목명을 직접 설정할 수 있습니다. 비워두면 기본값이 사용됩니다.
        </p>
        {enabledLines
          .filter((c) => c.enabled)
          .map((config) => (
            <LineSettingRow
              key={config.label}
              config={config}
              enabledMarketplaces={enabledMarketplaces}
            />
          ))}
      </CollapsibleContent>
    </Collapsible>
  );
}
