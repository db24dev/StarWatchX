"use client";

import { useEffect, useMemo, useState } from "react";
import MultiFeedLayout from "@/components/MultiFeedLayout";
import TelemetryPanel from "@/components/TelemetryPanel";
import { subscribeTelemetry, type TelemetryPacket } from "@/lib/wsClient";

const CAMERA_IDS = ["CAM-1", "CAM-2", "CAM-3"];

export default function Page() {
  const [telemetryReady, setTelemetryReady] = useState(false);
  const [latestByCamera, setLatestByCamera] = useState<
    Record<string, TelemetryPacket | null>
  >({});
  const [activeCamera, setActiveCamera] = useState<string>(CAMERA_IDS[0]);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }

    const unsubscribe = subscribeTelemetry((packet) => {
      setTelemetryReady(true);
      setLatestByCamera((prev) => ({
        ...prev,
        [packet.cameraId]: packet,
      }));
    });

    return () => {
      unsubscribe();
    };
  }, []);

  const activeTelemetry = useMemo(
    () => latestByCamera[activeCamera] ?? null,
    [latestByCamera, activeCamera]
  );

  return (
    <main className="min-h-screen bg-hud-bg p-6 text-white">
      <header className="mb-6 space-y-1">
        <p className="text-xs uppercase tracking-[0.4em] text-hud-emerald">
          StarWatch-X
        </p>
        <h1 className="text-4xl font-semibold tracking-tight">
          Space Object Mission Control
        </h1>
        <p className="text-sm text-slate-400">
          Real-time detections, tracking, and telemetry streaming from the Java
          engine.
        </p>
      </header>

      <section className="hud-panel mb-6 flex flex-wrap items-center gap-3 p-4 text-sm text-slate-200">
        <span className="text-xs uppercase tracking-[0.4em] text-slate-500">
          Active Camera
        </span>
        <div className="flex flex-wrap gap-2">
          {CAMERA_IDS.map((cameraId) => (
            <button
              key={cameraId}
              onClick={() => setActiveCamera(cameraId)}
              className={`rounded-full border border-white/10 px-4 py-2 text-xs tracking-widest transition ${
                activeCamera === cameraId
                  ? "bg-hud-accent/20 text-white"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              {cameraId}
            </button>
          ))}
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
        <MultiFeedLayout
          cameraIds={CAMERA_IDS}
          telemetry={latestByCamera}
          telemetryReady={telemetryReady}
        />
        <TelemetryPanel
          cameraId={activeCamera}
          data={activeTelemetry}
          telemetryReady={telemetryReady}
        />
      </div>

      {process.env.NODE_ENV === "development" && telemetryReady && (
        <section className="hud-panel mt-6 max-h-80 overflow-auto bg-black/70 p-4 text-xs text-slate-300">
          <p className="mb-2 font-semibold uppercase tracking-[0.3em] text-slate-500">
            Debug Telemetry
          </p>
          <pre>{JSON.stringify(latestByCamera, null, 2)}</pre>
        </section>
      )}
    </main>
  );
}
