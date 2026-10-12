import { prisma } from "@/lib/db";
import DeviceAddButton from "@/components/DeviceAddButton";
import DeviceDeleteButton from "@/components/DeviceDeleteButton";

export const dynamic = "force-dynamic";

function fmt(d: Date | null) {
  if (!d) return "从未上线";
  return d.toLocaleString("zh-CN", { hour12: false });
}

function isOnline(d: Date | null) {
  return d != null && Date.now() - d.getTime() < 10 * 60 * 1000;
}

export default async function DevicesPage() {
  const devices = await prisma.device.findMany({
    orderBy: { createdAt: "desc" },
    include: { _count: { select: { messages: true } } },
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-500">
          设备按名称识别：App 首次上报时会自动创建，也可以提前手动添加。
        </p>
        <DeviceAddButton />
      </div>

      <div className="rounded-xl border border-slate-200 bg-white">
        {devices.length === 0 ? (
          <p className="px-5 py-12 text-center text-sm text-slate-400">
            还没有设备，点击右上角「添加设备」创建第一个。
          </p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 text-left text-xs text-slate-400">
                <th className="px-5 py-3 font-medium">状态</th>
                <th className="px-5 py-3 font-medium">设备名称</th>
                <th className="px-5 py-3 font-medium">短信数</th>
                <th className="px-5 py-3 font-medium">最近在线</th>
                <th className="px-5 py-3 font-medium">操作</th>
              </tr>
            </thead>
            <tbody>
              {devices.map((d) => (
                <tr key={d.id} className="border-b border-slate-50 last:border-0">
                  <td className="px-5 py-3">
                    <span
                      className={`inline-flex items-center gap-1.5 text-xs font-medium ${
                        isOnline(d.lastSeenAt)
                          ? "text-emerald-600"
                          : "text-slate-400"
                      }`}
                    >
                      <span
                        className={`h-2 w-2 rounded-full ${
                          isOnline(d.lastSeenAt)
                            ? "bg-emerald-500"
                            : "bg-slate-300"
                        }`}
                      />
                      {isOnline(d.lastSeenAt) ? "在线" : "离线"}
                    </span>
                  </td>
                  <td className="px-5 py-3 font-medium text-slate-800">
                    {d.name}
                  </td>
                  <td className="px-5 py-3 text-slate-600">
                    {d._count.messages}
                  </td>
                  <td className="whitespace-nowrap px-5 py-3 text-slate-500">
                    {fmt(d.lastSeenAt)}
                  </td>
                  <td className="px-5 py-3">
                    <DeviceDeleteButton id={d.id} name={d.name} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
