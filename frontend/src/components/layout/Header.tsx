import { useNavigate } from "react-router-dom";
import { LogOut, User, Settings } from "lucide-react";
import { signOut } from "@/lib/auth";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export default function Header() {
  const navigate = useNavigate();
  const { profile } = useAuth();

  const handleLogout = async () => {
    await signOut();
    navigate("/login");
  };

  const displayName = profile?.contactName || profile?.companyName || profile?.email || "사용자";

  return (
    <header className="flex h-16 items-center justify-end border-b bg-background px-6">
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" className="gap-2">
            <User className="h-4 w-4" />
            <span>{displayName}</span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onClick={() => navigate("/profile")}>
            <Settings className="mr-2 h-4 w-4" />
            내 정보
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={handleLogout}>
            <LogOut className="mr-2 h-4 w-4" />
            로그아웃
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </header>
  );
}
