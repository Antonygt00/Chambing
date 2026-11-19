"""
Microservicio de probabilidad de aprobación.

    pip install -r requirements.txt
    python entrenar_modelo.py
    uvicorn app:app --port 8001

POST /predict        { ...features }
POST /predict/batch  { "items": [ ...features ] }
GET  /salud
"""
import json
from pathlib import Path

import joblib
import pandas as pd
from fastapi import FastAPI
from pydantic import BaseModel, Field

BASE = Path(__file__).resolve().parent
COLUMNAS = [
    "edad", "misma_ciudad", "salario", "cantidad_requisitos", "num_postulantes",
    "num_postulaciones_previas", "pct_aprobadas", "anios_experiencia",
    "nivel_educativo", "skills_coincidentes", "categoria",
]

app = FastAPI(title="Chambing predicción")
_modelo = None
_meta = {}


class Features(BaseModel):
    edad: float = 28
    misma_ciudad: float = 0
    salario: float = 0
    cantidad_requisitos: float = 1
    num_postulantes: float = 0
    num_postulaciones_previas: float = 0
    pct_aprobadas: float = 0.35
    anios_experiencia: float = 0
    nivel_educativo: float = 1
    skills_coincidentes: float = 0
    categoria: str = "General"


class Lote(BaseModel):
    items: list[Features] = Field(default_factory=list)


def cargar():
    global _modelo, _meta
    ruta = BASE / "modelo.pkl"
    if ruta.exists():
        _modelo = joblib.load(ruta)
    meta = BASE / "metadata.json"
    if meta.exists():
        _meta = json.loads(meta.read_text(encoding="utf-8"))


def etiqueta(probabilidad: float) -> str:
    alta = float(_meta.get("umbral_alta", 0.66))
    media = float(_meta.get("umbral_media", 0.40))
    if probabilidad >= alta:
        return "Alta"
    if probabilidad >= media:
        return "Media"
    return "Baja"


def responder(fila: Features) -> dict:
    datos = fila.model_dump()
    if _modelo is None:
        # Misma idea que la heurística de Spring, por si el pkl todavía no se entrenó.
        p = 0.35 + 0.12 * datos["misma_ciudad"] + 0.04 * min(datos["anios_experiencia"], 8)
        p += 0.03 * datos["nivel_educativo"] + 0.06 * min(datos["skills_coincidentes"], 5)
        p += 0.25 * datos["pct_aprobadas"] - 0.004 * datos["cantidad_requisitos"]
        p = max(0.05, min(0.95, p))
        return {"probabilidad": round(p, 3), "etiqueta": etiqueta(p), "modelo": "heuristica"}
    frame = pd.DataFrame([{c: datos[c] for c in COLUMNAS}])
    p = float(_modelo.predict_proba(frame)[0, 1])
    nombre = _meta.get("elegido", {}).get("modelo", "sklearn")
    return {"probabilidad": round(p, 3), "etiqueta": etiqueta(p), "modelo": nombre}


@app.on_event("startup")
def inicio():
    cargar()


@app.get("/salud")
def salud():
    return {"modelo": _modelo is not None, "metadata": bool(_meta)}


@app.post("/predict")
def predict(fila: Features):
    return responder(fila)


@app.post("/predict/batch")
def predict_batch(lote: Lote):
    return {"resultados": [responder(item) for item in lote.items]}
