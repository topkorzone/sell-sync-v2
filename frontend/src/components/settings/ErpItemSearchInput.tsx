import { useState, useCallback, useEffect, useRef } from "react";
import { Search, Loader2, X, Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import api from "@/lib/api";
import type { ErpItem, PageResponse } from "@/types";

interface ErpItemSearchInputProps {
  value: string;
  prodDes?: string;
  onChange: (prodCd: string, prodDes: string) => void;
  placeholder?: string;
  disabled?: boolean;
}

export default function ErpItemSearchInput({
  value,
  prodDes,
  onChange,
  placeholder = "ERP 품목 검색 또는 직접 입력",
  disabled = false,
}: ErpItemSearchInputProps) {
  const [open, setOpen] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [erpItems, setErpItems] = useState<ErpItem[]>([]);
  const [loading, setLoading] = useState(false);
  const searchTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // ERP 품목 검색
  const searchErpItems = useCallback(async (keyword: string) => {
    if (!keyword || keyword.length < 1) {
      setErpItems([]);
      return;
    }

    setLoading(true);
    try {
      const params: Record<string, string | number> = { page: 0, size: 100 };
      if (keyword) params.keyword = keyword;
      const { data } = await api.get<{ data: PageResponse<ErpItem> }>("/api/v1/erp/items", { params });
      setErpItems(data.data?.content ?? []);
    } catch {
      setErpItems([]);
    } finally {
      setLoading(false);
    }
  }, []);

  // 디바운스 검색
  useEffect(() => {
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    if (open && searchKeyword) {
      searchTimeoutRef.current = setTimeout(() => {
        searchErpItems(searchKeyword);
      }, 300);
    }

    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    };
  }, [searchKeyword, open, searchErpItems]);

  // 외부 클릭 시 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    if (open) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [open]);

  // 드롭다운 열릴 때 초기화
  useEffect(() => {
    if (open) {
      setSearchKeyword(value || "");
      if (value) {
        searchErpItems(value);
      }
      setTimeout(() => inputRef.current?.focus(), 50);
    } else {
      setErpItems([]);
    }
  }, [open, value, searchErpItems]);

  const handleSelect = (item: ErpItem) => {
    onChange(item.prodCd, item.prodDes);
    setOpen(false);
  };

  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation();
    onChange("", "");
  };

  const handleDirectInput = () => {
    if (searchKeyword) {
      onChange(searchKeyword, "");
      setOpen(false);
    }
  };

  const displayValue = value
    ? prodDes
      ? `${value} - ${prodDes}`
      : value
    : "";

  return (
    <div ref={containerRef} className="relative">
      {/* 트리거 버튼 */}
      <Button
        type="button"
        variant="outline"
        disabled={disabled}
        onClick={() => setOpen(!open)}
        className="h-8 w-full justify-start text-sm font-normal"
      >
        <Search className="mr-2 h-3.5 w-3.5 shrink-0 text-muted-foreground" />
        {displayValue ? (
          <span className="truncate flex-1 text-left">{displayValue}</span>
        ) : (
          <span className="truncate flex-1 text-left text-muted-foreground">
            {placeholder}
          </span>
        )}
        {value && (
          <X
            className="ml-2 h-3.5 w-3.5 shrink-0 text-muted-foreground hover:text-foreground"
            onClick={handleClear}
          />
        )}
      </Button>

      {/* 드롭다운 (Portal 없이 직접 위치) */}
      {open && (
        <div className="absolute left-0 top-full z-50 mt-1 w-80 rounded-md border bg-popover shadow-lg">
          {/* 검색 입력 */}
          <div className="p-2 border-b">
            <div className="relative">
              <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                ref={inputRef}
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    handleDirectInput();
                  }
                  if (e.key === "Escape") {
                    setOpen(false);
                  }
                }}
                placeholder="품목코드 또는 품목명 검색..."
                className="h-8 pl-8 text-sm"
              />
              {loading && (
                <Loader2 className="absolute right-2.5 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-muted-foreground" />
              )}
            </div>
          </div>

          {/* 검색 결과 */}
          {erpItems.length === 0 ? (
            <div className="p-4 text-center text-sm text-muted-foreground">
              {searchKeyword.length < 1
                ? "검색어를 입력하세요"
                : loading
                ? "검색 중..."
                : "검색 결과가 없습니다"}
            </div>
          ) : (
            <ul
              className="py-1 overflow-y-auto"
              style={{ maxHeight: "200px" }}
            >
              {erpItems.map((item) => (
                <li key={item.id}>
                  <button
                    type="button"
                    onClick={() => handleSelect(item)}
                    className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-muted transition-colors"
                  >
                    <span className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded shrink-0">
                      {item.prodCd}
                    </span>
                    <span className="text-sm text-foreground truncate flex-1">
                      {item.prodDes}
                    </span>
                    {value === item.prodCd && (
                      <Check className="h-3.5 w-3.5 text-primary shrink-0" />
                    )}
                  </button>
                </li>
              ))}
            </ul>
          )}

          {/* 하단 */}
          <div className="p-2 border-t bg-muted/50">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="h-7 text-xs w-full"
              onClick={handleDirectInput}
              disabled={!searchKeyword}
            >
              "{searchKeyword || "..."}" 직접 입력
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
