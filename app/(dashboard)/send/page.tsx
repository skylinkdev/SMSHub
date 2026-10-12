import { prisma } from "@/lib/db";
import SendForm from "@/components/SendForm";

export const dynamic = "force-dynamic";

function fmt(d: Date | null) {
  if (!d) return "-";
  return d.toLocaleString("zh-CN", { hour12: false });
}

const STATUS: Record<string, { label: string; cls: string }> = {
  pending: { label: "待发送", cls: "bg-amber-50 text-amber-600" },
  sent: { label: "已发送", cls: "bg-emerald-50 text-emerald-600" },
  failed: { label: "失败", cls: "bg-red-50 text-red-600" },
};

export default async function SendPage() {
  const [devices, tasks] = await Promise.all([
    prisma.device.findMany({
      orderBy: { createdAt: "asc" },
      select: { id: true, name: true },
    }),
    prisma.sendTask.findMany({
      orderBy: { createdAt: "desc" },
      take: 50,
      include: { device: { select: { name: true } } },
    }),
  ]);

  return (
    <div className="space-y-4">
      <div className="rounded-xl border border-slate-200 bg-white">
        <div className="border-b border-slate-100 px-5 py-4">
          <h2 className="font-semibold text-slate-800">发送短信</h2>
          <p className="mt-1 text-xs text-slate-400">
            任务提交后由手机 App 轮询执行（约 30 秒内）。也可直接调用 URL：
            <code className="ml-1 rounded bg-slate-100 px-1">
              /api/v1/send?name=设备名&to=号码&body=内容&sim=1
            </code>
          </p>
        </div>
        <SendForm devices={devices} />
      </div>

      <div className="rounded-xl border border-slate-200 bg-white">
        <div className="border-b border-slate-100 px-5 py-4 text-sm font-semibold text-slate-800">
          发送记录（最近 50 条）
        </div>
        {tasks.length === 0 ? (
          <p className="px-5 py-10 text-center text-sm text-slate-400">
            暂无发送任务
          </p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 text-left text-xs text-slate-400">
                <th className="px-5 py-3 font-medium">创建时间</th>
                <th className="px-5 py-3 font-medium">设备</th>
                <th className="px-5 py-3 font-medium">号码</th>
                <th className="px-5 py-3 font-medium">内容</th>
                <th className="px-5 py-3 font-medium">卡槽</th>
                <th className="px-5 py-3 font-medium">状态</th>
                <th className="px-5 py-3 font-medium">完成时间</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((t) => {
                const s = STATUS[t.status] ?? STATUS.pending;
                return (
                  <tr key={t.id} className="border-b border-slate-50 last:border-0">
                    <td className="whitespace-nowrap px-5 py-3 text-slate-500">
                      {fmt(t.createdAt)}
                    </td>
                    <td className="px-5 py-3">{t.device.name}</td>
                    <td className="whitespace-nowrap px-5 py-3 font-medium">{t.to}</td>
                    <td className="max-w-xs truncate px-5 py-3 text-slate-600" title={t.body}>
                      {t.body}
                      {t.error && (
                        <span className="block text-xs text-red-500">{t.error}</span>
                      )}
                    </td>
                    <td className="px-5 py-3 text-slate-500">
                      {t.simSlot ? `卡 ${t.simSlot}` : "默认"}
                    </td>
                    <td className="px-5 py-3">
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${s.cls}`}>
                        {s.label}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-5 py-3 text-slate-500">
                      {fmt(t.sentAt)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
