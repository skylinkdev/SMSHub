import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";
import { getOrCreateDevice } from "@/lib/device-auth";

// App 回执：POST /api/v1/outbox/<taskId> {name: "设备名", status: "sent"|"failed", error?}
export async function POST(
  req: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const body = await req.json().catch(() => ({}));
  const name = String((body as { name?: unknown })?.name ?? "").trim();
  if (!name) {
    return NextResponse.json({ error: "name 不能为空" }, { status: 400 });
  }
  const status = (body as { status?: string })?.status;
  if (status !== "sent" && status !== "failed") {
    return NextResponse.json({ error: "status 必须是 sent 或 failed" }, { status: 400 });
  }

  const device = await getOrCreateDevice(name);
  const { id } = await params;
  const result = await prisma.sendTask.updateMany({
    where: { id, deviceId: device.id, status: "pending" },
    data: {
      status,
      error:
        status === "failed"
          ? String((body as { error?: unknown })?.error ?? "").slice(0, 200) || "发送失败"
          : null,
      sentAt: new Date(),
    },
  });
  if (result.count === 0) {
    return NextResponse.json({ error: "任务不存在或已处理" }, { status: 404 });
  }
  return NextResponse.json({ ok: true });
}
