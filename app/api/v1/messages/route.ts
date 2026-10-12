import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";
import { authenticateDevice } from "@/lib/device-auth";

type IncomingMessage = {
  address?: unknown;
  body?: unknown;
  date?: unknown; // epoch 毫秒或 ISO 字符串
  type?: unknown; // "received" | "sent"
};

// App 批量上报短信。重复上报通过 unique(deviceId, address, smsDate) 幂等去重
export async function POST(req: Request) {
  const device = await authenticateDevice(req);
  if (!device) {
    return NextResponse.json({ error: "设备密钥无效" }, { status: 401 });
  }

  let payload: { messages?: IncomingMessage[] };
  try {
    payload = await req.json();
  } catch {
    return NextResponse.json({ error: "请求体不是合法 JSON" }, { status: 400 });
  }

  const list = Array.isArray(payload?.messages) ? payload.messages : [];
  if (list.length === 0) {
    return NextResponse.json({ error: "messages 不能为空" }, { status: 400 });
  }
  if (list.length > 500) {
    return NextResponse.json({ error: "单次最多上传 500 条" }, { status: 400 });
  }

  const rows = [];
  for (const m of list) {
    const address = String(m?.address ?? "").trim();
    const body = String(m?.body ?? "");
    if (!address || !body) continue;

    const raw = m?.date;
    const date =
      typeof raw === "number" ? new Date(raw) : new Date(String(raw ?? ""));
    if (Number.isNaN(date.getTime())) continue;

    rows.push({
      deviceId: device.id,
      address: address.slice(0, 64),
      body: body.slice(0, 4000),
      type: m?.type === "sent" ? "sent" : "received",
      smsDate: date,
    });
  }

  if (rows.length === 0) {
    return NextResponse.json({ error: "没有有效短信" }, { status: 400 });
  }

  const result = await prisma.message.createMany({
    data: rows,
    skipDuplicates: true,
  });
  await prisma.device.update({
    where: { id: device.id },
    data: { lastSeenAt: new Date() },
  });

  return NextResponse.json({ ok: true, received: result.count });
}
