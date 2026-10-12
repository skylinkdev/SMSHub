import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";

// 管理端删除单条短信（受登录中间件保护）
export async function DELETE(
  _req: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  const result = await prisma.message.deleteMany({ where: { id } });
  if (result.count === 0) {
    return NextResponse.json({ error: "短信不存在" }, { status: 404 });
  }
  return NextResponse.json({ ok: true });
}
