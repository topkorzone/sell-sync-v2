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
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { ChevronDown, Plus, Trash2 } from "lucide-react";
import type {
  ECountFieldMapping,
  ECountExtraFieldName,
  ECountFieldValueSource,
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

interface ECountFieldMappingFormProps {
  fieldMappings: ECountFieldMapping[];
  onChange: (mappings: ECountFieldMapping[]) => void;
}

export default function ECountFieldMappingForm({
  fieldMappings,
  onChange,
}: ECountFieldMappingFormProps) {
  const [isOpen, setIsOpen] = useState(fieldMappings.length > 0);

  const addMapping = () => {
    // 아직 사용되지 않은 첫 번째 필드 선택
    const usedFields = new Set(fieldMappings.map((m) => m.fieldName));
    const availableField = ECOUNT_FIELD_OPTIONS.find((f) => !usedFields.has(f.value));

    if (!availableField) {
      return; // 모든 필드가 이미 사용 중
    }

    const newMapping: ECountFieldMapping = {
      fieldName: availableField.value,
      valueSource: "FIXED",
      fixedValue: "",
      templateValue: "",
    };
    onChange([...fieldMappings, newMapping]);
  };

  const updateMapping = (index: number, updates: Partial<ECountFieldMapping>) => {
    const updated = fieldMappings.map((m, i) =>
      i === index ? { ...m, ...updates } : m
    );
    onChange(updated);
  };

  const removeMapping = (index: number) => {
    onChange(fieldMappings.filter((_, i) => i !== index));
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

  return (
    <Collapsible open={isOpen} onOpenChange={setIsOpen}>
      <CollapsibleTrigger asChild>
        <button
          type="button"
          className="flex w-full items-center justify-between rounded border px-3 py-2 text-xs hover:bg-muted/50 transition-colors"
        >
          <span className="text-muted-foreground">
            ECount 추가 필드 설정
            {fieldMappings.length > 0 && (
              <span className="ml-1 text-primary">({fieldMappings.length}개)</span>
            )}
          </span>
          <ChevronDown
            className={`h-3.5 w-3.5 text-muted-foreground transition-transform ${
              isOpen ? "rotate-180" : ""
            }`}
          />
        </button>
      </CollapsibleTrigger>
      <CollapsibleContent className="pt-2 space-y-2">
        <div className="flex items-center justify-between">
          <p className="text-xs text-muted-foreground px-1">
            단가(VAT포함), 적요 등 추가 필드를 설정합니다.
          </p>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={addMapping}
            disabled={fieldMappings.length >= ECOUNT_FIELD_OPTIONS.length}
            className="h-7 text-xs"
          >
            <Plus className="mr-1 h-3 w-3" />
            필드 추가
          </Button>
        </div>

        {fieldMappings.length === 0 ? (
          <div className="text-center py-3 text-xs text-muted-foreground border rounded border-dashed">
            설정된 추가 필드가 없습니다
          </div>
        ) : (
          <div className="space-y-2">
            {fieldMappings.map((mapping, index) => (
              <div
                key={index}
                className="grid grid-cols-[140px_140px_1fr_32px] gap-2 items-start p-2 border rounded bg-muted/30"
              >
                {/* 필드명 선택 */}
                <div className="space-y-1">
                  <Label className="text-xs">필드</Label>
                  <Select
                    value={mapping.fieldName}
                    onValueChange={(v) =>
                      updateMapping(index, {
                        fieldName: v as ECountExtraFieldName,
                        // 필드 타입이 바뀌면 값 소스도 초기화
                        valueSource: "FIXED",
                        fixedValue: "",
                        templateValue: "",
                      })
                    }
                  >
                    <SelectTrigger className="h-7 text-xs">
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
                    <SelectTrigger className="h-7 text-xs">
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

                {/* 값 입력 */}
                <div className="space-y-1">
                  {mapping.valueSource === "FIXED" && (
                    <>
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
                        className="h-7 text-xs"
                      />
                    </>
                  )}
                  {mapping.valueSource === "TEMPLATE" && (
                    <>
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
                        className="h-7 text-xs"
                      />
                    </>
                  )}
                  {mapping.valueSource !== "FIXED" &&
                    mapping.valueSource !== "TEMPLATE" && (
                      <>
                        <Label className="text-xs">값</Label>
                        <div className="h-7 flex items-center text-xs text-muted-foreground px-2 bg-muted rounded">
                          {VALUE_SOURCE_OPTIONS.find(
                            (s) => s.value === mapping.valueSource
                          )?.description ?? "자동 설정"}
                        </div>
                      </>
                    )}
                </div>

                {/* 삭제 버튼 */}
                <div className="pt-5">
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={() => removeMapping(index)}
                    className="h-7 w-7 text-red-500 hover:text-red-700"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </CollapsibleContent>
    </Collapsible>
  );
}
