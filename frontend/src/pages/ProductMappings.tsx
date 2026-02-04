import { useEffect, useState, useCallback } from "react";
import { toast } from "sonner";
import { Search, Loader2, Trash2, Link } from "lucide-react";
import ProductMappingDialog from "@/components/product-mapping/ProductMappingDialog";
import UnmappedMappingDialog from "@/components/product-mapping/UnmappedMappingDialog";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import api from "@/lib/api";
import type { ProductMapping, MarketplaceType, PageResponse, UnmappedProduct } from "@/types";

const marketplaceLabels: Record<MarketplaceType, string> = {
  NAVER: "스마트스토어",
  COUPANG: "쿠팡",
  ELEVEN_ST: "11번가",
  GMARKET: "G마켓",
  AUCTION: "옥션",
  WEMAKEPRICE: "위메프",
  TMON: "티몬",
};

export default function ProductMappings() {
  const [activeTab, setActiveTab] = useState<"unmapped" | "mappings">("unmapped");

  // 미매핑 상품 상태
  const [unmappedProducts, setUnmappedProducts] = useState<UnmappedProduct[]>([]);
  const [unmappedLoading, setUnmappedLoading] = useState(true);
  const [unmappedTotal, setUnmappedTotal] = useState(0);
  const [unmappedPage, setUnmappedPage] = useState(0);
  const [unmappedMarketplaceFilter, setUnmappedMarketplaceFilter] = useState<string>("");
  const [selectedUnmapped, setSelectedUnmapped] = useState<UnmappedProduct | null>(null);
  const [unmappedDialogOpen, setUnmappedDialogOpen] = useState(false);

  // 매핑 목록 상태
  const [mappings, setMappings] = useState<ProductMapping[]>([]);
  const [loading, setLoading] = useState(true);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [marketplaceFilter, setMarketplaceFilter] = useState<string>("");
  const [keyword, setKeyword] = useState("");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingMapping, setEditingMapping] = useState<ProductMapping | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deletingMapping, setDeletingMapping] = useState<ProductMapping | null>(null);
  const [deleting, setDeleting] = useState(false);

  // 미매핑 상품 목록 조회
  const fetchUnmappedProducts = useCallback(async () => {
    setUnmappedLoading(true);
    try {
      const params: Record<string, string | number> = { page: unmappedPage, size: 20 };
      if (unmappedMarketplaceFilter) params.marketplace = unmappedMarketplaceFilter;
      const { data } = await api.get<{ data: PageResponse<UnmappedProduct> }>(
        "/api/v1/product-mappings/unmapped",
        { params }
      );
      setUnmappedProducts(data.data?.content ?? []);
      setUnmappedTotal(data.data?.totalElements ?? 0);
    } catch {
      toast.error("미매핑 상품 목록을 불러오지 못했습니다.");
    } finally {
      setUnmappedLoading(false);
    }
  }, [unmappedPage, unmappedMarketplaceFilter]);

  // 매핑 목록 조회
  const fetchMappings = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string | number> = { page, size: 20 };
      if (marketplaceFilter) params.marketplace = marketplaceFilter;
      if (keyword) params.keyword = keyword;
      const { data } = await api.get<{ data: PageResponse<ProductMapping> }>(
        "/api/v1/product-mappings",
        { params }
      );
      setMappings(data.data?.content ?? []);
      setTotal(data.data?.totalElements ?? 0);
    } catch {
      toast.error("매핑 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [page, marketplaceFilter, keyword]);

  useEffect(() => {
    if (activeTab === "unmapped") {
      fetchUnmappedProducts();
    } else {
      fetchMappings();
    }
  }, [activeTab, fetchUnmappedProducts, fetchMappings]);

  const handleOpenUnmappedDialog = (product: UnmappedProduct) => {
    setSelectedUnmapped(product);
    setUnmappedDialogOpen(true);
  };

  const handleOpenEditDialog = (mapping: ProductMapping) => {
    setEditingMapping(mapping);
    setDialogOpen(true);
  };

  const handleOpenDeleteDialog = (mapping: ProductMapping) => {
    setDeletingMapping(mapping);
    setDeleteDialogOpen(true);
  };

  const handleDelete = async () => {
    if (!deletingMapping) return;
    setDeleting(true);
    try {
      await api.delete(`/api/v1/product-mappings/${deletingMapping.id}`);
      toast.success("매핑이 삭제되었습니다.");
      setDeleteDialogOpen(false);
      setDeletingMapping(null);
      fetchMappings();
    } catch {
      toast.error("매핑 삭제에 실패했습니다.");
    } finally {
      setDeleting(false);
    }
  };

  const handleMappingSaved = () => {
    fetchUnmappedProducts();
    fetchMappings();
    // 매핑 저장 후 매핑 목록 탭으로 이동
    setActiveTab("mappings");
  };

  const unmappedTotalPages = Math.ceil(unmappedTotal / 20);
  const totalPages = Math.ceil(total / 20);

  return (
    <div>
      <div className="mb-4">
        <h2 className="text-xl font-semibold tracking-tight">상품 매핑</h2>
        <p className="text-sm text-muted-foreground mt-1">
          마켓플레이스 상품과 ERP 품목을 연결합니다
        </p>
      </div>

      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as "unmapped" | "mappings")}>
        <TabsList className="mb-4">
          <TabsTrigger value="unmapped" className="gap-2">
            미매핑 상품
            {unmappedTotal > 0 && (
              <Badge variant="destructive" className="h-5 px-1.5 text-xs">
                {unmappedTotal}
              </Badge>
            )}
          </TabsTrigger>
          <TabsTrigger value="mappings">매핑 목록</TabsTrigger>
        </TabsList>

        {/* 미매핑 상품 탭 */}
        <TabsContent value="unmapped">
          <Card>
            <CardContent className="pt-6">
              <div className="mb-4 flex flex-wrap items-center gap-3">
                <Select
                  value={unmappedMarketplaceFilter}
                  onValueChange={(v) => {
                    setUnmappedMarketplaceFilter(v === "all" ? "" : v);
                    setUnmappedPage(0);
                  }}
                >
                  <SelectTrigger className="w-[150px]">
                    <SelectValue placeholder="마켓 선택" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">전체 마켓</SelectItem>
                    {Object.entries(marketplaceLabels).map(([value, label]) => (
                      <SelectItem key={value} value={value}>
                        {label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <span className="text-sm text-muted-foreground">
                  주문에 포함된 상품 중 ERP 매핑이 필요한 상품입니다
                </span>
              </div>

              {unmappedLoading ? (
                <div className="flex justify-center py-12">
                  <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                </div>
              ) : unmappedProducts.length === 0 ? (
                <div className="py-12 text-center">
                  <div className="text-sm text-muted-foreground">
                    미매핑 상품이 없습니다
                  </div>
                  <p className="text-xs text-muted-foreground mt-1">
                    모든 주문 상품이 ERP 품목과 매핑되어 있습니다
                  </p>
                </div>
              ) : (
                <>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-[100px]">마켓</TableHead>
                        <TableHead className="w-[140px]">상품 ID</TableHead>
                        <TableHead className="min-w-[180px]">상품명</TableHead>
                        <TableHead className="w-[140px]">SKU/옵션코드</TableHead>
                        <TableHead className="w-[100px] text-center">주문수</TableHead>
                        <TableHead className="w-[100px]"></TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {unmappedProducts.map((product, idx) => (
                        <TableRow key={`${product.marketplaceType}-${product.marketplaceProductId}-${product.marketplaceSku ?? idx}`}>
                          <TableCell>
                            <Badge variant="outline">
                              {marketplaceLabels[product.marketplaceType] || product.marketplaceType}
                            </Badge>
                          </TableCell>
                          <TableCell className="font-mono text-sm truncate max-w-[140px]">
                            {product.marketplaceProductId}
                          </TableCell>
                          <TableCell>
                            <div className="truncate max-w-[200px]" title={product.productName || "-"}>
                              {product.productName || "-"}
                            </div>
                            {product.optionName && (
                              <div className="text-xs text-muted-foreground truncate" title={product.optionName}>
                                옵션: {product.optionName}
                              </div>
                            )}
                          </TableCell>
                          <TableCell className="font-mono text-sm">
                            {product.marketplaceSku || "-"}
                          </TableCell>
                          <TableCell className="text-center">
                            <Badge variant="secondary">{product.orderCount}</Badge>
                          </TableCell>
                          <TableCell>
                            <Button
                              size="sm"
                              onClick={() => handleOpenUnmappedDialog(product)}
                            >
                              <Link className="h-4 w-4 mr-1" />
                              매핑
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                  <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                    <span>총 {unmappedTotal}건</span>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={unmappedPage === 0}
                        onClick={() => setUnmappedPage((p) => p - 1)}
                      >
                        이전
                      </Button>
                      <span className="flex items-center px-2">
                        {unmappedPage + 1} / {unmappedTotalPages || 1}
                      </span>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={unmappedPage + 1 >= unmappedTotalPages}
                        onClick={() => setUnmappedPage((p) => p + 1)}
                      >
                        다음
                      </Button>
                    </div>
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* 매핑 목록 탭 */}
        <TabsContent value="mappings">
          <Card>
            <CardContent className="pt-6">
              <div className="mb-4 flex flex-wrap items-center gap-3">
                <Select
                  value={marketplaceFilter}
                  onValueChange={(v) => {
                    setMarketplaceFilter(v === "all" ? "" : v);
                    setPage(0);
                  }}
                >
                  <SelectTrigger className="w-[150px]">
                    <SelectValue placeholder="마켓 선택" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">전체 마켓</SelectItem>
                    {Object.entries(marketplaceLabels).map(([value, label]) => (
                      <SelectItem key={value} value={value}>
                        {label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    placeholder="상품명, 품목코드 검색"
                    className="w-[260px] pl-9"
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        setPage(0);
                        fetchMappings();
                      }
                    }}
                  />
                </div>
              </div>
              {loading ? (
                <div className="flex justify-center py-12">
                  <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                </div>
              ) : mappings.length === 0 ? (
                <p className="py-12 text-center text-sm text-muted-foreground">
                  등록된 매핑이 없습니다.
                </p>
              ) : (
                <>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-[100px]">마켓</TableHead>
                        <TableHead className="w-[160px]">상품 ID</TableHead>
                        <TableHead className="min-w-[180px]">상품명</TableHead>
                        <TableHead className="w-[120px]">SKU/옵션코드</TableHead>
                        <TableHead className="w-[100px]">ERP 품목코드</TableHead>
                        <TableHead className="w-[80px] text-center">사용횟수</TableHead>
                        <TableHead className="w-[80px] text-center">자동생성</TableHead>
                        <TableHead className="w-[80px]"></TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {mappings.map((mapping) => (
                        <TableRow
                          key={mapping.id}
                          className="cursor-pointer hover:bg-muted/50"
                          onClick={() => handleOpenEditDialog(mapping)}
                        >
                          <TableCell>
                            <Badge variant="outline">
                              {marketplaceLabels[mapping.marketplaceType] || mapping.marketplaceType}
                            </Badge>
                          </TableCell>
                          <TableCell className="font-mono text-sm truncate max-w-[160px]">
                            {mapping.marketplaceProductId}
                          </TableCell>
                          <TableCell>
                            <div className="truncate max-w-[200px]" title={mapping.marketplaceProductName || "-"}>
                              {mapping.marketplaceProductName || "-"}
                            </div>
                            {mapping.marketplaceOptionName && (
                              <div className="text-xs text-muted-foreground truncate" title={mapping.marketplaceOptionName}>
                                옵션: {mapping.marketplaceOptionName}
                              </div>
                            )}
                          </TableCell>
                          <TableCell className="font-mono text-sm">
                            {mapping.marketplaceSku || "-"}
                          </TableCell>
                          <TableCell>
                            <Badge variant="default" className="font-mono">
                              {mapping.erpProdCd}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-center">
                            {mapping.useCount}
                          </TableCell>
                          <TableCell className="text-center">
                            {mapping.autoCreated ? (
                              <Badge variant="secondary">자동</Badge>
                            ) : (
                              <Badge variant="outline">수동</Badge>
                            )}
                          </TableCell>
                          <TableCell>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 text-destructive hover:text-destructive hover:bg-destructive/10"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleOpenDeleteDialog(mapping);
                              }}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                  <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                    <span>총 {total}건</span>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={page === 0}
                        onClick={() => setPage((p) => p - 1)}
                      >
                        이전
                      </Button>
                      <span className="flex items-center px-2">
                        {page + 1} / {totalPages || 1}
                      </span>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={page + 1 >= totalPages}
                        onClick={() => setPage((p) => p + 1)}
                      >
                        다음
                      </Button>
                    </div>
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* 미매핑 상품 매핑 다이얼로그 */}
      <UnmappedMappingDialog
        open={unmappedDialogOpen}
        onOpenChange={setUnmappedDialogOpen}
        product={selectedUnmapped}
        onSave={handleMappingSaved}
      />

      {/* 매핑 수정 다이얼로그 */}
      <ProductMappingDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        mapping={editingMapping}
        onSave={handleMappingSaved}
      />

      {/* 삭제 확인 다이얼로그 */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>매핑 삭제</AlertDialogTitle>
            <AlertDialogDescription>
              이 매핑을 삭제하시겠습니까? 삭제된 매핑은 복구할 수 없습니다.
              {deletingMapping && (
                <div className="mt-2 p-2 bg-muted rounded text-sm">
                  <div>상품 ID: {deletingMapping.marketplaceProductId}</div>
                  <div>ERP 품목코드: {deletingMapping.erpProdCd}</div>
                </div>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleting}>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              disabled={deleting}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleting ? <Loader2 className="h-4 w-4 animate-spin" /> : "삭제"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
