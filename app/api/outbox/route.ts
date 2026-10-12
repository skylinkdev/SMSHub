import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";

// Web 端（登录会话）创建发送任务
export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const { deviceId, to, body: text, simSlot } = body as {
    deviceId?: string;
    to?: string;
    body?: string;
    simSlot?: number | null;
  };
  if (!deviceId || !to?.trim() || !text?.trim()) {
    return NextResponse.json({ error: "设备、号码、内容不能为空" }, { status: 400 });
  }
  const device = await prisma.device.findUnique({ where: { id: deviceId } });
  if (!device) {
    return NextResponse.json({ error: "设备不存在" }, { status: 404 });
  }
  const task = await prisma.sendTask.create({
    data: {
      deviceId: device.id,
      to: to.trim().slice(0, 32),
      body: text.trim().slice(0, 1000),
      simSlot: simSlot === 1 || simSlot === 2 ? simSlot : null,
    },
  });
  return NextResponse.json(task);
}
