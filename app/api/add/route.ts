import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";
import { getOrCreateDevice } from "@/lib/device-auth";

// URL 通道：GET /api/add?type=0&msg=内容&from=号码(可选,默认"URL")
// type=0 收件 / type=1 发件；该通道上报的消息归属虚拟设备名 "URL"
export async function GET(req: Request) {
  const q = new URL(req.url).searchParams;
  const msg = String(q.get("msg") ?? "").trim();
  if (!msg) {
    return NextResponse.json({ error: "msg 不能为空" }, { status: 400 });
  }
  const from = String(q.get("from") ?? "URL").trim() || "URL";
  const type = q.get("type") === "1" ? "sent" : "received";

  const device = await getOrCreateDevice("URL");
  await prisma.message.create({
    data: {
      deviceId: device.id,
      address: from.slice(0, 64),
      body: msg.slice(0, 4000),
      type,
      smsDate: new Date(),
    },
  });
  await prisma.device.update({
    where: { id: device.id },
    data: { lastSeenAt: new Date() },
  });
  return NextResponse.json({ ok: true });
}
