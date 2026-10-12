import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";

// 前端轮询：返回 since（epoch 毫秒）之后新入库的短信数量
export async function GET(req: Request) {
  const since = Number(new URL(req.url).searchParams.get("since") || 0);
  const count = await prisma.message.count({
    where: { createdAt: { gt: new Date(since) } },
  });
  return NextResponse.json({ count, serverTime: Date.now() });
}
