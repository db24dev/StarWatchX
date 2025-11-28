import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx}",
    "./components/**/*.{js,ts,jsx,tsx}",
    "./lib/**/*.{js,ts,jsx,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        "hud-bg": "#050914",
        "hud-panel": "#0f172a",
        "hud-accent": "#2dd4ff",
        "hud-emerald": "#48ffe1",
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
        mono: ["Space Mono", "SFMono-Regular", "Menlo", "monospace"],
      },
      boxShadow: {
        "hud-glow": "0 0 40px rgba(0, 191, 255, 0.15)",
      },
    },
  },
  plugins: [],
};

export default config;
