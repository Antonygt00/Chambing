# Fases de selección y notificaciones

La implementación corresponde al backend Spring Boot y a las vistas web. Flutter no se modificó.

## Uso

1. El usuario se postula desde una oferta activa. Se registra la fase **Inscripción** y se abre **Mis postulaciones**, donde puede adjuntar su hoja de vida y documentación adicional.
2. La empresa abre **Mis Propuestas → Ver postulantes** (también desde **Editar Propuestas**). Puede buscar candidatos y filtrar por fase y resultado.
3. En cada candidato elige **Inscripción → Revisión documental → Potencial candidato → Seleccionado**. Puede saltar fases o retroceder si el proceso lo requiere. Debe explicar los retrocesos, rechazos y reaperturas.
4. Las observaciones aparecen en el historial del candidato. Las notas internas solo se entregan a la empresa y al administrador autenticado.
5. El usuario consulta su progreso, documentos e historial desde **Mis postulaciones**. Al recibir un aviso abre directamente la postulación correspondiente. El botón **Actualizar seguimiento** consulta los cambios más recientes.
6. La campana **Notificaciones** aparece en las páginas de usuario y empresa. Incluye contador, paginación y acciones para marcar una o todas como leídas.

El resultado está separado de la fase: `Pendiente` (En proceso), `Rechazado` y `Aprobado` (Seleccionado). Rechazar conserva la última fase alcanzada. Seleccionar establece la cuarta fase y `Aprobado` para mantener compatibilidad con el panel existente.

## Documentos

- PDF de hasta 5 MB; máximo 5 documentos por postulación.
- Se pueden adjuntar mientras el resultado sea En proceso; la empresa puede pedir archivos adicionales en la observación del cambio de fase.
- Almacenamiento privado en GridFS, dentro de la conexión MongoDB existente (`fs.files` y `fs.chunks`). No se utiliza un directorio público ni un servicio externo.
- Descarga como adjunto, con comprobación de propietario de la postulación o empresa de la oferta. La autorización no depende de conocer el ID del archivo.
- Se valida extensión, cabecera PDF y tamaño. Los límites multipart están en `DocumentoConfig` y se incluyen en el código versionado.

## Notificaciones

Se generan avisos de nueva inscripción para el usuario y la empresa, cambios de fase o resultado para el usuario, y documentos nuevos para la empresa.

Cada operación guarda los avisos pendientes junto con la postulación. Un proceso cada 15 segundos entrega esta outbox a la colección `notificaciones` mediante upsert por ID. Una interrupción se reintenta sin duplicar avisos ni restablecer su lectura. La web consulta la bandeja cada 15 segundos mientras está visible; en condiciones normales un aviso puede tardar aproximadamente 30 segundos en aparecer. No son correos ni notificaciones push del sistema operativo.

## Migración y acceso

Al arrancar el backend, `MigracionSeleccion` inicializa los registros antiguos de forma idempotente: aprobados a Seleccionado y los demás a Inscripción, conservando su resultado y sin inventar historial previo. Los nuevos cambios se registran a partir de entonces.

Los cambios y documentos usan bloqueo optimista: si otra operación modificó la postulación se responde 409 para recargar. Las inscripciones nuevas tienen una clave única para evitar duplicados concurrentes.

Las APIs de selección verifican la identidad autenticada y la empresa propietaria. Las APIs administrativas ahora verifican la misma sesión `adminLogged` que ya utilizaba el panel, evitando el acceso público a datos y notas internas.

## Ejecutar y comprobar

Se requieren Java 21 o posterior, Maven y las conexiones MongoDB/MySQL ya configuradas en el `application.properties` local. No se añadieron dependencias.

```powershell
mvn spring-boot:run
```

El servidor sigue en el puerto 9999. Reiniciarlo carga los nuevos recursos y ejecuta la migración. La cuenta MongoDB debe poder actualizar las postulaciones y crear/escribir `notificaciones` y GridFS.

Pruebas aisladas, sin conectarse a las bases reales:

```powershell
mvn "-Dtest=SeleccionServiceTest,AccesoSeleccionTest,NotificacionServiceTest,SeleccionHttpTest,NotificacionControllerTest" test
node --test src/test/js/seleccion.test.cjs
```

La prueba antigua `DemoApplicationTests` carga toda la aplicación y requiere las bases configuradas; por eso no está incluida en el comando aislado.

## API añadida

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/postulaciones/{id}` | Detalle autorizado; notas privadas omitidas para el usuario |
| PUT | `/empresa/{idEmpresa}/postulaciones/{id}/fase` | `{fase, resultado, observacion, notaInterna, version}` |
| POST | `/api/postulaciones/{id}/documentos` | Multipart, campo `archivo` |
| GET | `/api/postulaciones/{id}/documentos/{documentoId}` | Descargar PDF autorizado |
| GET | `/api/notificaciones?page=0&size=20` | Bandeja del destinatario autenticado |
| GET | `/api/notificaciones/pendientes` | Contador de no leídas |
| PUT | `/api/notificaciones/{id}/leida` | Marcar una propia como leída |
| PUT | `/api/notificaciones/leidas` | Marcar todas las propias como leídas |

`fase` admite `INSCRIPCION`, `REVISION_DOCUMENTAL`, `POTENCIAL_CANDIDATO` y `SELECCIONADO`. Los endpoints anteriores de cambio de estado de empresa y administrador usan el mismo servicio para conservar historial y notificaciones.
