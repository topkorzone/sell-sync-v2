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
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import api from "@/lib/api";
import type { CourierType, CourierConfigResponse } from "@/types";

const COURIER_OPTIONS: { value: CourierType; label: string; enabled: boolean }[] = [
  { value: "CJ", label: "CJ대한통운", enabled: true },
  { value: "HANJIN", label: "한진택배", enabled: false },
  { value: "LOGEN", label: "로젠택배", enabled: false },
  { value: "LOTTE", label: "롯데택배", enabled: false },
  { value: "POST", label: "우체국택배", enabled: false },
];

const FRT_DV_OPTIONS = [
  { value: "02", label: "착불" },
  { value: "01", label: "선불" },
  { value: "03", label: "신용" },
];

const BOX_TYPE_OPTIONS = [
  { value: "01", label: "극소" },
  { value: "02", label: "소" },
  { value: "03", label: "중" },
  { value: "04", label: "대1" },
  { value: "05", label: "이형" },
  { value: "06", label: "취급제한" },
  { value: "07", label: "대2" },
];

interface CourierFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  config?: CourierConfigResponse | null;
  onSuccess: () => void;
}

export default function CourierFormDialog({
  open,
  onOpenChange,
  config,
  onSuccess,
}: CourierFormDialogProps) {
  const isEdit = !!config;

  const [courierType, setCourierType] = useState<CourierType>("CJ");
  const [apiKey, setApiKey] = useState("");
  const [contractCode, setContractCode] = useState("");
  const [bizRegNum, setBizRegNum] = useState("");
  const [senderName, setSenderName] = useState("");
  const [senderPhone, setSenderPhone] = useState("");
  const [senderZipcode, setSenderZipcode] = useState("");
  const [senderAddress, setSenderAddress] = useState("");
  const [senderDetailAddress, setSenderDetailAddress] = useState("");
  const [frtDvCd, setFrtDvCd] = useState("02");
  const [boxTypeCd, setBoxTypeCd] = useState("02");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      if (config) {
        setCourierType(config.courierType);
        setApiKey("");
        setContractCode(config.contractCode || "");
        setBizRegNum(config.extraConfig?.bizRegNum || "");
        setSenderName(config.senderName || "");
        setSenderPhone(config.senderPhone || "");
        setSenderZipcode(config.senderZipcode || "");
        setSenderAddress(config.senderAddress || "");
        setSenderDetailAddress(config.extraConfig?.senderDetailAddress || "");
        setFrtDvCd(config.extraConfig?.frtDvCd || "02");
        setBoxTypeCd(config.extraConfig?.boxTypeCd || "02");
      } else {
        setCourierType("CJ");
        setApiKey("");
        setContractCode("");
        setBizRegNum("");
        setSenderName("");
        setSenderPhone("");
        setSenderZipcode("");
        setSenderAddress("");
        setSenderDetailAddress("");
        setFrtDvCd("02");
        setBoxTypeCd("02");
      }
    }
  }, [open, config]);

  const handleSubmit = async () => {
    if (!contractCode || !senderName || !senderPhone) {
      toast.error("필수 항목을 입력해주세요 (고객사코드, 보내는분 이름, 전화번호)");
      return;
    }
    if (!isEdit && !apiKey) {
      toast.error("CJ-Gateway-APIKey를 입력해주세요");
      return;
    }

    const body = {
      courierType,
      apiKey,
      contractCode,
      senderName,
      senderPhone,
      senderAddress,
      senderZipcode,
      extraConfig: {
        bizRegNum,
        senderDetailAddress,
        frtDvCd,
        boxTypeCd,
      },
    };

    setSaving(true);
    try {
      if (isEdit && config) {
        await api.put(`/api/v1/settings/couriers/${config.id}`, body);
        toast.success("택배사 설정이 수정되었습니다");
      } else {
        await api.post("/api/v1/settings/couriers", body);
        toast.success("택배사가 등록되었습니다");
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
      <DialogContent className="sm:max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEdit ? "택배사 수정" : "택배사 추가"}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? "택배사 API 설정을 수정합니다."
              : "새로운 택배사를 연동합니다."}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-2">
          <div className="grid gap-2">
            <Label htmlFor="courierType">택배사</Label>
            <Select
              value={courierType}
              onValueChange={(v) => setCourierType(v as CourierType)}
              disabled={isEdit}
            >
              <SelectTrigger id="courierType">
                <SelectValue placeholder="택배사 선택" />
              </SelectTrigger>
              <SelectContent>
                {COURIER_OPTIONS.map((opt) => (
                  <SelectItem
                    key={opt.value}
                    value={opt.value}
                    disabled={!opt.enabled}
                  >
                    {opt.label}
                    {!opt.enabled && " (준비중)"}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid gap-2">
            <Label htmlFor="apiKey">CJ-Gateway-APIKey</Label>
            <Input
              id="apiKey"
              type="password"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder={
                isEdit
                  ? "변경 시 새로운 값을 입력하세요"
                  : "API Key를 입력하세요"
              }
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="grid gap-2">
              <Label htmlFor="contractCode">고객사코드 (CUST_ID)</Label>
              <Input
                id="contractCode"
                value={contractCode}
                onChange={(e) => setContractCode(e.target.value)}
                placeholder="고객사코드"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="bizRegNum">사업자번호</Label>
              <Input
                id="bizRegNum"
                value={bizRegNum}
                onChange={(e) => setBizRegNum(e.target.value)}
                placeholder="000-00-00000"
              />
            </div>
          </div>

          <hr className="my-1" />

          <div className="grid grid-cols-2 gap-4">
            <div className="grid gap-2">
              <Label htmlFor="senderName">보내는분 이름</Label>
              <Input
                id="senderName"
                value={senderName}
                onChange={(e) => setSenderName(e.target.value)}
                placeholder="홍길동"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="senderPhone">보내는분 전화번호</Label>
              <Input
                id="senderPhone"
                value={senderPhone}
                onChange={(e) => setSenderPhone(e.target.value)}
                placeholder="010-1234-5678"
              />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div className="grid gap-2">
              <Label htmlFor="senderZipcode">우편번호</Label>
              <Input
                id="senderZipcode"
                value={senderZipcode}
                onChange={(e) => setSenderZipcode(e.target.value)}
                placeholder="12345"
              />
            </div>
            <div className="col-span-2 grid gap-2">
              <Label htmlFor="senderAddress">주소</Label>
              <Input
                id="senderAddress"
                value={senderAddress}
                onChange={(e) => setSenderAddress(e.target.value)}
                placeholder="서울시 강남구 테헤란로 123"
              />
            </div>
          </div>

          <div className="grid gap-2">
            <Label htmlFor="senderDetailAddress">상세주소</Label>
            <Input
              id="senderDetailAddress"
              value={senderDetailAddress}
              onChange={(e) => setSenderDetailAddress(e.target.value)}
              placeholder="101동 202호"
            />
          </div>

          <hr className="my-1" />

          <div className="grid grid-cols-2 gap-4">
            <div className="grid gap-2">
              <Label htmlFor="frtDvCd">운임구분</Label>
              <Select value={frtDvCd} onValueChange={setFrtDvCd}>
                <SelectTrigger id="frtDvCd">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {FRT_DV_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="boxTypeCd">박스타입</Label>
              <Select value={boxTypeCd} onValueChange={setBoxTypeCd}>
                <SelectTrigger id="boxTypeCd">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {BOX_TYPE_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button type="button" onClick={handleSubmit} disabled={saving}>
            {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {isEdit ? "수정" : "등록"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
