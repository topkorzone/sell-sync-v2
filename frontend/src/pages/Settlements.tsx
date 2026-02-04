import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Landmark } from "lucide-react";

export default function Settlements() {
  return (
    <div>
      <h2 className="mb-4 text-xl font-semibold tracking-tight">정산 관리</h2>
      <Card>
        <CardHeader>
          <CardTitle className="sr-only">정산 관리</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
            <Landmark className="mb-3 h-10 w-10" />
            <p>정산 관리 기능이 곧 추가됩니다.</p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
