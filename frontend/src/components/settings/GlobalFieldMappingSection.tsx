import { useState } from "react";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import { Plus, Trash2 } from "lucide-react";
import type {
  GlobalFieldMapping,
  ECountExtraFieldName,
  ECountFieldValueSource,
  GlobalFieldLineType,
} from "@/types";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { HelpCircle } from "lucide-react";

// ECount 필드 옵션
const ECOUNT_FIELD_OPTIONS: { value: ECountExtraFieldName; label: string; type: 'string' | 'number' }[] = [
  { value: "USER_PRICE_VAT", label: "단가(VAT포함)", type: "number" },
  { value: "REMARKS", label: "적요", type: "string" },
  { value: "P_REMARKS1", label: "적요1", type: "string" },
  { value: "P_REMARKS2", label: "적요2", type: "string" },
  { value: "P_REMARKS3", label: "적요3", type: "string" },
  { value: "P_AMT1", label: "금액1", type: "number" },
  { value: "P_AMT2", label: "금액2", type: "number" },
  { value: "ITEM_CD", label: "관리항목", type: "string" },
  { value: "ADD_TXT_01", label: "추가문자1", type: "string" },
  { value: "ADD_TXT_02", label: "추가문자2", type: "string" },
  { value: "ADD_TXT_03", label: "추가문자3", type: "string" },
  { value: "ADD_NUM_01", label: "추가숫자1", type: "number" },
  { value: "ADD_NUM_02", label: "추가숫자2", type: "number" },
  { value: "ADD_NUM_03", label: "추가숫자3", type: "number" },
];

// 값 소스 옵션
const VALUE_SOURCE_OPTIONS: { value: ECountFieldValueSource; label: string; description: string; forTypes: ('string' | 'number')[] }[] = [
  { value: "FIXED", label: "고정값", description: "직접 입력한 값 사용", forTypes: ["string", "number"] },
  { value: "UNIT_PRICE_VAT", label: "VAT포함 단가", description: "자동 계산된 VAT포함 단가", forTypes: ["number"] },
  { value: "TOTAL_AMOUNT", label: "총금액", description: "라인 총금액", forTypes: ["number"] },
  { value: "SUPPLY_AMOUNT", label: "공급가액", description: "VAT 제외 금액", forTypes: ["number"] },
  { value: "VAT_AMOUNT", label: "부가세액", description: "VAT 금액", forTypes: ["number"] },
  { value: "ORDER_ID", label: "주문번호", description: "시스템 주문 ID", forTypes: ["string"] },
  { value: "MARKETPLACE_ORDER_ID", label: "마켓주문번호", description: "마켓플레이스 주문번호", forTypes: ["string"] },
  { value: "BUYER_NAME", label: "구매자명", description: "주문자 이름", forTypes: ["string"] },
  { value: "RECEIVER_NAME", label: "수령자명", description: "받는사람 이름", forTypes: ["string"] },
  { value: "PRODUCT_NAME", label: "상품명", description: "주문 상품명", forTypes: ["string"] },
  { value: "OPTION_NAME", label: "옵션명", description: "상품 옵션명", forTypes: ["string"] },
  { value: "TEMPLATE", label: "템플릿", description: "변수 조합 (예: 주문:{orderId})", forTypes: ["string"] },
];

// 라인 타입 옵션
const LINE_TYPE_OPTIONS: { value: GlobalFieldLineType; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "PRODUCT_SALE", label: "상품판매" },
  { value: "DELIVERY_FEE", label: "배송비" },
  { value: "SALES_COMMISSION", label: "판매수수료" },
  { value: "DELIVERY_COMMISSION", label: "배송수수료" },
];

// 템플릿 변수 설명
const TEMPLATE_VARIABLES = [
  { var: "{orderId}", desc: "주문번호" },
  { var: "{marketplaceOrderId}", desc: "마켓주문번호" },
  { var: "{buyerName}", desc: "구매자명" },
  { var: "{receiverName}", desc: "수령자명" },
  { var: "{productName}", desc: "상품명" },
  { var: "{optionName}", desc: "옵션명" },
  { var: "{marketplace}", desc: "마켓플레이스명" },
];

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

interface GlobalFieldMappingSectionProps {
  fieldMappings: GlobalFieldMapping[];
  onChange: (mappings: GlobalFieldMapping[]) => void;
}

export default function GlobalFieldMappingSection({
  fieldMappings,
  onChange,
}: GlobalFieldMappingSectionProps) {
  const [expandedIndex, setExpandedIndex] = useState<number | null>(null);

  const addMapping = () => {
    // 아직 사용되지 않은 첫 번째 필드 선택
    const usedFields = new Set(fieldMappings.map((m) => m.fieldName));
    const availableField = ECOUNT_FIELD_OPTIONS.find((f) => !usedFields.has(f.value));

    if (!availableField) {
      return; // 모든 필드가 이미 사용 중
    }

    const newMapping: GlobalFieldMapping = {
      fieldName: availableField.value,
      valueSource: "FIXED",
      fixedValue: "",
      templateValue: "",
      lineTypes: ["ALL"],
    };
    onChange([...fieldMappings, newMapping]);
    setExpandedIndex(fieldMappings.length);
  };

  const updateMapping = (index: number, updates: Partial<GlobalFieldMapping>) => {
    const updated = fieldMappings.map((m, i) =>
      i === index ? { ...m, ...updates } : m
    );
    onChange(updated);
  };

  const removeMapping = (index: number) => {
    onChange(fieldMappings.filter((_, i) => i !== index));
    if (expandedIndex === index) {
      setExpandedIndex(null);
    }
  };

  const toggleLineType = (index: number, lineType: GlobalFieldLineType, checked: boolean) => {
    const mapping = fieldMappings[index];
    let newLineTypes: GlobalFieldLineType[];

    if (lineType === "ALL") {
      // ALL 선택 시 다른 모든 타입 해제
      newLineTypes = checked ? ["ALL"] : [];
    } else {
      // 개별 타입 선택 시 ALL 해제
      const filteredTypes = mapping.lineTypes.filter((t) => t !== "ALL" && t !== lineType);
      newLineTypes = checked ? [...filteredTypes, lineType] : filteredTypes;
    }

    updateMapping(index, { lineTypes: newLineTypes });
  };

  const getFieldType = (fieldName: ECountExtraFieldName): 'string' | 'number' => {
    return ECOUNT_FIELD_OPTIONS.find((f) => f.value === fieldName)?.type ?? 'string';
  };

  const getAvailableValueSources = (fieldName: ECountExtraFieldName) => {
    const fieldType = getFieldType(fieldName);
    return VALUE_SOURCE_OPTIONS.filter((s) => s.forTypes.includes(fieldType));
  };

  const getAvailableFields = (currentField: ECountExtraFieldName) => {
    const usedFields = new Set(
      fieldMappings.map((m) => m.fieldName).filter((f) => f !== currentField)
    );
    return ECOUNT_FIELD_OPTIONS.filter((f) => !usedFields.has(f.value));
  };

  const getLineTypesLabel = (lineTypes: GlobalFieldLineType[]) => {
    if (lineTypes.includes("ALL") || lineTypes.length === 0) return "전체";
    return lineTypes.map((t) => LINE_TYPE_OPTIONS.find((o) => o.value === t)?.label).join(", ");
  };

  return (
    <div className="space-y-3 border rounded-lg p-4">
      <div className="flex items-center justify-between">
        <div>
          <div className="text-sm font-medium flex items-center">
            전표 필드 설정
            <HelpTooltip text="모든 라인에 공통으로 적용되는 ECount 필드를 설정합니다. 적용 대상을 선택하여 특정 라인에만 적용할 수도 있습니다." />
          </div>
          <p className="text-xs text-muted-foreground">
            모든 라인에 적용되는 필드를 설정합니다
          </p>
        </div>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={addMapping}
          disabled={fieldMappings.length >= ECOUNT_FIELD_OPTIONS.length}
          className="h-8"
        >
          <Plus className="mr-1 h-4 w-4" />
          필드 추가
        </Button>
      </div>

      {fieldMappings.length === 0 ? (
        <div className="text-center py-6 text-sm text-muted-foreground border rounded border-dashed">
          설정된 필드가 없습니다. "필드 추가" 버튼을 클릭하여 추가하세요.
        </div>
      ) : (
        <div className="space-y-2">
          {fieldMappings.map((mapping, index) => (
            <div
              key={index}
              className="border rounded bg-muted/30"
            >
              {/* 헤더 영역 - 접힌 상태에서도 보임 */}
              <div
                className="flex items-center justify-between p-3 cursor-pointer hover:bg-muted/50"
                onClick={() => setExpandedIndex(expandedIndex === index ? null : index)}
              >
                <div className="flex items-center gap-3">
                  <span className="text-sm font-medium">
                    {ECOUNT_FIELD_OPTIONS.find((f) => f.value === mapping.fieldName)?.label || mapping.fieldName}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {VALUE_SOURCE_OPTIONS.find((s) => s.value === mapping.valueSource)?.label}
                  </span>
                  <span className="text-xs px-2 py-0.5 rounded bg-muted">
                    {getLineTypesLabel(mapping.lineTypes)}
                  </span>
                </div>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={(e) => {
                    e.stopPropagation();
                    removeMapping(index);
                  }}
                  className="h-7 w-7 text-red-500 hover:text-red-700"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>

              {/* 상세 설정 영역 - 펼쳐진 상태에서만 보임 */}
              {expandedIndex === index && (
                <div className="border-t p-3 space-y-3">
                  <div className="grid grid-cols-2 gap-3">
                    {/* 필드명 선택 */}
                    <div className="space-y-1">
                      <Label className="text-xs">필드</Label>
                      <Select
                        value={mapping.fieldName}
                        onValueChange={(v) =>
                          updateMapping(index, {
                            fieldName: v as ECountExtraFieldName,
                            valueSource: "FIXED",
                            fixedValue: "",
                            templateValue: "",
                          })
                        }
                      >
                        <SelectTrigger className="h-8 text-sm">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {getAvailableFields(mapping.fieldName).map((field) => (
                            <SelectItem key={field.value} value={field.value}>
                              {field.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>

                    {/* 값 소스 선택 */}
                    <div className="space-y-1">
                      <Label className="text-xs">값 소스</Label>
                      <Select
                        value={mapping.valueSource}
                        onValueChange={(v) =>
                          updateMapping(index, { valueSource: v as ECountFieldValueSource })
                        }
                      >
                        <SelectTrigger className="h-8 text-sm">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {getAvailableValueSources(mapping.fieldName).map((source) => (
                            <SelectItem key={source.value} value={source.value}>
                              {source.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  {/* 값 입력 */}
                  {mapping.valueSource === "FIXED" && (
                    <div className="space-y-1">
                      <Label className="text-xs">값</Label>
                      <Input
                        value={mapping.fixedValue ?? ""}
                        onChange={(e) =>
                          updateMapping(index, { fixedValue: e.target.value })
                        }
                        placeholder={
                          getFieldType(mapping.fieldName) === "number"
                            ? "숫자 입력"
                            : "텍스트 입력"
                        }
                        className="h-8 text-sm"
                      />
                    </div>
                  )}

                  {mapping.valueSource === "TEMPLATE" && (
                    <div className="space-y-1">
                      <Label className="text-xs">
                        템플릿
                        <HelpTooltip
                          text={`사용 가능한 변수: ${TEMPLATE_VARIABLES.map(
                            (t) => `${t.var}(${t.desc})`
                          ).join(", ")}`}
                        />
                      </Label>
                      <Input
                        value={mapping.templateValue ?? ""}
                        onChange={(e) =>
                          updateMapping(index, { templateValue: e.target.value })
                        }
                        placeholder="예: 주문:{orderId}"
                        className="h-8 text-sm"
                      />
                    </div>
                  )}

                  {mapping.valueSource !== "FIXED" && mapping.valueSource !== "TEMPLATE" && (
                    <div className="space-y-1">
                      <Label className="text-xs">값</Label>
                      <div className="h-8 flex items-center text-xs text-muted-foreground px-2 bg-muted rounded">
                        {VALUE_SOURCE_OPTIONS.find((s) => s.value === mapping.valueSource)?.description ?? "자동 설정"}
                      </div>
                    </div>
                  )}

                  {/* 적용 대상 선택 */}
                  <div className="space-y-2">
                    <Label className="text-xs">적용 대상</Label>
                    <div className="flex flex-wrap gap-3">
                      {LINE_TYPE_OPTIONS.map((option) => (
                        <label key={option.value} className="flex items-center gap-1.5 text-xs cursor-pointer">
                          <Checkbox
                            checked={
                              option.value === "ALL"
                                ? mapping.lineTypes.includes("ALL") || mapping.lineTypes.length === 0
                                : mapping.lineTypes.includes(option.value)
                            }
                            onCheckedChange={(checked) =>
                              toggleLineType(index, option.value, !!checked)
                            }
                          />
                          {option.label}
                        </label>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
