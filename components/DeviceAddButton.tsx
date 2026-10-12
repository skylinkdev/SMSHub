"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import CopyButton from "@/components/CopyButton";

export default function DeviceAddButton() {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [created, setCreated] = useState<{ name: string; deviceKey: string } | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await fetch("/api/devices", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name }),
      });
      const data = await res.json();
      if (!res.ok) {
        setError(data.error || "创建失败");
        return;
      }
      setCreated({ name: data.name, deviceKey: data.deviceKey });
      router.refresh();
    } catch {
      setError("网络错误，请重试");
    } finally {
      setLoading(false);
    }
  }

  function close() {
    setOpen(false);
    setName("");
    setError("");
    setCreated(null);
  }

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-700"
      >
        + 添加设备
      </button>

      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
            {created ? (
              <>
                <h3 className="text-lg font-semibold text-slate-900">
                  设备创建成功
                </h3>
                <p className="mt-2 text-sm text-slate-500">
                  请在安卓 App 的「服务器配置」中填入以下设备密钥完成绑定。密钥仅此一次完整展示，请妥善保存。
                </p>
                <div className="mt-4 rounded-lg bg-slate-50 p-4">
                  <p className="text-xs text-slate-500">设备名称</p>
                  <p className="font-medium">{created.name}</p>
                  <p className="mt-3 text-xs text-slate-500">设备密钥</p>
                  <div className="mt-1 flex items-center gap-2">
                    <code className="break-all text-sm font-semibold text-indigo-600">
                      {created.deviceKey}
                    </code>
                    <CopyButton text={created.deviceKey} />
                  </div>
                </div>
                <button
                  onClick={close}
                  className="mt-5 w-full rounded-lg bg-indigo-600 py-2 text-sm font-medium text-white hover:bg-indigo-700"
                >
                  完成
                </button>
              </>
            ) : (
              <form onSubmit={submit}>
                <h3 className="text-lg font-semibold text-slate-900">添加设备</h3>
                <p className="mt-1 text-sm text-slate-500">
                  为每台安装 App 的手机创建一个设备。
                </p>
                <input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  autoFocus
                  placeholder="例如：小米 14 / 备用机"
                  className="mt-4 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
                />
                {error && (
                  <p className="mt-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
                    {error}
                  </p>
                )}
                <div className="mt-5 flex justify-end gap-2">
                  <button
                    type="button"
                    onClick={close}
                    className="rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50"
                  >
                    取消
                  </button>
                  <button
                    type="submit"
                    disabled={loading}
                    className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
                  >
                    {loading ? "创建中…" : "创建"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </>
  );
}
