"""
Entrena y compara Regresión Logística, Random Forest y (si está instalado) XGBoost.
Guarda el mejor modelo en prediccion/modelo.pkl y las métricas en prediccion/metadata.json.

Reentrenar cuando haya postulaciones reales:
1. Exporta de Mongo una fila por postulación con las mismas columnas que el CSV
   (edad, misma_ciudad, salario, cantidad_requisitos, num_postulantes,
    num_postulaciones_previas, pct_aprobadas, anios_experiencia, nivel_educativo,
    skills_coincidentes, categoria, aprobada).
2. Guárdalo como prediccion/data/postulaciones_reales.csv
3. python entrenar_modelo.py --csv data/postulaciones_reales.csv
4. Reinicia el servicio: uvicorn app:app --port 8001

Sin --csv usa el dataset simulado (lo genera si no existe).
"""
import argparse
import json
from pathlib import Path

import joblib
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score, roc_auc_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

COLUMNAS = [
    "edad", "misma_ciudad", "salario", "cantidad_requisitos", "num_postulantes",
    "num_postulaciones_previas", "pct_aprobadas", "anios_experiencia",
    "nivel_educativo", "skills_coincidentes", "categoria",
]
NUM = COLUMNAS[:-1]
CAT = ["categoria"]
BASE = Path(__file__).resolve().parent


def cargar(csv: Path) -> pd.DataFrame:
    if not csv.exists():
        from generar_datos_simulados import main as generar
        generar()
    df = pd.read_csv(csv)
    faltan = [c for c in COLUMNAS + ["aprobada"] if c not in df.columns]
    if faltan:
        raise SystemExit(f"Al CSV le faltan columnas: {faltan}")
    return df


def pipeline(modelo):
    pre = ColumnTransformer([
        ("num", StandardScaler(), NUM),
        ("cat", OneHotEncoder(handle_unknown="ignore"), CAT),
    ])
    return Pipeline([("pre", pre), ("modelo", modelo)])


def evaluar(nombre, modelo, x_train, x_test, y_train, y_test):
    modelo.fit(x_train, y_train)
    proba = modelo.predict_proba(x_test)[:, 1]
    pred = (proba >= 0.5).astype(int)
    metricas = {
        "modelo": nombre,
        "accuracy": round(float(accuracy_score(y_test, pred)), 4),
        "f1": round(float(f1_score(y_test, pred)), 4),
        "roc_auc": round(float(roc_auc_score(y_test, proba)), 4),
    }
    print(f"{nombre}: accuracy={metricas['accuracy']} f1={metricas['f1']} roc_auc={metricas['roc_auc']}")
    return modelo, metricas


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", default=str(BASE / "data" / "postulaciones_simuladas.csv"))
    args = parser.parse_args()
    df = cargar(Path(args.csv))
    x = df[COLUMNAS]
    y = df["aprobada"].astype(int)
    x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=0.2, random_state=42, stratify=y)

    candidatos = [
        ("logistic_regression", pipeline(LogisticRegression(max_iter=500))),
        ("random_forest", pipeline(RandomForestClassifier(
            n_estimators=200, max_depth=8, random_state=42, n_jobs=-1))),
    ]
    try:
        from xgboost import XGBClassifier
        candidatos.append(("xgboost", pipeline(XGBClassifier(
            n_estimators=200, max_depth=4, learning_rate=0.08,
            eval_metric="logloss", random_state=42))))
    except Exception:
        print("XGBoost no está instalado; se comparan logística y random forest.")

    resultados = [evaluar(nombre, modelo, x_train, x_test, y_train, y_test) for nombre, modelo in candidatos]
    mejor, metricas = max(resultados, key=lambda par: par[1]["roc_auc"])
    joblib.dump(mejor, BASE / "modelo.pkl")
    meta = {
        "elegido": metricas,
        "comparacion": [m for _, m in resultados],
        "columnas": COLUMNAS,
        "umbral_alta": 0.66,
        "umbral_media": 0.40,
        "filas": int(len(df)),
        "origen": str(Path(args.csv).name),
    }
    (BASE / "metadata.json").write_text(json.dumps(meta, indent=2), encoding="utf-8")
    print(f"Modelo guardado: {metricas['modelo']} -> {BASE / 'modelo.pkl'}")


if __name__ == "__main__":
    main()
