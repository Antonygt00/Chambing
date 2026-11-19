"""
Genera un dataset sintético de Chambing para entrenar el modelo de aprobación.

Mínimos pedidos: 1.000 usuarios, 150 propuestas y 3.000 postulaciones.
La etiqueta no es aleatoria: sigue una relación logística con ruido, para que
el modelo tenga señal (ciudad, experiencia, skills, historial, salario, competencia).

Uso:
    python generar_datos_simulados.py
Escribe prediccion/data/postulaciones_simuladas.csv
"""
from pathlib import Path

import numpy as np
import pandas as pd
from faker import Faker

CATEGORIAS = [
    "Tecnología y Desarrollo",
    "Marketing Digital",
    "Recursos Humanos",
    "Administración y Finanzas",
    "Operaciones",
    "Ventas",
    "Salud",
]
CIUDADES = ["Cartagena", "Barranquilla", "Bogotá", "Medellín", "Santa Marta"]
NIVELES = ["Junior", "Semi-senior", "Senior"]


def construir(n_usuarios=1000, n_propuestas=150, n_postulaciones=3000, semilla=42):
    faker = Faker("es_CO")
    Faker.seed(semilla)
    rng = np.random.default_rng(semilla)

    usuarios = []
    for i in range(n_usuarios):
        usuarios.append({
            "usuario_id": i,
            "edad": int(rng.integers(18, 62)),
            "ciudad": rng.choice(CIUDADES, p=[0.55, 0.15, 0.12, 0.1, 0.08]),
            "anios_experiencia": int(rng.integers(0, 21)),
            "nivel_educativo": int(rng.integers(1, 6)),
            "skills": faker.words(nb=int(rng.integers(2, 7))),
            "pct_aprobadas": float(np.clip(rng.normal(0.35, 0.2), 0, 1)),
            "num_postulaciones_previas": int(rng.integers(0, 25)),
        })

    propuestas = []
    for i in range(n_propuestas):
        categoria = str(rng.choice(CATEGORIAS))
        propuestas.append({
            "propuesta_id": i,
            "categoria": categoria,
            "ciudad": str(rng.choice(CIUDADES, p=[0.6, 0.15, 0.1, 0.1, 0.05])),
            "salario": int(rng.integers(1_300_000, 8_000_000)),
            "cantidad_requisitos": int(rng.integers(1, 9)),
            "num_postulantes": int(rng.integers(1, 80)),
            "nivel": str(rng.choice(NIVELES)),
            "skills": faker.words(nb=int(rng.integers(2, 6))),
            "empresa": faker.company(),
            "titulo": f"{faker.job()} {categoria.split()[0]}",
        })

    filas = []
    for _ in range(n_postulaciones):
        u = usuarios[int(rng.integers(0, n_usuarios))]
        p = propuestas[int(rng.integers(0, n_propuestas))]
        coinciden = len(set(map(str.lower, u["skills"])) & set(map(str.lower, p["skills"])))
        misma = int(u["ciudad"] == p["ciudad"])
        fila = {
            "usuario_id": u["usuario_id"],
            "propuesta_id": p["propuesta_id"],
            "edad": u["edad"],
            "misma_ciudad": misma,
            "salario": p["salario"],
            "cantidad_requisitos": p["cantidad_requisitos"],
            "num_postulantes": p["num_postulantes"],
            "num_postulaciones_previas": u["num_postulaciones_previas"],
            "pct_aprobadas": round(u["pct_aprobadas"], 3),
            "anios_experiencia": u["anios_experiencia"],
            "nivel_educativo": u["nivel_educativo"],
            "skills_coincidentes": coinciden,
            "categoria": p["categoria"],
        }
        fila["aprobada"] = _etiqueta(fila, rng)
        filas.append(fila)

    return pd.DataFrame(filas), usuarios, propuestas


def _etiqueta(fila, rng):
    # Logit con señal conocida. El ruido deja casos difíciles a propósito.
    logit = (
        -0.15
        + 0.85 * fila["misma_ciudad"]
        + 0.08 * min(fila["anios_experiencia"], 12)
        + 0.18 * fila["nivel_educativo"]
        + 0.35 * fila["skills_coincidentes"]
        + 1.3 * fila["pct_aprobadas"]
        - 0.00000035 * fila["salario"]
        - 0.08 * fila["cantidad_requisitos"]
        - 0.012 * fila["num_postulantes"]
        - 0.02 * fila["num_postulaciones_previas"]
        + (0.15 if 23 <= fila["edad"] <= 40 else -0.25)
        + float(rng.normal(0, 0.55))
    )
    probabilidad = 1 / (1 + np.exp(-logit))
    return int(rng.random() < probabilidad)


def main():
    df, usuarios, propuestas = construir()
    out = Path(__file__).resolve().parent / "data"
    out.mkdir(exist_ok=True)
    destino = out / "postulaciones_simuladas.csv"
    df.to_csv(destino, index=False)
    print(f"Usuarios: {len(usuarios)} | Propuestas: {len(propuestas)} | Postulaciones: {len(df)}")
    print(f"Tasa de aprobación: {df['aprobada'].mean():.3f}")
    print(f"CSV: {destino}")


if __name__ == "__main__":
    main()
