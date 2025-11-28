"use client";

import type { TelemetryPacket } from "@/lib/wsClient";

interface TelemetryPanelProps {
  cameraId: string;
  data: TelemetryPacket | null;
  telemetryReady: boolean;
}

export default function TelemetryPanel({
  cameraId,
  data,
  telemetryReady,
}: TelemetryPanelProps) {
  const status = (() => {
    if (!telemetryReady) return "Waiting for telemetry packets…";
    if (!data) return "No data yet for this camera.";
    if (data.objects.length === 0) return "No detections.";
    return `${data.objects.length} detections locked`;
  })();

  return (
    <section className="hud-panel flex h-full flex-col p-5">
      <header className="mb-4">
        <p className="text-xs uppercase tracking-[0.4em] text-slate-500">
          Telemetry
        </p>
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-semibold tracking-wide">{cameraId}</h2>
          <span className="text-xs text-slate-400">
            {telemetryReady && data
              ? new Date(data.timestamp).toLocaleTimeString()
              : "Waiting..."}
          </span>
        </div>
        <p className="text-sm text-slate-400">{status}</p>
      </header>

      <div className="overflow-auto rounded-2xl border border-white/5 bg-black/40">
        <table className="w-full table-auto text-sm text-slate-200">
          <thead className="bg-white/5 text-xs uppercase tracking-widest text-slate-400">
            <tr>
              <th className="px-3 py-2 text-left">ID</th>
              <th className="px-3 py-2 text-left">Label</th>
              <th className="px-3 py-2 text-left">Confidence</th>
              <th className="px-3 py-2 text-left">Velocity (px/s)</th>
            </tr>
          </thead>
          <tbody>
            {!telemetryReady ? (
              <tr>
                <td
                  colSpan={4}
                  className="px-3 py-8 text-center text-sm text-slate-500"
                >
                  Waiting for telemetry packets...
                </td>
              </tr>
            ) : !data ? (
              <tr>
                <td
                  colSpan={4}
                  className="px-3 py-8 text-center text-sm text-slate-500"
                >
                  No data yet for this camera.
                </td>
              </tr>
            ) : data.objects.length > 0 ? (
              data.objects.map((object) => (
                <tr key={object.id} className="border-t border-white/5 text-sm">
                  <td className="px-3 py-2 font-mono text-xs text-hud-emerald">
                    {object.id}
                  </td>
                  <td className="px-3 py-2 capitalize">{object.label}</td>
                  <td className="px-3 py-2">
                    {(object.confidence * 100).toFixed(0)}
                    <span className="text-xs text-slate-500">%</span>
                  </td>
                  <td className="px-3 py-2 font-mono text-xs">
                    {(object.vx ?? 0).toFixed(1)}, {(object.vy ?? 0).toFixed(1)}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td
                  colSpan={4}
                  className="px-3 py-8 text-center text-sm text-slate-500"
                >
                  No detections reported for this camera.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
