"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

// 轮询新短信，有新数据时自动刷新服务端组件
export default function AutoRefresh({ intervalMs = 10000 }: { intervalMs?: number }) {
  const router = useRouter();
  const since = useRef(Date.now());
  const [newCount, setNewCount] = useState(0);

  useEffect(() => {
    const timer = setInterval(async () => {
      try {
        const res = await fetch(`/api/messages/latest?since=${since.current}`);
        if (!res.ok) return;
        const data = await res.json();
        since.current = data.serverTime;
        if (data.count > 0) {
          setNewCount((c) => c + data.count);
          router.refresh();
        }
      } catch {
        // 网络异常时下一轮再试
      }
    }, intervalMs);
    return () => clearInterval(timer);
  }, [intervalMs, router]);

  if (newCount === 0) return null;
  return (
    <button
      onClick={() => setNewCount(0)}
      className="fixed bottom-6 right-6 z-50 rounded-full bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-lg transition hover:bg-indigo-700"
    >
      收到 {newCount} 条新短信 · 已更新
    </button>
  );
}
