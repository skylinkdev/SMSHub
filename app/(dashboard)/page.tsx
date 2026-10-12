import Link from "next/link";
import { prisma } from "@/lib/db";

export const dynamic = "force-dynamic";

function fmt(d: Date) {
  return d.toLocaleString("zh-CN", { hour12: false });
}

export default async function DashboardPage() {
  const todayStart = new Date();
  todayStart.setHours(0, 0, 0, 0);
  const onlineWindow = new Date(Date.now() - 10 * 60 * 1000);

  const [deviceCount, onlineCount, messageCount, todayCount, recent] =
    await Promise.all([
      prisma.device.count(),
      prisma.device.count({ where: { lastSeenAt: { gte: onlineWindow } } }),
      prisma.message.count(),
      prisma.message.count({ where: { smsDate: { gte: todayStart } } }),
      prisma.message.findMany({
        orderBy: { smsDate: "desc" },
        take: 10,
        include: { device: { select: { name: true, phoneNumber: true } } },
      }),
    ]);

  const stats = [
    { label: "设备总数", value: deviceCount },
    { label: "在线设备", value: onlineCount },
    { label: "短信总数", value: messageCount },
    { label: "今日短信", value: todayCount },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {stats.map((s) => (
          <div
            key={s.label}
            className="rounded-xl border border-slate-200 bg-white p-5"
          >
            <p className="text-sm text-slate-500">{s.label}</p>
            <p className="mt-2 text-3xl font-bold text-slate-900">{s.value}</p>
          </div>
        ))}
      </div>

      <div className="rounded-xl border border-slate-200 bg-white">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
          <h2 className="font-semibold text-slate-800">最新短信</h2>
          <Link
            href="/messages"
            className="text-sm text-indigo-600 hover:underline"
          >
            查看全部 →
          </Link>
        </div>
        {recent.length === 0 ? (
          <p className="px-5 py-10 text-center text-sm text-slate-400">
            暂无短信。在 App 中填入服务器地址保存后即可自动上报。
          </p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 text-left text-xs text-slate-400">
                <th className="px-5 py-3 font-medium">时间</th>
                <th className="px-5 py-3 font-medium">设备</th>
                <th className="px-5 py-3 font-medium">发送号码</th>
                <th className="px-5 py-3 font-medium">接收号码</th>
                <th className="px-5 py-3 font-medium">内容</th>
              </tr>
            </thead>
            <tbody>
              {recent.map((m) => (
                <tr key={m.id} className="border-b border-slate-50 last:border-0">
                  <td className="whitespace-nowrap px-5 py-3 text-slate-500">
                    {fmt(m.smsDate)}
                  </td>
                  <td className="px-5 py-3">{m.device.name}</td>
                  <td className="whitespace-nowrap px-5 py-3 font-medium">
                    {m.type === "sent" ? m.device.phoneNumber || "本机" : m.address}
                  </td>
                  <td className="whitespace-nowrap px-5 py-3 font-medium">
                    {m.type === "sent" ? m.address : m.device.phoneNumber || "本机"}
                  </td>
                  <td className="max-w-md truncate px-5 py-3 text-slate-600" title={m.body}>
                    {m.body}
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
