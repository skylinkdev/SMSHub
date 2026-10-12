"use client";

import { useRouter } from "next/navigation";

export default function MessageDeleteButton({ id }: { id: string }) {
  const router = useRouter();

  async function remove() {
    if (!confirm("确定删除这条短信记录吗？")) return;
    await fetch(`/api/messages/${id}`, { method: "DELETE" });
    router.refresh();
  }

  return (
    <button onClick={remove} className="text-sm text-red-600 hover:underline">
      删除
    </button>
  );
}
