"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

export default function SendForm({
  devices,
}: {
  devices: { id: string; name: string }[];
}) {
  const router = useRouter();
  const [deviceId, setDeviceId] = useState(devices[0]?.id ?? "");
  const [to, setTo] = useState("");
  const [body, setBody] = useState("");
  const [simSlot, setSimSlot] = useState("");
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState("");

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setMsg("");
    setLoading(true);
    try {
      const res = await fetch("/api/outbox", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          deviceId,
          to,
          body,
          simSlot: simSlot ? parseInt(simSlot, 10) : null,
        }),
      });
      const data = await res.json();
      if (!res.ok) {
        setMsg(data.error || "创建失败");
      } else {
        setMsg("已加入发送队列，手机将在 30 秒内执行");
        setTo("");
        setBody("");
        router.refresh();
      }
    } catch {
      setMsg("网络错误");
    } finally {
      setLoading(false);
    }
  }

  if (devices.length === 0) {
    return (
      <p className="px-5 py-6 text-sm text-slate-400">
        请先在「设备管理」添加设备
      </p>
    );
  }

  return (
    <form onSubmit={submit} className="space-y-4 p-5">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="mb-1 block text-xs text-slate-500">发送设备</label>
          <select
            value={deviceId}
            onChange={(e) => setDeviceId(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500"
          >
            {devices.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="mb-1 block text-xs text-slate-500">SIM 卡</label>
          <select
            value={simSlot}
            onChange={(e) => setSimSlot(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500"
          >
            <option value="">系统默认卡</option>
            <option value="1">卡 1</option>
            <option value="2">卡 2</option>
          </select>
        </div>
      </div>
      <div>
        <label className="mb-1 block text-xs text-slate-500">接收号码</label>
        <input
          value={to}
          onChange={(e) => setTo(e.target.value)}
          required
          placeholder="13800001111"
          className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500"
        />
      </div>
      <div>
        <label className="mb-1 block text-xs text-slate-500">短信内容</label>
        <textarea
          value={body}
          onChange={(e) => setBody(e.target.value)}
          required
          rows={3}
          maxLength={1000}
          placeholder="长短信会自动拆分多条发送"
          className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500"
        />
      </div>
      {msg && (
        <p className="rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-600">
          {msg}
        </p>
      )}
      <button
        type="submit"
        disabled={loading}
        className="rounded-lg bg-indigo-600 px-5 py-2 text-sm font-medium text-white transition hover:bg-indigo-700 disabled:opacity-50"
      >
        {loading ? "提交中…" : "发送"}
      </button>
    </form>
  );
}
