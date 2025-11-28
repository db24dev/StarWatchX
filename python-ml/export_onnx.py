#!/usr/bin/env python3
"""
STARWATCH-X ONNX export helper.

Dependencies:
    python -m venv .venv && source .venv/bin/activate
    pip install --upgrade pip
    pip install ultralytics torch torchvision onnx onnxruntime

Train the model:
    python train_yolo.py --epochs 10

Export to ONNX:
    python export_onnx.py --weights weights/best.pt
"""

import argparse
import shutil
from pathlib import Path


PYTHON_ML_DIR = Path(__file__).resolve().parent
DEFAULT_WEIGHTS = PYTHON_ML_DIR / "weights" / "best.pt"
DEFAULT_OUTPUT = (
    PYTHON_ML_DIR.parent
    / "java-engine"
    / "src"
    / "main"
    / "resources"
    / "model"
    / "starwatchx_yolov8.onnx"
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Export YOLOv8 weights to ONNX for STARWATCH-X.")
    parser.add_argument(
        "--weights",
        type=Path,
        default=DEFAULT_WEIGHTS,
        help="Path to the trained *.pt weights file.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT,
        help="Destination ONNX path for the Java engine.",
    )
    parser.add_argument(
        "--imgsz",
        type=int,
        default=640,
        help="Image size that matches training (default: 640).",
    )
    parser.add_argument(
        "--dynamic",
        action="store_true",
        help="Enable dynamic axes for ONNX (slightly slower but flexible).",
    )
    parser.add_argument(
        "--opset",
        type=int,
        default=12,
        help="ONNX opset version (default: 12).",
    )
    return parser.parse_args()


def export_to_onnx() -> Path:
    args = parse_args()
    weights_path = args.weights.expanduser().resolve()
    output_path = args.output.expanduser().resolve()

    if not weights_path.exists():
        raise SystemExit(f"Missing weights file: {weights_path}\nRun train_yolo.py first.")

    from ultralytics import YOLO  # Lazy import for friendlier error messages

    print("🚀 STARWATCH-X :: Exporting YOLOv8 weights to ONNX")
    print(f"Input weights : {weights_path}")
    print(f"Output ONNX   : {output_path}")
    print(f"Image size    : {args.imgsz}")
    print(f"Dynamic axes  : {args.dynamic}")

    model = YOLO(str(weights_path))
    exported_path = Path(
        model.export(
            format="onnx",
            imgsz=args.imgsz,
            dynamic=args.dynamic,
            simplify=True,
            opset=args.opset,
        )
    )

    output_path.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(exported_path, output_path)
    print(f"\n✅ ONNX export complete. Model written to {output_path}")
    return output_path


if __name__ == "__main__":
    export_to_onnx()
