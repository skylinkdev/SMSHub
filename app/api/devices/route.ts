import { NextResponse } from "next/server";
import { randomBytes } from "crypto";
import { prisma } from "@/lib/db";

// 创建设备并生成设备密钥（在 App 中填入该密钥完成绑定）
export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const name = String((body as { name?: string })?.name ?? "").trim();
  if (!name) {
    return NextResponse.json({ error: "设备名称不能为空" }, { status: 400 });
  }
  const device = await prisma.device.create({
    data: {
      name: name.slice(0, 50),
      deviceKey: randomBytes(24).toString("hex"),
    },
  });
  return NextResponse.json(device);
}
