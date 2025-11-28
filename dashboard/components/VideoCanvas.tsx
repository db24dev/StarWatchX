"use client";

import { useEffect, useRef, useState } from "react";
import type { TelemetryPacket } from "@/lib/wsClient";

interface VideoCanvasProps {
  cameraId: string;
  data: TelemetryPacket | null;
}

const FRAME_WIDTH = 1280;
const FRAME_HEIGHT = 720;
const VIDEO_SOURCES: Record<string, string> = {
  "CAM-1": "/videos/cam1.mp4",
  "CAM-2": "/videos/cam2.mp4",
  "CAM-3": "/videos/cam3.mp4",
};

export default function VideoCanvas({ cameraId, data }: VideoCanvasProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [dimensions, setDimensions] = useState({ width: 320, height: 200 });

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const observer = new ResizeObserver((entries) => {
      const entry = entries[0];
      if (!entry) return;
      const width = entry.contentRect.width;
      const height = width * (FRAME_HEIGHT / FRAME_WIDTH);
      setDimensions({ width, height });
    });

    observer.observe(container);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const { width, height } = dimensions;
    canvas.width = width;
    canvas.height = height;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    // Clear canvas (transparent background so video shows through)
    ctx.clearRect(0, 0, width, height);

    // Semi-transparent grid overlay
    ctx.strokeStyle = "rgba(45, 212, 255, 0.15)";
    ctx.lineWidth = 0.5;
    for (let x = 0; x < width; x += 40) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
      ctx.stroke();
    }
    for (let y = 0; y < height; y += 40) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
      ctx.stroke();
    }

    // Camera ID label with background
    ctx.font = "bold 12px 'Space Mono', monospace";
    const labelText = cameraId;
    const labelWidth = ctx.measureText(labelText).width + 16;
    ctx.fillStyle = "rgba(0, 0, 0, 0.6)";
    ctx.fillRect(8, 8, labelWidth, 22);
    ctx.fillStyle = "#2dd4ff";
    ctx.fillText(labelText, 16, 23);

    const scaleX = width / FRAME_WIDTH;
    const scaleY = height / FRAME_HEIGHT;

    data?.objects.forEach((object) => {
      const boxX = object.x * scaleX;
      const boxY = object.y * scaleY;
      const boxWidth = Math.max(12, object.width * scaleX);
      const boxHeight = Math.max(12, object.height * scaleY);

      ctx.strokeStyle = "#2dd4ff";
      ctx.lineWidth = 2;
      ctx.strokeRect(boxX, boxY, boxWidth, boxHeight);

      ctx.fillStyle = "rgba(45, 212, 255, 0.15)";
      ctx.fillRect(boxX, boxY, boxWidth, boxHeight);

      const label = `${object.label} ${(object.confidence * 100).toFixed(0)}%`;
      ctx.fillStyle = "#e0f2fe";
      ctx.fillText(label, boxX + 4, boxY - 6);

      const centerX = boxX + boxWidth / 2;
      const centerY = boxY + boxHeight / 2;
      const velocityScale = 12;
      ctx.strokeStyle = "#4ade80";
      ctx.beginPath();
      ctx.moveTo(centerX, centerY);
      ctx.lineTo(
        centerX + object.vx * velocityScale,
        centerY + object.vy * velocityScale
      );
      ctx.stroke();
      ctx.fillStyle = "#4ade80";
      ctx.fillText(
        `→ ${object.vx.toFixed(1)}, ${object.vy.toFixed(1)}`,
        centerX + 6,
        centerY - 6
      );
    });
  }, [data, dimensions, cameraId]);

  const videoSrc = VIDEO_SOURCES[cameraId] ?? "/videos/cam1.mp4";

  return (
    <div ref={containerRef} className="p-4">
      <div
        className="relative w-full overflow-hidden rounded-2xl border border-white/10 shadow-inner shadow-black/30"
        style={{ minHeight: 180, height: dimensions.height }}
      >
        <video
          src={videoSrc}
          autoPlay
          muted
          loop
          playsInline
          preload="auto"
          className="absolute inset-0 z-0"
          style={{ width: "100%", height: "100%", objectFit: "cover" }}
        />
        <canvas
          ref={canvasRef}
          className="absolute inset-0 z-10 pointer-events-none"
          style={{ minHeight: 180 }}
        />
      </div>
    </div>
  );
}
