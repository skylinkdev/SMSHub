import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "SMSHub - 短信管理平台",
  description: "配合安卓应用监控短信，Web 远程查看",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="zh-CN">
      <body className="bg-slate-100 text-slate-800 antialiased">{children}</body>
    </html>
  );
}
