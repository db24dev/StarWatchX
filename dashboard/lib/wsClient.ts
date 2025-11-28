"use client";

const DEFAULT_TELEMETRY_PATH =
  process.env.NEXT_PUBLIC_TELEMETRY_PATH?.trim() || "/";

const NORMALIZED_PATH =
  DEFAULT_TELEMETRY_PATH === "/" || DEFAULT_TELEMETRY_PATH === ""
    ? ""
    : DEFAULT_TELEMETRY_PATH.startsWith("/")
      ? DEFAULT_TELEMETRY_PATH
      : `/${DEFAULT_TELEMETRY_PATH}`;

export const TELEMETRY_WS_URL =
  process.env.NEXT_PUBLIC_TELEMETRY_URL ||
  `ws://localhost:8081${NORMALIZED_PATH || "/"}`;

export type TelemetryObject = {
  id: string;
  label: string;
  confidence: number;
  x: number;
  y: number;
  width: number;
  height: number;
  vx?: number;
  vy?: number;
};

export type TelemetryPacket = {
  cameraId: string;
  timestamp: number;
  objects: TelemetryObject[];
};

export type TelemetryListener = (packet: TelemetryPacket) => void;

const listeners = new Set<TelemetryListener>();
const RECONNECT_DELAY_MS = 3000;

let socket: WebSocket | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let shouldReconnect = true;

function ensureConnection() {
  if (typeof window === "undefined") {
    return;
  }
  if (
    socket &&
    (socket.readyState === WebSocket.OPEN ||
      socket.readyState === WebSocket.CONNECTING)
  ) {
    return;
  }

  socket = new WebSocket(TELEMETRY_WS_URL);

  socket.onopen = () => {
    console.info("[Telemetry] Connected to", TELEMETRY_WS_URL);
  };

  socket.onerror = (event) => {
    console.error("[Telemetry] WebSocket error", event);
  };

  socket.onclose = (event) => {
    console.warn(
      `[Telemetry] Connection closed (${event.code}: ${
        event.reason || "no reason"
      })`
    );
    if (shouldReconnect) {
      scheduleReconnect();
    }
  };

  socket.onmessage = (event) => {
    const payload = event.data;
    if (typeof payload !== "string") {
      console.error("[Telemetry] Non-string payload received", payload);
      return;
    }
    try {
      const parsed = JSON.parse(payload);
      if (!parsed || typeof parsed.cameraId !== "string") {
        console.error("[Telemetry] Invalid packet shape", parsed);
        return;
      }
      const objects = Array.isArray(parsed.objects)
        ? parsed.objects.map((obj: Partial<TelemetryObject>) => ({
            id: String(obj.id ?? "untracked"),
            label: String(obj.label ?? "object"),
            confidence: Number(obj.confidence ?? 0),
            x: Number(obj.x ?? 0),
            y: Number(obj.y ?? 0),
            width: Number(obj.width ?? 0),
            height: Number(obj.height ?? 0),
            vx:
              obj.vx === undefined || obj.vx === null
                ? undefined
                : Number(obj.vx),
            vy:
              obj.vy === undefined || obj.vy === null
                ? undefined
                : Number(obj.vy),
          }))
        : [];

      const packet: TelemetryPacket = {
        cameraId: parsed.cameraId,
        timestamp: Number(parsed.timestamp ?? Date.now()),
        objects,
      };
      listeners.forEach((listener) => listener(packet));
    } catch (err) {
      console.error(
        "[Telemetry] Failed to parse message",
        payload,
        err instanceof Error ? err : undefined
      );
    }
  };
}

function scheduleReconnect() {
  if (reconnectTimer !== null) {
    return;
  }
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null;
    ensureConnection();
  }, RECONNECT_DELAY_MS);
}

function teardownConnection() {
  shouldReconnect = false;
  if (reconnectTimer !== null) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  if (socket) {
    socket.close();
    socket = null;
  }
}

export function subscribeTelemetry(listener: TelemetryListener) {
  listeners.add(listener);
  if (typeof window !== "undefined") {
    shouldReconnect = true;
    ensureConnection();
  }
  return () => {
    listeners.delete(listener);
    if (listeners.size === 0) {
      teardownConnection();
    }
  };
}
