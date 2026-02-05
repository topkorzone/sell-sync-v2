import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Store, Truck, Server } from "lucide-react";
import StoreList from "@/components/settings/StoreList";
import CourierList from "@/components/settings/CourierList";
import ErpList from "@/components/settings/ErpList";

export default function Settings() {
  return (
    <div>
      <h2 className="mb-4 text-xl font-semibold tracking-tight">설정</h2>
      <Tabs defaultValue="marketplace">
        <TabsList>
          <TabsTrigger value="marketplace" className="gap-2">
            <Store className="h-4 w-4" />
            마켓플레이스 연동
          </TabsTrigger>
          <TabsTrigger value="courier" className="gap-2">
            <Truck className="h-4 w-4" />
            택배사 설정
          </TabsTrigger>
          <TabsTrigger value="erp" className="gap-2">
            <Server className="h-4 w-4" />
            ERP 연동
          </TabsTrigger>
        </TabsList>
        <TabsContent value="marketplace">
          <Card>
            <CardHeader>
              <CardTitle className="sr-only">마켓플레이스 연동</CardTitle>
            </CardHeader>
            <CardContent>
              <StoreList />
            </CardContent>
          </Card>
        </TabsContent>
        <TabsContent value="courier">
          <Card>
            <CardHeader>
              <CardTitle className="sr-only">택배사 설정</CardTitle>
            </CardHeader>
            <CardContent>
              <CourierList />
            </CardContent>
          </Card>
        </TabsContent>
        <TabsContent value="erp">
          <Card>
            <CardHeader>
              <CardTitle className="sr-only">ERP 연동</CardTitle>
            </CardHeader>
            <CardContent>
              <ErpList />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
