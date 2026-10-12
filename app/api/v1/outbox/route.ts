import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";
import { getOrCreateDevice } from "@/lib/device-auth";

// App 轮询待发送任务：GET /api/v1/outbox?name=设备名（每次最多 10 条）
export async function GET(req: Request) {
  const name = String(new URL(req.url).searchParams.get("name") ?? "").trim();
  if (!name) {
    return NextResponse.json({ error: "name 不能为空" }, { status: 400 });
  }
  const device = await getOrCreateDevice(name);
  const tasks = await prisma.sendTask.findMany({
    where: { deviceId: device.id, status: "pending" },
    orderBy: { createdAt: "asc" },
    take: 10,
    select: { id: true, to: true, body: true, simSlot: true },
  });
  await prisma.device.update({
    where: { id: device.id },
    data: { lastSeenAt: new Date() },
  });
  return NextResponse.json({ tasks });
}
