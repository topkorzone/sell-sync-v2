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
import { Loader2, Wifi } from "lucide-react";
import { toast } from "sonner";
import api from "@/lib/api";
import type {
  MarketplaceType,
  MarketplaceCredentialRequest,
  MarketplaceCredentialResponse,
  ConnectionTestResponse,
  ApiResponse,
} from "@/types";

const MARKETPLACE_OPTIONS: { value: MarketplaceType; label: string }[] = [
  { value: "COUPANG", label: "쿠팡" },
  { value: "NAVER", label: "네이버 스마트스토어" },
];

const FIELD_LABELS: Record<
  string,
  { sellerId: string; clientId: string; clientSecret: string }
> = {
  COUPANG: {
    sellerId: "Vendor ID",
    clientId: "Access Key",
    clientSecret: "Secret Key",
  },
  NAVER: {
    sellerId: "판매자 ID",
    clientId: "애플리케이션 ID",
    clientSecret: "애플리케이션 시크릿",
  },
};

interface StoreFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  credential?: MarketplaceCredentialResponse | null;
  onSuccess: () => void;
}

export default function StoreFormDialog({
  open,
  onOpenChange,
  credential,
  onSuccess,
}: StoreFormDialogProps) {
  const isEdit = !!credential;

  const [marketplaceType, setMarketplaceType] = useState<MarketplaceType>("COUPANG");
  const [sellerId, setSellerId] = useState("");
  const [clientId, setClientId] = useState("");
  const [clientSecret, setClientSecret] = useState("");
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);

  useEffect(() => {
    if (open) {
      if (credential) {
        setMarketplaceType(credential.marketplaceType);
        setSellerId(credential.sellerId);
        setClientId("");
        setClientSecret("");
      } else {
        setMarketplaceType("COUPANG");
        setSellerId("");
        setClientId("");
        setClientSecret("");
      }
    }
  }, [open, credential]);

  const labels = FIELD_LABELS[marketplaceType] ?? FIELD_LABELS.COUPANG;

  const buildRequest = (): MarketplaceCredentialRequest => ({
    marketplaceType,
    sellerId,
    clientId,
    clientSecret,
  });

  const handleTestConnection = async () => {
    if (!sellerId || !clientId || !clientSecret) {
      toast.error("모든 필드를 입력해주세요");
      return;
    }
    setTesting(true);
    try {
      const { data } = await api.post<ApiResponse<ConnectionTestResponse>>(
        "/api/v1/settings/marketplaces/test-connection",
        buildRequest()
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
    if (!sellerId || !clientId || !clientSecret) {
      toast.error("모든 필드를 입력해주세요");
      return;
    }
    setSaving(true);
    try {
      if (isEdit && credential) {
        await api.put(`/api/v1/settings/marketplaces/${credential.id}`, buildRequest());
        toast.success("스토어 정보가 수정되었습니다");
      } else {
        await api.post("/api/v1/settings/marketplaces", buildRequest());
        toast.success("스토어가 등록되었습니다");
      }
      onOpenChange(false);
      onSuccess();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { error?: { message?: string } } } };
      const msg = error.response?.data?.error?.message || "저장 중 오류가 발생했습니다";
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{isEdit ? "스토어 수정" : "스토어 추가"}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? "마켓플레이스 API 자격증명을 수정합니다."
              : "새로운 마켓플레이스를 연동합니다."}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-2">
          <div className="grid gap-2">
            <Label htmlFor="marketplaceType">마켓플레이스</Label>
            <Select
              value={marketplaceType}
              onValueChange={(v) => setMarketplaceType(v as MarketplaceType)}
              disabled={isEdit}
            >
              <SelectTrigger id="marketplaceType">
                <SelectValue placeholder="마켓플레이스 선택" />
              </SelectTrigger>
              <SelectContent>
                {MARKETPLACE_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid gap-2">
            <Label htmlFor="sellerId">{labels.sellerId}</Label>
            <Input
              id="sellerId"
              value={sellerId}
              onChange={(e) => setSellerId(e.target.value)}
              placeholder={labels.sellerId + "을(를) 입력하세요"}
            />
          </div>

          <div className="grid gap-2">
            <Label htmlFor="clientId">{labels.clientId}</Label>
            <Input
              id="clientId"
              value={clientId}
              onChange={(e) => setClientId(e.target.value)}
              placeholder={
                isEdit
                  ? "변경 시 새로운 값을 입력하세요"
                  : labels.clientId + "을(를) 입력하세요"
              }
            />
          </div>

          <div className="grid gap-2">
            <Label htmlFor="clientSecret">{labels.clientSecret}</Label>
            <Input
              id="clientSecret"
              type="password"
              value={clientSecret}
              onChange={(e) => setClientSecret(e.target.value)}
              placeholder={
                isEdit
                  ? "변경 시 새로운 값을 입력하세요"
                  : labels.clientSecret + "을(를) 입력하세요"
              }
            />
          </div>
        </div>

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
