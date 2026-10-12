import { prisma } from "@/lib/db";
import type { Device } from "@prisma/client";

// App 端鉴权：Authorization: Bearer <deviceKey>
export async function authenticateDevice(req: Request): Promise<Device | null> {
  const header = req.headers.get("authorization");
  if (!header?.startsWith("Bearer ")) return null;
  const key = header.slice(7).trim();
  if (!key) return null;
  return prisma.device.findUnique({ where: { deviceKey: key } });
}
