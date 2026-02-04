import { useState, useEffect } from "react";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Loader2, Wifi } from "lucide-react";
import { toast } from "sonner";
import api from "@/lib/api";
import FieldMappingEditor from "./FieldMappingEditor";
import type {
  ErpType,
  ErpConfigRequest,
  ErpConfigResponse,
  ConnectionTestResponse,
  ApiResponse,
} from "@/types";

const ERP_OPTIONS: { value: ErpType; label: string }[] = [
  { value: "ECOUNT", label: "이카운트 ERP (ECount)" },
];

interface ErpFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  config?: ErpConfigResponse | null;
  onSuccess: () => void;
}

export default function ErpFormDialog({
  open,
  onOpenChange,
  config,
  onSuccess,
}: ErpFormDialogProps) {
  const isEdit = !!config;

  const [erpType, setErpType] = useState<ErpType>("ECOUNT");
  const [companyCode, setCompanyCode] = useState("");
  const [userId, setUserId] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [fieldMapping, setFieldMapping] = useState<Record<string, unknown>>({});
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [activeTab, setActiveTab] = useState("connection");

  useEffect(() => {
    if (open) {
      if (config) {
        setErpType(config.erpType);
        setCompanyCode(config.companyCode);
        setUserId(config.userId);
        setApiKey("");
        setFieldMapping(config.fieldMapping ?? {});
      } else {
        setErpType("ECOUNT");
        setCompanyCode("");
        setUserId("");
        setApiKey("");
        setFieldMapping({});
      }
      setActiveTab("connection");
    }
  }, [open, config]);

  const buildRequest = (): ErpConfigRequest => ({
    erpType,
    companyCode,
    userId,
    apiKey,
    fieldMapping,
  });

  const handleTestConnection = async () => {
    if (!companyCode || !userId || !apiKey) {
      toast.error("연결 설정의 모든 필드를 입력해주세요");
      return;
    }
    setTesting(true);
    try {
      const { data } = await api.post<ApiResponse<ConnectionTestResponse>>(
        "/api/v1/settings/erp/test-connection",
        { erpType, companyCode, userId, apiKey }
      );
      if (data.data?.connected) {
        toast.success(data.data.message || "연결 성공");
      } else {
        toast.error(data.data?.message || "연결 실패");
      }
    } catch {
      toast.error("연결 테스트 중 오류가 발생했습니다");
    } finally {
      setTesting(false);
    }
  };

  const handleSubmit = async () => {
    if (!companyCode || !userId || !apiKey) {
      toast.error("연결 설정의 모든 필드를 입력해주세요");
      return;
    }
    setSaving(true);
    try {
      if (isEdit && config) {
        await api.put(`/api/v1/settings/erp/${config.id}`, buildRequest());
        toast.success("ERP 설정이 수정되었습니다");
      } else {
        await api.post("/api/v1/settings/erp", buildRequest());
        toast.success("ERP가 등록되었습니다");
      }
      onOpenChange(false);
      onSuccess();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      const msg = error.response?.data?.message || "저장 중 오류가 발생했습니다";
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{isEdit ? "ERP 설정 수정" : "ERP 추가"}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? "ERP 연동 설정을 수정합니다."
              : "새로운 ERP 시스템을 연동합니다."}
          </DialogDescription>
        </DialogHeader>

        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="connection">연결 설정</TabsTrigger>
            <TabsTrigger value="mapping">필드 매핑</TabsTrigger>
          </TabsList>

          <TabsContent value="connection" className="space-y-4 py-2">
            <div className="grid gap-2">
              <Label htmlFor="erpType">ERP 타입</Label>
              <Select
                value={erpType}
                onValueChange={(v) => setErpType(v as ErpType)}
                disabled={isEdit}
              >
                <SelectTrigger id="erpType">
                  <SelectValue placeholder="ERP 선택" />
                </SelectTrigger>
                <SelectContent>
                  {ERP_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="companyCode">회사 코드</Label>
              <Input
                id="companyCode"
                value={companyCode}
                onChange={(e) => setCompanyCode(e.target.value)}
                placeholder="ECount 회사 코드를 입력하세요"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="userId">사용자 ID</Label>
              <Input
                id="userId"
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                placeholder="ECount 사용자 ID를 입력하세요"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="apiKey">API 인증키</Label>
              <Input
                id="apiKey"
                type="password"
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                placeholder={
                  isEdit
                    ? "변경 시 새로운 값을 입력하세요"
                    : "ECount API 인증키를 입력하세요"
                }
              />
            </div>
          </TabsContent>

          <TabsContent value="mapping" className="py-2">
            <FieldMappingEditor
              fieldMapping={fieldMapping}
              onChange={setFieldMapping}
            />
          </TabsContent>
        </Tabs>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button
            type="button"
            variant="outline"
            onClick={handleTestConnection}
            disabled={testing || saving}
          >
            {testing ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <Wifi className="mr-2 h-4 w-4" />
            )}
            연결 테스트
          </Button>
          <Button type="button" onClick={handleSubmit} disabled={saving || testing}>
            {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {isEdit ? "수정" : "등록"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
