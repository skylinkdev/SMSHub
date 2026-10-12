import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";
import { authenticateDevice } from "@/lib/device-auth";

// 心跳/连接测试：验证设备密钥并刷新在线时间
async function handle(req: Request) {
  const device = await authenticateDevice(req);
  if (!device) {
    return NextResponse.json({ error: "设备密钥无效" }, { status: 401 });
  }
  await prisma.device.update({
    where: { id: device.id },
    data: { lastSeenAt: new Date() },
  });
  return NextResponse.json({
    ok: true,
    serverTime: Date.now(),
    device: { id: device.id, name: device.name },
  });
}

export const GET = handle;
export const POST = handle;
