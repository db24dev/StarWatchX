#!/usr/bin/env python3
"""
STARWATCH-X YOLOv8 training utility.

Dependencies:
    python -m venv .venv && source .venv/bin/activate
    pip install --upgrade pip
    pip install ultralytics torch torchvision onnx

Train the model:
    python train_yolo.py --epochs 10 --imgsz 640 --batch 16

Export to ONNX after training:
    python export_onnx.py --weights weights/best.pt
"""

import argparse
import shutil
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent
DEFAULT_DATA_CONFIG = PROJECT_ROOT / "data.yaml"
WEIGHTS_DIR = PROJECT_ROOT / "weights"
DEFAULT_OUTPUT_WEIGHTS = WEIGHTS_DIR / "best.pt"
DEFAULT_BASE_MODEL = "yolov8n.pt"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train YOLOv8 for STARWATCH-X.")
    parser.add_argument(
        "--data",
        type=Path,
        default=DEFAULT_DATA_CONFIG,
        help="Path to data.yaml describing the dataset.",
    )
    parser.add_argument(
        "--epochs",
        type=int,
        default=10,
        help="Number of training epochs (default: 10).",
    )
    parser.add_argument(
        "--imgsz",
        type=int,
        default=640,
        help="Image size for training (default: 640).",
    )
    parser.add_argument(
        "--batch",
        type=int,
        default=16,
        help="Batch size (default: 16).",
    )
    parser.add_argument(
        "--workers",
        type=int,
        default=4,
        help="Number of dataloader workers (default: 4).",
    )
    parser.add_argument(
        "--device",
        default=None,
        help="Torch device override, e.g. '0' or 'cpu'.",
    )
    parser.add_argument(
        "--base-model",
        default=DEFAULT_BASE_MODEL,
        help="Base YOLOv8 checkpoint to fine-tune (default: yolov8n.pt).",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT_WEIGHTS,
        help="Where to copy the best weights after training.",
    )
    parser.add_argument(
        "--run-name",
        default="starwatchx",
        help="Name of the Ultralytics run folder.",
    )
    parser.add_argument(
        "--project",
        type=Path,
        default=PROJECT_ROOT / "runs",
        help="Parent directory for Ultralytics runs.",
    )
    return parser.parse_args()


def train() -> Path:
    args = parse_args()

    from ultralytics import YOLO  # Imported lazily so pip guidance above is meaningful

    print("🚀 STARWATCH-X :: YOLOv8 fine-tuning")
    print(f"Base model      : {args.base_model}")
    print(f"Data config     : {args.data}")
    print(f"Epochs          : {args.epochs}")
    print(f"Image size      : {args.imgsz}")
    print(f"Batch size      : {args.batch}")
    print(f"Dataloader workers: {args.workers}")

    WEIGHTS_DIR.mkdir(parents=True, exist_ok=True)
    args.project.mkdir(parents=True, exist_ok=True)

    model = YOLO(args.base_model)
    results = model.train(
        data=str(args.data),
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        workers=args.workers,
        project=str(args.project),
        name=args.run_name,
        exist_ok=True,
        device=args.device,
    )

    save_dir = Path(getattr(results, "save_dir", args.project / args.run_name))
    best_pt = save_dir / "weights" / "best.pt"
    if not best_pt.exists():
        raise FileNotFoundError(f"Ultralytics did not produce {best_pt}")

    dest = args.output
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(best_pt, dest)
    print(f"\n✅ Training complete. Best weights copied to {dest}")
    return dest


if __name__ == "__main__":
    train()
