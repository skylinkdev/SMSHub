import Sidebar from "@/components/Sidebar";
import LogoutButton from "@/components/LogoutButton";
import AutoRefresh from "@/components/AutoRefresh";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-6">
          <h1 className="text-base font-semibold text-slate-800">
            短信管理平台
          </h1>
          <LogoutButton />
        </header>
        <main className="flex-1 p-6">{children}</main>
      </div>
      <AutoRefresh />
    </div>
  );
}
