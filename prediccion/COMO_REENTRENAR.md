# Reentrenar el modelo de aprobación

El servicio vive en `ChambingV4/prediccion` y Spring lo llama en `PREDICCION_URL` (por defecto `http://localhost:8001`).

## Datos simulados (desarrollo)

```bash
cd ChambingV4/prediccion
pip install -r requirements.txt
python generar_datos_simulados.py
python entrenar_modelo.py
uvicorn app:app --port 8001
```

`entrenar_modelo.py` compara regresión logística y random forest (y XGBoost si el paquete está instalado). Guarda el de mayor ROC-AUC en `modelo.pkl` y el detalle en `metadata.json`.

## Datos reales

Exporta una fila por postulación con estas columnas:

`edad`, `misma_ciudad` (0/1), `salario`, `cantidad_requisitos`, `num_postulantes`, `num_postulaciones_previas`, `pct_aprobadas` (0 a 1), `anios_experiencia`, `nivel_educativo` (1 a 5), `skills_coincidentes`, `categoria`, `aprobada` (1 si la empresa la marcó Aprobado, 0 si no).

Guarda el archivo como `prediccion/data/postulaciones_reales.csv` y ejecuta:

```bash
python entrenar_modelo.py --csv data/postulaciones_reales.csv
```

Reinicia uvicorn para que cargue el nuevo `modelo.pkl`. No hace falta recompilar Spring: el contrato del JSON no cambia.

Si el microservicio está apagado, el panel sigue mostrando una etiqueta calculada en Java y marcada como `heuristica`.
