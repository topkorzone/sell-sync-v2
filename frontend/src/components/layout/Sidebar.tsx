import { useLocation, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  ShoppingCart,
  Link2,
  Package,
  Truck,
  DollarSign,
  Settings,
} from "lucide-react";
import { cn } from "@/lib/utils";

const menuItems = [
  { path: "/", icon: LayoutDashboard, label: "대시보드" },
  { path: "/orders", icon: ShoppingCart, label: "주문 관리" },
  { path: "/product-mappings", icon: Link2, label: "상품 매핑" },
  { path: "/coupang-products", icon: Package, label: "쿠팡 상품" },
  { path: "/shipments", icon: Truck, label: "배송 관리" },
  { path: "/settlements", icon: DollarSign, label: "정산 관리" },
  { path: "/settings", icon: Settings, label: "설정" },
];

export default function Sidebar() {
  const location = useLocation();
  const navigate = useNavigate();

  const isActive = (path: string) => {
    if (path === "/") return location.pathname === "/";
    return location.pathname.startsWith(path);
  };

  return (
    <aside className="flex h-screen w-56 flex-col border-r border-sidebar-border bg-sidebar">
      <div className="flex h-16 items-center justify-center border-b border-sidebar-border">
        <h2 className="text-lg font-semibold text-sidebar-foreground">
          MarketHub
        </h2>
      </div>
      <nav className="flex-1 space-y-1 p-3">
        {menuItems.map((item) => (
          <button
            key={item.path}
            onClick={() => navigate(item.path)}
            className={cn(
              "flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
              isActive(item.path)
                ? "bg-sidebar-accent text-sidebar-accent-foreground"
                : "text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
            )}
          >
            <item.icon className="h-4 w-4" />
            {item.label}
          </button>
        ))}
      </nav>
    </aside>
  );
}
