import Link from "next/link";
import { prisma } from "@/lib/db";
import MessageDeleteButton from "@/components/MessageDeleteButton";
import type { Prisma } from "@prisma/client";

export const dynamic = "force-dynamic";

const PAGE_SIZE = 20;

type SearchParams = Promise<{
  deviceId?: string;
  q?: string;
  type?: string;
  page?: string;
}>;

function fmt(d: Date) {
  return d.toLocaleString("zh-CN", { hour12: false });
}

export default async function MessagesPage({
  searchParams,
}: {
  searchParams: SearchParams;
}) {
  const sp = await searchParams;
  const page = Math.max(1, parseInt(sp.page || "1", 10) || 1);
  const q = (sp.q || "").trim();

  const where: Prisma.MessageWhereInput = {};
  if (sp.deviceId) where.deviceId = sp.deviceId;
  if (sp.type === "received" || sp.type === "sent") where.type = sp.type;
  if (q) {
    where.OR = [
      { address: { contains: q, mode: "insensitive" } },
      { body: { contains: q, mode: "insensitive" } },
    ];
  }

  const [devices, total, items] = await Promise.all([
    prisma.device.findMany({ orderBy: { createdAt: "asc" } }),
    prisma.message.count({ where }),
    prisma.message.findMany({
      where,
      orderBy: { smsDate: "desc" },
      skip: (page - 1) * PAGE_SIZE,
      take: PAGE_SIZE,
      include: { device: { select: { name: true } } },
    }),
  ]);

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  function pageUrl(p: number) {
    const params = new URLSearchParams();
    if (sp.deviceId) params.set("deviceId", sp.deviceId);
    if (q) params.set("q", q);
    if (sp.type) params.set("type", sp.type);
    params.set("page", String(p));
    return `/messages?${params.toString()}`;
  }

  return (
    <div className="space-y-4">
      {/* 筛选栏 */}
      <form
        method="GET"
        action="/messages"
        className="flex flex-wrap items-end gap-3 rounded-xl border border-slate-200 bg-white p-4"
      >
        <div>
          <label className="mb-1 block text-xs text-slate-500">设备</label>
          <select
            name="deviceId"
            defaultValue={sp.deviceId || ""}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500"
          >
            <option value="">全部设备</option>
            {devices.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="mb-1 block text-xs text-slate-500">类型</label>
          <select
            name="type"
            defaultValue={sp.type || ""}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500"
          >
            <option value="">全部</option>
            <option value="received">接收</option>
            <option value="sent">发送</option>
          </select>
        </div>
        <div className="min-w-52 flex-1">
          <label className="mb-1 block text-xs text-slate-500">关键词</label>
          <input
            name="q"
            defaultValue={q}
            placeholder="搜索号码或短信内容…"
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500"
          />
        </div>
        <button
          type="submit"
          className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-700"
        >
          查询
        </button>
      </form>

      {/* 列表 */}
      <div className="rounded-xl border border-slate-200 bg-white">
        <div className="border-b border-slate-100 px-5 py-4 text-sm text-slate-500">
          共 <span className="font-semibold text-slate-800">{total}</span> 条记录
        </div>
        {items.length === 0 ? (
          <p className="px-5 py-10 text-center text-sm text-slate-400">
            没有符合条件的短信
          </p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 text-left text-xs text-slate-400">
                <th className="px-5 py-3 font-medium">时间</th>
                <th className="px-5 py-3 font-medium">设备</th>
                <th className="px-5 py-3 font-medium">类型</th>
                <th className="px-5 py-3 font-medium">发送号码</th>
                <th className="px-5 py-3 font-medium">接收号码</th>
                <th className="px-5 py-3 font-medium">内容</th>
                <th className="px-5 py-3 font-medium">操作</th>
              </tr>
            </thead>
            <tbody>
              {items.map((m) => (
                <tr key={m.id} className="border-b border-slate-50 last:border-0">
                  <td className="whitespace-nowrap px-5 py-3 text-slate-500">
                    {fmt(m.smsDate)}
                  </td>
                  <td className="whitespace-nowrap px-5 py-3">{m.device.name}</td>
                  <td className="px-5 py-3">
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                        m.type === "sent"
                          ? "bg-amber-50 text-amber-600"
                          : "bg-emerald-50 text-emerald-600"
                      }`}
                    >
                      {m.type === "sent" ? "发送" : "接收"}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-5 py-3 font-medium">
                    {m.type === "sent" ? `${m.device.name}（本机）` : m.address}
                  </td>
                  <td className="whitespace-nowrap px-5 py-3 font-medium">
                    {m.type === "sent" ? m.address : `${m.device.name}（本机）`}
                  </td>
                  <td
                    className="max-w-md truncate px-5 py-3 text-slate-600"
                    title={m.body}
                  >
                    {m.body}
                  </td>
                  <td className="px-5 py-3">
                    <MessageDeleteButton id={m.id} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {/* 分页 */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-100 px-5 py-3 text-sm">
            <span className="text-slate-500">
              第 {page} / {totalPages} 页
            </span>
            <div className="flex gap-2">
              {page > 1 && (
                <Link
                  href={pageUrl(page - 1)}
                  className="rounded-lg border border-slate-300 px-3 py-1.5 text-slate-600 hover:bg-slate-50"
                >
                  上一页
                </Link>
              )}
              {page < totalPages && (
                <Link
                  href={pageUrl(page + 1)}
                  className="rounded-lg border border-slate-300 px-3 py-1.5 text-slate-600 hover:bg-slate-50"
                >
                  下一页
                </Link>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
