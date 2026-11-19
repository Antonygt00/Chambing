# Departamentos y ciudades

El catálogo de ubicaciones se almacena en MongoDB, en la misma base configurada para Chambing:

- `departamentos`: `id`, `nombre`, `clave` normalizada, `activo`.
- `ciudades`: `id`, `nombre`, `clave`, `departamentoId`, `activo`.
- `usuarios.direccion` y `empresas.direccion`: conservan `calle`, `departamento` y `ciudad`, y ahora incluyen `departamentoId` y `ciudadId`.

## Activación

Recompila y reinicia Spring Boot. `MigracionUbicaciones` crea las colecciones e índices, importa una sola vez la lista que antes estaba escrita en los perfiles, e incorpora las ubicaciones presentes en las direcciones existentes. No es un catálogo oficial completo de todos los municipios: el administrador puede ampliarlo.

La carga se registra con el ID `ubicaciones_v1` en `migraciones_catalogos`. Reiniciar no repone ubicaciones eliminadas ni revierte ediciones. Si se interrumpe la primera ejecución, los upserts permiten reintentar sin duplicar los registros ya creados. No elimines ese marcador después de administrar el catálogo.

Las pruebas utilizan un MongoDB local independiente y no ejecutan la migración sobre la base configurada de la aplicación.

## Administración

Inicia sesión en `/admin` y entra a `/admin/panel`. En el menú están **Departamentos** y **Ciudades**, con búsqueda, paginación y creación, edición y eliminación. En el formulario de ciudad eliges el departamento al que pertenece. También puedes activar o desactivar cada registro.

No se permite eliminar un departamento con ciudades o direcciones asociadas ni una ciudad que esté en uso. Desactivar conserva las direcciones guardadas y retira las opciones de los nuevos registros. Las direcciones que ya usaban una ubicación desactivada pueden conservarla al editar otros datos del perfil.

Renombrar un departamento actualiza el nombre en las direcciones vinculadas. Renombrar o cambiar de departamento una ciudad actualiza también los datos de esa ciudad en usuarios y empresas.

## Formularios y compatibilidad

Registro de usuario y empresa, ambos perfiles y la edición de direcciones en el panel usan selectores dependientes: primero el departamento y después sus ciudades. No mantienen listas geográficas en el HTML. Se muestran los fallos de carga y se puede reintentar.

El backend verifica que la ciudad pertenezca al departamento y que la ubicación esté activa para nuevas direcciones. Los clientes antiguos pueden seguir enviando nombres válidos: el servidor los resuelve a IDs, ignorando mayúsculas y acentos. Flutter no se modificó.

## Rutas

| Método | Ruta | Acceso |
|---|---|---|
| GET | `/api/catalogos/departamentos` | Departamentos activos, público |
| GET | `/api/catalogos/ciudades?departamentoId=...` | Ciudades activas de un departamento activo, público |
| GET | `/admin/departamentos?page=0&size=10&q=...` | Listado administrativo |
| POST | `/admin/departamentos` | Crear: `{nombre, activo}` |
| PUT / DELETE | `/admin/departamentos/{id}` | Editar / eliminar |
| GET | `/admin/ciudades?page=0&size=10&q=...&departamentoId=...` | Listado administrativo; filtro por departamento opcional |
| POST | `/admin/ciudades` | Crear: `{nombre, departamentoId, activo}` |
| PUT / DELETE | `/admin/ciudades/{id}` | Editar / eliminar |
| GET | `/admin/departamentos/opciones` | Opciones para formularios administrativos |
| GET | `/admin/ciudades/opciones?departamentoId=...` | Opciones del departamento, incluyendo inactivas |

Todas las rutas `/admin/...` requieren la sesión administrativa existente.

## Pruebas

```powershell
node --test src/test/js/seleccion.test.cjs src/test/js/ubicaciones.test.cjs
```

La integración con MongoDB está aislada en `UbicacionesMongoTest` y solo se habilita con `-Dubicaciones.mongo.port=PUERTO`. Utiliza exclusivamente `127.0.0.1` y bases `chambing_ubicaciones_test_*`, que elimina al terminar. Nunca consulta `application.properties`.
