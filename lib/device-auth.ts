import { prisma } from "@/lib/db";
import type { Device } from "@prisma/client";

// 按设备名获取设备，不存在则自动创建（全平台仅用设备名识别设备）
export async function getOrCreateDevice(name: string): Promise<Device> {
  const trimmed = name.trim().slice(0, 64);
  const existing = await prisma.device.findUnique({ where: { name: trimmed } });
  if (existing) return existing;
  return prisma.device.create({ data: { name: trimmed } });
}
