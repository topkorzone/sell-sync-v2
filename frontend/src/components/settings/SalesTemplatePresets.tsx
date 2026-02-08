import { cn } from "@/lib/utils";
import { Package, Coins, BarChart3, Settings2, Check } from "lucide-react";
import type { SalesLineTemplate } from "@/types";

export type PresetId = "SIMPLE_SALE" | "WITH_COMMISSION" | "FULL_SETTLEMENT" | "CUSTOM";

export interface PresetLineConfig {
  enabled: boolean;
  prodCd?: string;
  prodDes?: string;
  qtySource?: string;
  priceSource?: string;
  vatCalculation?: string;
  negateAmount?: boolean;
  skipIfZero?: boolean;
}

export interface TemplatePreset {
  id: PresetId;
  name: string;
  description: string;
  icon: typeof Package;
  lines: {
    productSale: PresetLineConfig;
    deliveryFee: PresetLineConfig;
    salesCommission: PresetLineConfig;
    deliveryCommission: PresetLineConfig;
  } | null;
}

export const TEMPLATE_PRESETS: TemplatePreset[] = [
  {
    id: "SIMPLE_SALE",
    name: "일반 판매",
    description: "상품 판매 + 배송비만 기록",
    icon: Package,
    lines: {
      productSale: {
        enabled: true,
        prodCd: "FROM_MAPPING",
        prodDes: "주문상품",
        qtySource: "ORDER_QUANTITY",
        priceSource: "ORDER_TOTAL_PRICE",
        vatCalculation: "SUPPLY_DIV_11",
        negateAmount: false,
        skipIfZero: false,
      },
      deliveryFee: {
        enabled: true,
        prodCd: "",
        prodDes: "택배비",
        qtySource: "FIXED_1",
        priceSource: "ORDER_DELIVERY_FEE",
        vatCalculation: "SUPPLY_DIV_11",
        negateAmount: false,
        skipIfZero: true,
      },
      salesCommission: { enabled: false },
      deliveryCommission: { enabled: false },
    },
  },
  {
    id: "WITH_COMMISSION",
    name: "수수료 포함",
    description: "상품 + 배송비 + 판매수수료",
    icon: Coins,
    lines: {
      productSale: {
        enabled: true,
        prodCd: "FROM_MAPPING",
        prodDes: "주문상품",
        qtySource: "ORDER_QUANTITY",
        priceSource: "ORDER_TOTAL_PRICE",
        vatCalculation: "SUPPLY_DIV_11",
        negateAmount: false,
        skipIfZero: false,
      },
      deliveryFee: {
        enabled: true,
        prodCd: "",
        prodDes: "택배비",
        qtySource: "FIXED_1",
        priceSource: "ORDER_DELIVERY_FEE",
        vatCalculation: "SUPPLY_DIV_11",
        negateAmount: false,
        skipIfZero: true,
      },
      salesCommission: {
        enabled: true,
        prodCd: "",
        prodDes: "판매수수료",
        qtySource: "FIXED_1",
        priceSource: "COMMISSION_AMOUNT",
        vatCalculation: "SUPPLY_DIV_11",
        negateAmount: true,
        skipIfZero: true,
      },
      deliveryCommission: { enabled: false },
    },
  },
  {
    id: "FULL_SETTLEMENT",
    name: "전체 정산",
    description: "상품 + 배송비 + 모든 수수료",
    icon: BarChart3,
    lines: {
      productSale: {
        enabled: true,
        prodCd: "FROM_MAPPING",
        prodDes: "주문상품",
        qtySource: "ORDER_QUANTITY",
        priceSource: "ORDER_TOTAL_PRICE",
        vatCalculation: "SUPPLY_DIV_11",
        negateAmount: false,
        skipIfZero: false,
      },
      deliveryFee: {
        enabled: true,
        prodCd: "",
        prodDes: "택배비",
        qtySource: "FIXED_1",
        priceSource: "ORDER_DELIVERY_FEE",
        vatCalculation: "SUPPLY_DIV_11",
        negateAmount: false,
        skipIfZero: true,
      },
      salesCommission: {
        enabled: true,
        prodCd: "",
        prodDes: "판매수수료",
        qtySource: "FIXED_1",
        priceSource: "COMMISSION_AMOUNT",
        vatCalculation: "SUPPLY_DIV_11",
        negateAmount: true,
        skipIfZero: true,
      },
      deliveryCommission: {
        enabled: true,
        prodCd: "",
        prodDes: "배송수수료",
        qtySource: "FIXED_1",
        priceSource: "DELIVERY_COMMISSION",
        vatCalculation: "SUPPLY_DIV_11",
        negateAmount: true,
        skipIfZero: true,
      },
    },
  },
  {
    id: "CUSTOM",
    name: "직접 설정",
    description: "각 행을 개별적으로 설정",
    icon: Settings2,
    lines: null,
  },
];

// 현재 템플릿 설정으로부터 프리셋 ID 감지
export function detectPresetFromLines(
  productSale: SalesLineTemplate,
  deliveryFee: SalesLineTemplate,
  salesCommission: SalesLineTemplate,
  deliveryCommission: SalesLineTemplate
): PresetId {
  // 판매수수료와 배송수수료가 모두 비활성화 (prodDes가 비어있거나 skipIfZero가 true이면서 관련 설정이 없는 경우)
  const hasProductSale = productSale.prodCd || productSale.prodDes;
  const hasDeliveryFee = deliveryFee.prodDes;
  const hasSalesCommission = salesCommission.prodDes && salesCommission.priceSource === "COMMISSION_AMOUNT";
  const hasDeliveryCommission = deliveryCommission.prodDes && deliveryCommission.priceSource === "DELIVERY_COMMISSION";

  if (hasProductSale && hasDeliveryFee && hasSalesCommission && hasDeliveryCommission) {
    return "FULL_SETTLEMENT";
  }
  if (hasProductSale && hasDeliveryFee && hasSalesCommission && !hasDeliveryCommission) {
    return "WITH_COMMISSION";
  }
  if (hasProductSale && hasDeliveryFee && !hasSalesCommission && !hasDeliveryCommission) {
    return "SIMPLE_SALE";
  }
  return "CUSTOM";
}

interface SalesTemplatePresetsProps {
  selectedPreset: PresetId;
  onSelectPreset: (presetId: PresetId) => void;
}

export default function SalesTemplatePresets({
  selectedPreset,
  onSelectPreset,
}: SalesTemplatePresetsProps) {
  return (
    <div className="space-y-3">
      <div className="text-sm font-medium">전표 유형 선택</div>
      <div className="grid grid-cols-2 gap-3">
        {TEMPLATE_PRESETS.map((preset) => {
          const Icon = preset.icon;
          const isSelected = selectedPreset === preset.id;
          return (
            <button
              key={preset.id}
              type="button"
              onClick={() => onSelectPreset(preset.id)}
              className={cn(
                "relative flex flex-col items-start gap-2 rounded-lg border-2 p-4 text-left transition-all hover:border-primary/50",
                isSelected
                  ? "border-primary bg-primary/5"
                  : "border-border bg-card"
              )}
            >
              {isSelected && (
                <div className="absolute right-2 top-2">
                  <Check className="h-4 w-4 text-primary" />
                </div>
              )}
              <div
                className={cn(
                  "flex h-10 w-10 items-center justify-center rounded-lg",
                  isSelected ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground"
                )}
              >
                <Icon className="h-5 w-5" />
              </div>
              <div>
                <div className="font-medium text-sm">{preset.name}</div>
                <div className="text-xs text-muted-foreground mt-0.5">
                  {preset.description}
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
