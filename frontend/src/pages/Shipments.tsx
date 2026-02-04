import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PackageOpen } from "lucide-react";

export default function Shipments() {
  return (
    <div>
      <h2 className="mb-4 text-xl font-semibold tracking-tight">배송 관리</h2>
      <Card>
        <CardHeader>
          <CardTitle className="sr-only">배송 관리</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
            <PackageOpen className="mb-3 h-10 w-10" />
            <p>배송 관리 기능이 곧 추가됩니다.</p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
