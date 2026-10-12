import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";
import { getOrCreateDevice } from "@/lib/device-auth";

// URL 直接调用发短信（按设备名路由到对应手机）：
//   GET  /api/v1/send?name=Xiaomi%202510DRK44C&to=13800001111&body=内容&sim=1
//   POST /api/v1/send  JSON: {name, to, body, sim}
async function createTask(params: Record<string, unknown>) {
  const name = String(params.name ?? "").trim();
  const to = String(params.to ?? "").trim();
  const body = String(params.body ?? "").trim();
  const simRaw = String(params.sim ?? "").trim();
  if (!name || !to || !body) {
    return NextResponse.json({ error: "name、to、body 不能为空" }, { status: 400 });
  }
  const simSlot = simRaw === "1" || simRaw === "2" ? parseInt(simRaw, 10) : null;

  const device = await getOrCreateDevice(name);
  const task = await prisma.sendTask.create({
    data: { deviceId: device.id, to: to.slice(0, 32), body: body.slice(0, 1000), simSlot },
  });
  return NextResponse.json({ ok: true, taskId: task.id, status: "pending" });
}

export async function GET(req: Request) {
  const q = new URL(req.url).searchParams;
  return createTask({ name: q.get("name"), to: q.get("to"), body: q.get("body"), sim: q.get("sim") });
}

export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  return createTask(body as Record<string, unknown>);
}
