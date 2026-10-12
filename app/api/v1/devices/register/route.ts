import { NextResponse } from "next/server";
import { randomBytes } from "crypto";
import { prisma } from "@/lib/db";

// App 首次连接自动注册设备，签发设备密钥。
// 携带相同 installId 重复注册时返回原设备（重装/重复调用幂等）。
export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const raw = body as { name?: unknown; installId?: unknown };

  const name =
    String(raw?.name ?? "").trim().slice(0, 50) || "未命名设备";
  const installId = String(raw?.installId ?? "").trim().slice(0, 64) || null;

  if (installId) {
    const existing = await prisma.device.findUnique({ where: { installId } });
    if (existing) {
      return NextResponse.json({
        id: existing.id,
        name: existing.name,
        deviceKey: existing.deviceKey,
      });
    }
  }

  const device = await prisma.device.create({
    data: { name, installId, deviceKey: randomBytes(24).toString("hex") },
  });
  return NextResponse.json({
    id: device.id,
    name: device.name,
    deviceKey: device.deviceKey,
  });
}
