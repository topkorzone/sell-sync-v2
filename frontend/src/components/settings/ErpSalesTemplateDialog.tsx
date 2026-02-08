import { useState, useEffect, useCallback } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Loader2, ChevronRight, ChevronLeft, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import api from "@/lib/api";
import type {
  ErpConfigResponse,
  SalesLineTemplate,
  AdditionalLineTemplate,
  ErpSalesTemplateResponse,
  ErpSalesTemplateRequest,
  ApiResponse,
  MarketplaceType,
  GlobalFieldMapping,
} from "@/types";
import SalesTemplatePresets, {
  TEMPLATE_PRESETS,
  detectPresetFromLines,
  type PresetId,
  type PresetLineConfig,
} from "./SalesTemplatePresets";
import SalesTemplatePreview from "./SalesTemplatePreview";
import SalesLineDetailForm from "./SalesLineDetailForm";
import GlobalFieldMappingSection from "./GlobalFieldMappingSection";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { HelpCircle } from "lucide-react";

// 마켓플레이스 옵션
const MARKETPLACE_OPTIONS: { value: MarketplaceType; label: string }[] = [
  { value: "COUPANG", label: "쿠팡" },
  { value: "NAVER", label: "스마트스토어" },
  { value: "ELEVEN_ST", label: "11번가" },
  { value: "GMARKET", label: "G마켓" },
  { value: "AUCTION", label: "옥션" },
];

// 공통 헤더 필드 - 한글화
// 출하창고(WH_CD)는 ERP 품목 매핑 정보에서 자동으로 가져옴
const HEADER_FIELDS = [
  {
    code: "EMP_CD",
    label: "담당자",
    placeholder: "예: 00092 (선택사항)",
    help: "전표 담당자의 ERP 사원코드를 입력하세요. 비워두면 담당자 없이 전표가 생성됩니다.",
  },
  {
    code: "IO_TYPE",
    label: "거래유형",
    type: "select",
    options: [
      { value: "__DEFAULT__", label: "부가세율 적용 (기본)" },
      { value: "01", label: "과세" },
      { value: "02", label: "영세" },
      { value: "03", label: "면세" },
    ],
    help: "부가세 적용 방식을 선택하세요",
  },
];

function emptyLine(): SalesLineTemplate {
  return {
    prodCd: "",
    prodDes: "",
    qtySource: "FIXED_1",
    priceSource: "ORDER_TOTAL_PRICE",
    vatCalculation: "SUPPLY_DIV_11",
    negateAmount: false,
    skipIfZero: true,
    remarks: "",
    extraFields: {},
  };
}

function applyPresetToLine(
  base: SalesLineTemplate,
  presetConfig: PresetLineConfig | undefined
): SalesLineTemplate {
  if (!presetConfig || !presetConfig.enabled) {
    return { ...emptyLine(), prodDes: "" };
  }
  return {
    ...base,
    prodCd: presetConfig.prodCd ?? base.prodCd,
    prodDes: presetConfig.prodDes ?? base.prodDes,
    qtySource: presetConfig.qtySource ?? base.qtySource,
    priceSource: presetConfig.priceSource ?? base.priceSource,
    vatCalculation: presetConfig.vatCalculation ?? base.vatCalculation,
    negateAmount: presetConfig.negateAmount ?? base.negateAmount,
    skipIfZero: presetConfig.skipIfZero ?? base.skipIfZero,
  };
}

interface ErpSalesTemplateDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  config: ErpConfigResponse;
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

export default function ErpSalesTemplateDialog({
  open,
  onOpenChange,
  config,
}: ErpSalesTemplateDialogProps) {
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [step, setStep] = useState(1);

  // 프리셋 선택
  const [selectedPreset, setSelectedPreset] = useState<PresetId>("SIMPLE_SALE");

  // Header state (출하창고 WH_CD는 ERP 품목 매핑에서 자동으로 가져옴)
  const [defaultHeader, setDefaultHeader] = useState<Record<string, string>>({
    EMP_CD: "",
    IO_TYPE: "",
  });

  // Marketplace headers state - 체크박스 기반
  const [marketplaceHeaders, setMarketplaceHeaders] = useState<
    Record<string, Record<string, string>>
  >({});
  const [enabledMarketplaces, setEnabledMarketplaces] = useState<Set<MarketplaceType>>(
    new Set()
  );

  // Line templates state
  const [lineProductSale, setLineProductSale] = useState<SalesLineTemplate>(emptyLine());
  const [lineDeliveryFee, setLineDeliveryFee] = useState<SalesLineTemplate>(emptyLine());
  const [lineSalesCommission, setLineSalesCommission] = useState<SalesLineTemplate>(emptyLine());
  const [lineDeliveryCommission, setLineDeliveryCommission] = useState<SalesLineTemplate>(emptyLine());

  // 추가 항목 state
  const [additionalLines, setAdditionalLines] = useState<AdditionalLineTemplate[]>([]);

  // 글로벌 필드 매핑 state
  const [globalFieldMappings, setGlobalFieldMappings] = useState<GlobalFieldMapping[]>([]);

  // 프리셋 기본값 적용
  const applyPreset = useCallback((presetId: PresetId) => {
    const preset = TEMPLATE_PRESETS.find((p) => p.id === presetId);
    if (!preset || !preset.lines) return;

    setLineProductSale((prev) => applyPresetToLine(prev, preset.lines!.productSale));
    setLineDeliveryFee((prev) => applyPresetToLine(prev, preset.lines!.deliveryFee));
    setLineSalesCommission((prev) => applyPresetToLine(prev, preset.lines!.salesCommission));
    setLineDeliveryCommission((prev) => applyPresetToLine(prev, preset.lines!.deliveryCommission));
  }, []);

  const handlePresetChange = (presetId: PresetId) => {
    setSelectedPreset(presetId);
    if (presetId !== "CUSTOM") {
      applyPreset(presetId);
    }
  };

  useEffect(() => {
    if (open && config) {
      loadTemplate();
    }
  }, [open, config]);

  const loadTemplate = async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiResponse<ErpSalesTemplateResponse>>(
        `/api/v1/settings/erp/${config.id}/sales-template`
      );
      if (data.data) {
        const t = data.data;
        setDefaultHeader(t.defaultHeader ?? { EMP_CD: "", IO_TYPE: "" });
        setMarketplaceHeaders(t.marketplaceHeaders ?? {});
        setEnabledMarketplaces(new Set(Object.keys(t.marketplaceHeaders ?? {})) as Set<MarketplaceType>);
        setLineProductSale(t.lineProductSale ?? emptyLine());
        setLineDeliveryFee(t.lineDeliveryFee ?? emptyLine());
        setLineSalesCommission(t.lineSalesCommission ?? emptyLine());
        setLineDeliveryCommission(t.lineDeliveryCommission ?? emptyLine());
        setAdditionalLines(t.additionalLines ?? []);
        setGlobalFieldMappings(t.globalFieldMappings ?? []);

        // 프리셋 자동 감지
        const detected = detectPresetFromLines(
          t.lineProductSale ?? emptyLine(),
          t.lineDeliveryFee ?? emptyLine(),
          t.lineSalesCommission ?? emptyLine(),
          t.lineDeliveryCommission ?? emptyLine()
        );
        setSelectedPreset(detected);
      } else {
        resetForm();
      }
    } catch {
      // 템플릿 미설정 시 초기값 유지
      resetForm();
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setDefaultHeader({ EMP_CD: "", IO_TYPE: "" });
    setMarketplaceHeaders({});
    setEnabledMarketplaces(new Set());
    setSelectedPreset("SIMPLE_SALE");
    // 기본 프리셋 적용
    const preset = TEMPLATE_PRESETS.find((p) => p.id === "SIMPLE_SALE");
    if (preset?.lines) {
      setLineProductSale(applyPresetToLine(emptyLine(), preset.lines.productSale));
      setLineDeliveryFee(applyPresetToLine(emptyLine(), preset.lines.deliveryFee));
      setLineSalesCommission(applyPresetToLine(emptyLine(), preset.lines.salesCommission));
      setLineDeliveryCommission(applyPresetToLine(emptyLine(), preset.lines.deliveryCommission));
    }
    setAdditionalLines([]);
    setGlobalFieldMappings([]);
    setStep(1);
  };

  // 추가 항목 관리 함수
  const addAdditionalLine = () => {
    setAdditionalLines((prev) => [
      ...prev,
      {
        prodCd: "",
        prodDes: "",
        whCd: "",
        qty: 1,
        unitPrice: 0,
        vatCalculation: "SUPPLY_DIV_11",
        negateAmount: false,
        remarks: "",
        enabled: true,
      },
    ]);
  };

  const updateAdditionalLine = (index: number, field: keyof AdditionalLineTemplate, value: unknown) => {
    setAdditionalLines((prev) =>
      prev.map((line, i) => (i === index ? { ...line, [field]: value } : line))
    );
  };

  const removeAdditionalLine = (index: number) => {
    setAdditionalLines((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      // 활성화된 마켓만 포함
      const filteredMarketplaceHeaders: Record<string, Record<string, string>> = {};
      enabledMarketplaces.forEach((mkt) => {
        if (marketplaceHeaders[mkt]) {
          filteredMarketplaceHeaders[mkt] = marketplaceHeaders[mkt];
        }
      });

      const request: ErpSalesTemplateRequest = {
        marketplaceHeaders: filteredMarketplaceHeaders,
        defaultHeader,
        lineProductSale,
        lineDeliveryFee,
        lineSalesCommission,
        lineDeliveryCommission,
        additionalLines,
        globalFieldMappings,
        active: true,
      };
      await api.put(`/api/v1/settings/erp/${config.id}/sales-template`, request);
      toast.success("전표 템플릿이 저장되었습니다");
      onOpenChange(false);
    } catch {
      toast.error("템플릿 저장에 실패했습니다");
    } finally {
      setSaving(false);
    }
  };

  const updateDefaultHeader = (key: string, value: string) => {
    // IO_TYPE의 __DEFAULT__는 빈 문자열로 저장
    const actualValue = key === "IO_TYPE" && value === "__DEFAULT__" ? "" : value;
    setDefaultHeader((prev) => ({ ...prev, [key]: actualValue }));
  };

  const toggleMarketplace = (mkt: MarketplaceType, checked: boolean) => {
    const newSet = new Set(enabledMarketplaces);
    if (checked) {
      newSet.add(mkt);
      // 기본 거래처 정보 초기화
      if (!marketplaceHeaders[mkt]) {
        setMarketplaceHeaders((prev) => ({
          ...prev,
          [mkt]: { CUST: "", CUST_DES: "" },
        }));
      }
    } else {
      newSet.delete(mkt);
    }
    setEnabledMarketplaces(newSet);
  };

  const updateMarketplaceHeader = (mkt: string, key: string, value: string) => {
    setMarketplaceHeaders((prev) => ({
      ...prev,
      [mkt]: { ...(prev[mkt] ?? {}), [key]: value },
    }));
  };

  // 미리보기에 사용할 첫 번째 활성 마켓
  const previewMarketplace = Array.from(enabledMarketplaces)[0] || "COUPANG";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle>전표 템플릿 설정</DialogTitle>
          <DialogDescription>
            ECount 판매전표 자동등록에 사용될 템플릿을 설정합니다.
          </DialogDescription>
        </DialogHeader>

        {loading ? (
          <div className="flex justify-center py-12">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <div className="flex-1 min-h-0 flex flex-col">
            {/* 스텝 인디케이터 */}
            <div className="flex items-center gap-2 mb-4 px-1 shrink-0">
              <button
                type="button"
                onClick={() => setStep(1)}
                className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-sm transition-colors ${
                  step === 1
                    ? "bg-primary text-primary-foreground"
                    : "bg-muted text-muted-foreground hover:bg-muted/80"
                }`}
              >
                <span className="w-5 h-5 rounded-full bg-background/20 flex items-center justify-center text-xs font-medium">
                  1
                </span>
                기본 설정
              </button>
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
              <button
                type="button"
                onClick={() => setStep(2)}
                className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-sm transition-colors ${
                  step === 2
                    ? "bg-primary text-primary-foreground"
                    : "bg-muted text-muted-foreground hover:bg-muted/80"
                }`}
              >
                <span className="w-5 h-5 rounded-full bg-background/20 flex items-center justify-center text-xs font-medium">
                  2
                </span>
                세부 설정
              </button>
            </div>

            {/* 컨텐츠 영역 - 좌우 분할 */}
            <div className="flex gap-4 flex-1 min-h-0 overflow-hidden">
              {/* 왼쪽: 설정 폼 */}
              <div className="flex-1 overflow-y-auto pr-2 space-y-6 min-h-0">
                {step === 1 ? (
                  <>
                    {/* 프리셋 선택 */}
                    <SalesTemplatePresets
                      selectedPreset={selectedPreset}
                      onSelectPreset={handlePresetChange}
                    />

                    {/* 공통 헤더 설정 */}
                    <div className="space-y-3">
                      <div className="text-sm font-medium">공통 설정</div>
                      <div className="space-y-3">
                        {HEADER_FIELDS.map((field) => (
                          <div key={field.code} className="grid gap-1">
                            <Label className="text-xs">
                              {field.label}
                              <HelpTooltip text={field.help} />
                            </Label>
                            {field.type === "select" ? (
                              <Select
                                value={
                                  field.code === "IO_TYPE" && !defaultHeader[field.code]
                                    ? "__DEFAULT__"
                                    : defaultHeader[field.code] ?? "__DEFAULT__"
                                }
                                onValueChange={(v) => updateDefaultHeader(field.code, v)}
                              >
                                <SelectTrigger className="h-8 text-sm">
                                  <SelectValue placeholder="선택하세요" />
                                </SelectTrigger>
                                <SelectContent>
                                  {field.options!.map((opt) => (
                                    <SelectItem key={opt.value} value={opt.value}>
                                      {opt.label}
                                    </SelectItem>
                                  ))}
                                </SelectContent>
                              </Select>
                            ) : (
                              <Input
                                value={defaultHeader[field.code] ?? ""}
                                onChange={(e) =>
                                  updateDefaultHeader(field.code, e.target.value)
                                }
                                placeholder={field.placeholder}
                                className="h-8 text-sm"
                              />
                            )}
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* 마켓플레이스별 거래처 설정 */}
                    <div className="space-y-3">
                      <div className="text-sm font-medium">
                        마켓플레이스별 거래처
                        <HelpTooltip text="체크한 마켓의 주문만 전표 생성 대상이 됩니다" />
                      </div>
                      <div className="space-y-2">
                        {MARKETPLACE_OPTIONS.map((mkt) => {
                          const isEnabled = enabledMarketplaces.has(mkt.value);
                          const fields = marketplaceHeaders[mkt.value] ?? {};
                          return (
                            <div
                              key={mkt.value}
                              className="rounded-lg border p-3 space-y-2"
                            >
                              <label className="flex items-center gap-2 cursor-pointer">
                                <Checkbox
                                  checked={isEnabled}
                                  onCheckedChange={(v) =>
                                    toggleMarketplace(mkt.value, v === true)
                                  }
                                />
                                <span className="text-sm font-medium">{mkt.label}</span>
                              </label>
                              {isEnabled && (
                                <div className="grid grid-cols-2 gap-2 pl-6">
                                  <div className="grid gap-1">
                                    <Label className="text-xs">거래처코드</Label>
                                    <Input
                                      value={fields.CUST ?? ""}
                                      onChange={(e) =>
                                        updateMarketplaceHeader(
                                          mkt.value,
                                          "CUST",
                                          e.target.value
                                        )
                                      }
                                      placeholder="ERP 거래처 코드"
                                      className="h-7 text-xs"
                                    />
                                  </div>
                                  <div className="grid gap-1">
                                    <Label className="text-xs">거래처명</Label>
                                    <Input
                                      value={fields.CUST_DES ?? ""}
                                      onChange={(e) =>
                                        updateMarketplaceHeader(
                                          mkt.value,
                                          "CUST_DES",
                                          e.target.value
                                        )
                                      }
                                      placeholder={`예: ${mkt.label}(주)`}
                                      className="h-7 text-xs"
                                    />
                                  </div>
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  </>
                ) : (
                  /* Step 2: 세부 설정 */
                  <div className="space-y-6">
                    {/* 글로벌 필드 매핑 섹션 */}
                    <GlobalFieldMappingSection
                      fieldMappings={globalFieldMappings}
                      onChange={setGlobalFieldMappings}
                    />

                    <SalesLineDetailForm
                      selectedPreset={selectedPreset}
                      lineProductSale={lineProductSale}
                      setLineProductSale={setLineProductSale}
                      lineDeliveryFee={lineDeliveryFee}
                      setLineDeliveryFee={setLineDeliveryFee}
                      lineSalesCommission={lineSalesCommission}
                      setLineSalesCommission={setLineSalesCommission}
                      lineDeliveryCommission={lineDeliveryCommission}
                      setLineDeliveryCommission={setLineDeliveryCommission}
                      enabledMarketplaces={enabledMarketplaces}
                    />

                    {/* 추가 항목 섹션 */}
                    <div className="space-y-3 border-t pt-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="text-sm font-medium">추가 항목</div>
                          <div className="text-xs text-muted-foreground">
                            전표 생성 시 자동으로 포함되는 항목들입니다
                          </div>
                        </div>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={addAdditionalLine}
                        >
                          <Plus className="mr-1 h-4 w-4" />
                          항목 추가
                        </Button>
                      </div>

                      {additionalLines.length === 0 ? (
                        <div className="text-center py-4 text-sm text-muted-foreground border rounded-lg">
                          추가 항목이 없습니다
                        </div>
                      ) : (
                        <div className="space-y-3">
                          {additionalLines.map((line, index) => (
                            <div
                              key={index}
                              className={`border rounded-lg p-3 space-y-3 ${
                                !line.enabled ? "opacity-50" : ""
                              }`}
                            >
                              <div className="flex items-center justify-between">
                                <label className="flex items-center gap-2 text-sm">
                                  <Checkbox
                                    checked={line.enabled}
                                    onCheckedChange={(checked) =>
                                      updateAdditionalLine(index, "enabled", !!checked)
                                    }
                                  />
                                  활성화
                                </label>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="h-7 w-7 text-red-500 hover:text-red-700"
                                  onClick={() => removeAdditionalLine(index)}
                                >
                                  <Trash2 className="h-4 w-4" />
                                </Button>
                              </div>

                              <div className="grid grid-cols-3 gap-2">
                                <div className="space-y-1">
                                  <Label className="text-xs">품목코드</Label>
                                  <Input
                                    value={line.prodCd}
                                    onChange={(e) =>
                                      updateAdditionalLine(index, "prodCd", e.target.value)
                                    }
                                    placeholder="품목코드"
                                    className="h-8 text-sm"
                                  />
                                </div>
                                <div className="col-span-2 space-y-1">
                                  <Label className="text-xs">품목명 *</Label>
                                  <Input
                                    value={line.prodDes}
                                    onChange={(e) =>
                                      updateAdditionalLine(index, "prodDes", e.target.value)
                                    }
                                    placeholder="품목명"
                                    className="h-8 text-sm"
                                  />
                                </div>
                              </div>

                              <div className="grid grid-cols-4 gap-2">
                                <div className="space-y-1">
                                  <Label className="text-xs">창고코드</Label>
                                  <Input
                                    value={line.whCd || ""}
                                    onChange={(e) =>
                                      updateAdditionalLine(index, "whCd", e.target.value)
                                    }
                                    placeholder="창고코드"
                                    className="h-8 text-sm"
                                  />
                                </div>
                                <div className="space-y-1">
                                  <Label className="text-xs">수량</Label>
                                  <Input
                                    type="number"
                                    min={1}
                                    value={line.qty}
                                    onChange={(e) =>
                                      updateAdditionalLine(
                                        index,
                                        "qty",
                                        parseInt(e.target.value) || 1
                                      )
                                    }
                                    className="h-8 text-sm"
                                  />
                                </div>
                                <div className="space-y-1">
                                  <Label className="text-xs">단가 (VAT포함)</Label>
                                  <Input
                                    type="number"
                                    min={0}
                                    value={line.unitPrice}
                                    onChange={(e) =>
                                      updateAdditionalLine(
                                        index,
                                        "unitPrice",
                                        parseFloat(e.target.value) || 0
                                      )
                                    }
                                    className="h-8 text-sm"
                                  />
                                </div>
                                <div className="space-y-1">
                                  <Label className="text-xs">VAT 계산</Label>
                                  <Select
                                    value={line.vatCalculation}
                                    onValueChange={(v) =>
                                      updateAdditionalLine(index, "vatCalculation", v)
                                    }
                                  >
                                    <SelectTrigger className="h-8 text-sm">
                                      <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                      <SelectItem value="SUPPLY_DIV_11">
                                        과세 (1/11)
                                      </SelectItem>
                                      <SelectItem value="NO_VAT">면세</SelectItem>
                                    </SelectContent>
                                  </Select>
                                </div>
                              </div>

                              <div className="grid grid-cols-2 gap-2">
                                <div className="space-y-1">
                                  <Label className="text-xs">적요</Label>
                                  <Input
                                    value={line.remarks || ""}
                                    onChange={(e) =>
                                      updateAdditionalLine(index, "remarks", e.target.value)
                                    }
                                    placeholder="적요"
                                    className="h-8 text-sm"
                                  />
                                </div>
                                <div className="flex items-end pb-1">
                                  <label className="flex items-center gap-2 text-sm">
                                    <Checkbox
                                      checked={line.negateAmount}
                                      onCheckedChange={(checked) =>
                                        updateAdditionalLine(
                                          index,
                                          "negateAmount",
                                          !!checked
                                        )
                                      }
                                    />
                                    마이너스 금액 (수수료 등)
                                  </label>
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>

              {/* 오른쪽: 미리보기 */}
              <div className="w-72 shrink-0 min-h-0">
                <SalesTemplatePreview
                  defaultHeader={defaultHeader}
                  marketplaceHeaders={marketplaceHeaders}
                  lineProductSale={lineProductSale}
                  lineDeliveryFee={lineDeliveryFee}
                  lineSalesCommission={lineSalesCommission}
                  lineDeliveryCommission={lineDeliveryCommission}
                  additionalLines={additionalLines}
                  selectedMarketplace={previewMarketplace}
                />
              </div>
            </div>
          </div>
        )}

        <DialogFooter className="mt-4 flex items-center justify-between sm:justify-between">
          <div>
            {step === 2 && (
              <Button
                variant="ghost"
                onClick={() => setStep(1)}
                disabled={saving}
              >
                <ChevronLeft className="mr-1 h-4 w-4" />
                이전
              </Button>
            )}
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={saving}
            >
              취소
            </Button>
            {step === 1 ? (
              <Button onClick={() => setStep(2)}>
                다음
                <ChevronRight className="ml-1 h-4 w-4" />
              </Button>
            ) : (
              <Button onClick={handleSave} disabled={saving || loading}>
                {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                저장
              </Button>
            )}
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
