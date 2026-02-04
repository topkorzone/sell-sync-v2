import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface FieldMappingEditorProps {
  fieldMapping: Record<string, unknown>;
  onChange: (mapping: Record<string, unknown>) => void;
}

const ECOUNT_FIELDS = [
  { key: "WH_CD", label: "창고코드", placeholder: "예: 01", description: "ECount 창고 코드" },
  { key: "PROD_CD", label: "품목코드 기본값", placeholder: "예: MISC", description: "매핑되지 않은 품목의 기본 코드" },
  { key: "CUST", label: "거래처코드", placeholder: "예: ONLINE", description: "온라인 판매 거래처 코드" },
  { key: "DEPT_CD", label: "부서코드", placeholder: "예: 10", description: "ECount 부서 코드" },
  { key: "EMP_CD", label: "담당자코드", placeholder: "예: 001", description: "ECount 담당자 코드" },
];

export default function FieldMappingEditor({
  fieldMapping,
  onChange,
}: FieldMappingEditorProps) {
  const handleChange = (key: string, value: string) => {
    onChange({
      ...fieldMapping,
      [key]: value || undefined,
    });
  };

  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        ECount ERP에 전표를 등록할 때 사용할 기본 필드 값을 설정합니다.
      </p>
      <div className="grid gap-4">
        {ECOUNT_FIELDS.map((field) => (
          <div key={field.key} className="grid gap-2">
            <Label htmlFor={field.key}>{field.label}</Label>
            <Input
              id={field.key}
              value={String(fieldMapping[field.key] ?? "")}
              onChange={(e) => handleChange(field.key, e.target.value)}
              placeholder={field.placeholder}
            />
            <p className="text-xs text-muted-foreground">{field.description}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
