"use client";

import { useState } from "react";

const PREVIEW_LEN = 60;

/** 短信内容：默认截断展示，点击「展开」显示全部 */
export default function MessageBody({ body }: { body: string }) {
  const [expanded, setExpanded] = useState(false);
  const isLong = body.length > PREVIEW_LEN;

  if (!isLong) {
    return <span className="break-all">{body}</span>;
  }

  return (
    <span className="break-all">
      {expanded ? body : `${body.slice(0, PREVIEW_LEN)}…`}
      <button
        type="button"
        onClick={() => setExpanded((v) => !v)}
        className="ml-2 whitespace-nowrap text-indigo-600 hover:underline"
      >
        {expanded ? "收起" : "展开"}
      </button>
    </span>
  );
}
