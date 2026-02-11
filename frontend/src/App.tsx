import { Routes, Route, Navigate, Outlet, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { Toaster } from "@/components/ui/sonner";
import { getSession } from "@/lib/auth";
import { AuthProvider } from "@/contexts/AuthContext";
import Sidebar from "@/components/layout/Sidebar";
import Header from "@/components/layout/Header";
import { Loader2 } from "lucide-react";

import Landing from "@/pages/Landing";
import Dashboard from "@/pages/Dashboard";
import Login from "@/pages/Login";
import Signup from "@/pages/Signup";
import Orders from "@/pages/Orders";
import ProductMappings from "@/pages/ProductMappings";
import CoupangProducts from "@/pages/CoupangProducts";
import Shipments from "@/pages/Shipments";
import Settlements from "@/pages/Settlements";
import ErpDocuments from "@/pages/ErpDocuments";
import Settings from "@/pages/Settings";
import Profile from "@/pages/Profile";

function ProtectedLayout() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);

  useEffect(() => {
    const checkAuth = async () => {
      try {
        const session = await getSession();
        if (session) {
          setAuthenticated(true);
        } else {
          navigate("/login", { replace: true });
        }
      } catch {
        navigate("/login", { replace: true });
      } finally {
        console.log("finally");
        setLoading(false);
      }
    };
    checkAuth();
  }, [navigate]);

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!authenticated) {
    return null;
  }

  return (
    <AuthProvider>
      <div className="flex h-screen">
        <Sidebar />
        <div className="flex flex-1 flex-col overflow-hidden">
          <Header />
          <main className="flex-1 overflow-auto p-6">
            <Outlet />
          </main>
        </div>
      </div>
    </AuthProvider>
  );
}

export default function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route element={<ProtectedLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/product-mappings" element={<ProductMappings />} />
          <Route path="/coupang-products" element={<CoupangProducts />} />
          <Route path="/shipments" element={<Shipments />} />
          <Route path="/settlements" element={<Settlements />} />
          <Route path="/erp-documents" element={<ErpDocuments />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="/profile" element={<Profile />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <Toaster position="top-right" richColors />
    </>
  );
}
