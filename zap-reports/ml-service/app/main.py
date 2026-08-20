from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Dict, List, Optional

import joblib
import pandas as pd
from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, ConfigDict, Field

BASE_DIR = Path(__file__).resolve().parent.parent
MODEL_PATH = Path(os.getenv("RPM_ML_MODEL_PATH", BASE_DIR / "model" / "modelo_rpm_rf_humano_v1.joblib"))
METRICS_PATH = Path(os.getenv("RPM_ML_METRICS_PATH", BASE_DIR / "model" / "metricas_ml_validacion_humana_80.json"))
MODEL_VERSION = os.getenv("RPM_ML_MODEL_VERSION", "RPM-ML-RF-HUMANO-V1")
API_KEY = os.getenv("ML_API_KEY", "").strip()
HUMAN_REVIEW_THRESHOLD = float(os.getenv("RPM_ML_HUMAN_REVIEW_THRESHOLD", "0.70"))

MODEL = joblib.load(MODEL_PATH)
MODEL_FEATURES = list(getattr(MODEL, "feature_names_in_", []))
MODEL_CLASSES = list(MODEL.named_steps["model"].classes_)

try:
    METRICS = json.loads(METRICS_PATH.read_text(encoding="utf-8")) if METRICS_PATH.exists() else {}
except Exception:
    METRICS = {}

app = FastAPI(
    title="Global ISO Security - RPM ML",
    version=MODEL_VERSION,
    description=(
        "Servicio experimental de aprendizaje supervisado para apoyar la priorización RPM. "
        "La predicción no sustituye la validación humana del SGSI."
    ),
)


def require_api_key(x_ml_api_key: Optional[str] = Header(default=None)) -> None:
    if API_KEY and x_ml_api_key != API_KEY:
        raise HTTPException(status_code=401, detail="API key inválida")


class FeatureInput(BaseModel):
    model_config = ConfigDict(extra="ignore")

    analysis_id: Optional[int] = None
    sector: Optional[str] = None
    tamano: Optional[str] = None
    control_dominio: Optional[str] = None
    control_humano: Optional[int] = Field(default=None, ge=0, le=1)
    aplicabilidad: Optional[str] = None
    estado_implementacion: Optional[str] = None
    porcentaje_implementacion: Optional[float] = None
    puntaje_relevancia: Optional[float] = None
    fecha_objetivo_vencida: Optional[int] = Field(default=None, ge=0, le=1)
    probabilidad: Optional[float] = None
    impacto: Optional[float] = None
    nivel_inherente: Optional[float] = None
    riesgo_categoria: Optional[str] = None
    evidencias_total: Optional[int] = None
    evidencias_pendientes: Optional[int] = None
    evidencias_rechazadas: Optional[int] = None
    evidencias_vencidas: Optional[int] = None
    hallazgos_abiertos: Optional[int] = None
    hallazgos_recurrentes: Optional[int] = None
    hallazgo_severidad_ordinal: Optional[int] = None


class BatchRequest(BaseModel):
    items: List[FeatureInput]


def _frame(items: List[FeatureInput]) -> pd.DataFrame:
    rows = []
    for item in items:
        data = item.model_dump()
        rows.append({name: data.get(name) for name in MODEL_FEATURES})
    return pd.DataFrame(rows, columns=MODEL_FEATURES)


def _prediction_payload(items: List[FeatureInput]) -> List[dict]:
    if not items:
        return []
    frame = _frame(items)
    predicted = MODEL.predict(frame)
    probabilities = MODEL.predict_proba(frame)
    results = []
    for idx, item in enumerate(items):
        probs: Dict[str, float] = {
            str(cls): round(float(probabilities[idx][i]), 6)
            for i, cls in enumerate(MODEL_CLASSES)
        }
        confidence = max(probs.values()) if probs else 0.0
        results.append(
            {
                "analysis_id": item.analysis_id,
                "priority": str(predicted[idx]),
                "confidence": round(confidence, 6),
                "probabilities": probs,
                "model_version": MODEL_VERSION,
                "requires_human_review": confidence < HUMAN_REVIEW_THRESHOLD,
                "confidence_note": "Probabilidad estimada experimental; no calibrada como certeza clínica o profesional.",
            }
        )
    return results


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "model_loaded": True,
        "model_version": MODEL_VERSION,
        "features": len(MODEL_FEATURES),
    }


@app.get("/metadata")
def metadata() -> dict:
    rf = (METRICS.get("models") or {}).get("RandomForest", {})
    return {
        "model_version": MODEL_VERSION,
        "algorithm": "RandomForestClassifier",
        "training_validation_cases": 80,
        "validation_reference": "validación humana independiente sobre 80 escenarios sintéticos controlados",
        "cross_validation_accuracy": rf.get("accuracy"),
        "cross_validation_f1_macro": rf.get("f1_macro"),
        "cross_validation_kappa": rf.get("kappa"),
        "classes": MODEL_CLASSES,
        "features": MODEL_FEATURES,
        "human_review_threshold": HUMAN_REVIEW_THRESHOLD,
        "warning": "Modelo experimental de apoyo. La decisión final permanece en el responsable humano.",
    }


@app.post("/predict", dependencies=[Depends(require_api_key)])
def predict(item: FeatureInput) -> dict:
    return _prediction_payload([item])[0]


@app.post("/predict/batch", dependencies=[Depends(require_api_key)])
def predict_batch(payload: BatchRequest) -> dict:
    if len(payload.items) > 500:
        raise HTTPException(status_code=400, detail="Máximo 500 elementos por lote")
    return {
        "model_version": MODEL_VERSION,
        "predictions": _prediction_payload(payload.items),
    }
