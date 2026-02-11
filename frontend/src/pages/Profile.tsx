import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import api from "@/lib/api";

export default function Profile() {
  const { profile, refreshProfile } = useAuth();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    companyName: "",
    businessNumber: "",
    contactName: "",
    contactEmail: "",
    contactPhone: "",
  });

  useEffect(() => {
    if (profile) {
      setFormData({
        companyName: profile.companyName || "",
        businessNumber: profile.businessNumber || "",
        contactName: profile.contactName || "",
        contactEmail: profile.contactEmail || "",
        contactPhone: profile.contactPhone || "",
      });
    }
  }, [profile]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await api.put("/api/v1/profile", formData);
      await refreshProfile();
      toast.success("프로필이 저장되었습니다");
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "프로필 저장에 실패했습니다");
    } finally {
      setLoading(false);
    }
  };

  if (!profile) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl">
      <h2 className="mb-4 text-xl font-semibold tracking-tight">내 정보</h2>

      <div className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>계정 정보</CardTitle>
            <CardDescription>로그인에 사용되는 계정 정보입니다</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label>이메일</Label>
              <Input value={profile.email} disabled />
            </div>
            <div className="space-y-2">
              <Label>역할</Label>
              <Input value={profile.role === "admin" ? "관리자" : "사용자"} disabled />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>회사 정보</CardTitle>
            <CardDescription>사업자 및 연락처 정보를 관리합니다</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="companyName">회사명</Label>
                <Input
                  id="companyName"
                  name="companyName"
                  value={formData.companyName}
                  onChange={handleChange}
                  placeholder="회사명을 입력하세요"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="businessNumber">사업자등록번호</Label>
                <Input
                  id="businessNumber"
                  name="businessNumber"
                  value={formData.businessNumber}
                  onChange={handleChange}
                  placeholder="000-00-00000"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="contactName">담당자명</Label>
                <Input
                  id="contactName"
                  name="contactName"
                  value={formData.contactName}
                  onChange={handleChange}
                  placeholder="담당자명을 입력하세요"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="contactEmail">연락처 이메일</Label>
                <Input
                  id="contactEmail"
                  name="contactEmail"
                  type="email"
                  value={formData.contactEmail}
                  onChange={handleChange}
                  placeholder="contact@example.com"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="contactPhone">연락처 전화번호</Label>
                <Input
                  id="contactPhone"
                  name="contactPhone"
                  value={formData.contactPhone}
                  onChange={handleChange}
                  placeholder="010-0000-0000"
                />
              </div>
              <Button type="submit" disabled={loading}>
                {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                저장
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
