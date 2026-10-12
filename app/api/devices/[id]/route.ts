import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";

type Params = { params: Promise<{ id: string }> };

export async function DELETE(_req: Request, { params }: Params) {
  const { id } = await params;
  await prisma.device.delete({ where: { id } }).catch(() => null);
  return NextResponse.json({ ok: true });
}

// 重命名设备
export async function PATCH(req: Request, { params }: Params) {
  const { id } = await params;
  const body = await req.json().catch(() => ({}));
  const name = String((body as { name?: string })?.name ?? "").trim();
  if (!name) {
    return NextResponse.json({ error: "设备名称不能为空" }, { status: 400 });
  }
  const device = await prisma.device
    .update({ where: { id }, data: { name: name.slice(0, 50) } })
    .catch(() => null);
  if (!device) {
    return NextResponse.json({ error: "设备不存在" }, { status: 404 });
  }
  return NextResponse.json(device);
}
