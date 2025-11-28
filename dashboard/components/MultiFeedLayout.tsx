"use client";

import { useEffect, useState } from "react";
import VideoCanvas from "@/components/VideoCanvas";
import type { TelemetryPacket } from "@/lib/wsClient";

type MultiFeedLayoutProps = {
  cameraIds: string[];
  telemetry: Record<string, TelemetryPacket | null | undefined>;
  telemetryReady: boolean;
};

export default function MultiFeedLayout({
  cameraIds,
  telemetry,
  telemetryReady,
}: MultiFeedLayoutProps) {
  const [time, setTime] = useState<string | null>(null);

  useEffect(() => {
    setTime(new Date().toLocaleTimeString());
    const interval = setInterval(() => {
      setTime(new Date().toLocaleTimeString());
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  return (
    <section className="hud-panel p-4">
      <header className="mb-4 flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.35em] text-slate-500">
            Live Video Grid
          </p>
          <h2 className="text-xl font-semibold text-white">
            Multi-Camera Telemetry
          </h2>
        </div>
        <span className="text-xs text-slate-400">
          {time ?? "--:--:-- --"}
        </span>
      </header>

      <div className="grid gap-4 lg:grid-cols-3 md:grid-cols-2 grid-cols-1">
        {cameraIds.map((cameraId) => (
          <div
            key={cameraId}
            className="rounded-2xl border border-white/10 bg-black/40 backdrop-blur-md"
          >
            <div className="flex items-center justify-between border-b border-white/5 px-4 py-3 text-xs uppercase tracking-[0.3em] text-slate-400">
              <span className="text-white text-sm tracking-[0.2em]">
                {cameraId}
              </span>
              <span className="flex items-center gap-2 text-[10px]">
                <span
                  className={`h-2 w-2 rounded-full ${
                    telemetryReady ? "bg-emerald-400 animate-pulse" : "bg-zinc-600"
                  }`}
                />
                {telemetryReady
                  ? `${telemetry[cameraId]?.objects?.length ?? 0} Objects`
                  : "Awaiting telemetry"}
              </span>
            </div>
            <VideoCanvas cameraId={cameraId} data={telemetry[cameraId] ?? null} />
          </div>
        ))}
      </div>
    </section>
  );
}
