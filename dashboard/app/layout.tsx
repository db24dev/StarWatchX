import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";
import { Inter } from "next/font/google";

const inter = Inter({ subsets: ["latin"], display: "swap" });

export const metadata: Metadata = {
  title: "StarWatch-X Dashboard",
  description: "Mission control interface for STARWATCH-X telemetry",
};

export default function RootLayout({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <html lang="en">
      <body className={`${inter.className} bg-hud-bg text-white min-h-screen`}>
        {children}
      </body>
    </html>
  );
}
