import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";
import { getOrCreateDevice } from "@/lib/device-auth";

// 心跳/连接测试：?name=设备名（GET 或 POST JSON {name}），按名自动创建并刷新在线时间
async function resolveName(req: Request): Promise<string> {
  const url = new URL(req.url);
  const fromQuery = url.searchParams.get("name");
  if (fromQuery) return fromQuery;
  if (req.method === "POST") {
    const body = await req.json().catch(() => ({}));
    return String((body as { name?: unknown })?.name ?? "");
  }
  return "";
}

async function handle(req: Request) {
  const name = (await resolveName(req)).trim();
  if (!name) {
    return NextResponse.json({ error: "name 不能为空" }, { status: 400 });
  }
  const device = await getOrCreateDevice(name);
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
