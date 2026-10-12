"use client";

import { useRouter } from "next/navigation";

export default function DeviceDeleteButton({
  id,
  name,
}: {
  id: string;
  name: string;
}) {
  const router = useRouter();

  async function remove() {
    if (!confirm(`确定删除设备「${name}」吗？该设备的所有短信记录将一并删除。`)) {
      return;
    }
    await fetch(`/api/devices/${id}`, { method: "DELETE" });
    router.refresh();
  }

  return (
    <button
      onClick={remove}
      className="text-sm text-red-600 hover:underline"
    >
      删除
    </button>
  );
}
