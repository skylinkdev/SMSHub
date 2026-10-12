import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";

// 手动预建设备（可选）：App 上报时也会按设备名自动创建
export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const name = String((body as { name?: string })?.name ?? "").trim();
  if (!name) {
    return NextResponse.json({ error: "设备名称不能为空" }, { status: 400 });
  }
  const existing = await prisma.device.findUnique({ where: { name: name.slice(0, 64) } });
  if (existing) {
    return NextResponse.json(existing);
  }
  const device = await prisma.device.create({
    data: { name: name.slice(0, 64) },
  });
  return NextResponse.json(device);
}
