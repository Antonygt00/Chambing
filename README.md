# Chambing

Backend de **Chambing** — plataforma que conecta empresas con talento.

API REST construida con **Spring Boot 3.5** + **Java 21**. Usa **MongoDB** para datos de negocio (empresas, usuarios, propuestas, postulaciones) y **MySQL** para catálogos (géneros, tipos de documento).

**Producción:** [https://chambing.com.co](https://chambing.com.co)

---

## Stack

| Tecnología | Uso |
|------------|-----|
| Spring Boot 3.5 | Framework principal |
| Spring Security + JWT | Autenticación API |
| OAuth2 (Google / GitHub) | Login social (web) |
| MongoDB | Empresas, usuarios, propuestas, postulaciones, mensajes, etc. |
| MySQL + JPA | Catálogos (`generos`, `tipos_documento`) |
| Thymeleaf | Vistas web del portal |
| Python (carpeta `prediccion/`) | Modelo de predicción de empleos |

---

## Estructura

```
Chambing/
├── chambing_debug/          # Proyecto Spring Boot (Maven)
│   ├── pom.xml
│   └── src/main/java/Chambing/demo/
│       ├── controller/      # Endpoints REST y vistas
│       ├── Model/           # Entidades Mongo y JPA
│       ├── repository/
│       ├── security/        # JWT, OAuth2, SecurityConfig
│       └── dto/
├── prediccion/              # Scripts Python de predicción
│   ├── app.py
│   ├── entrenar_modelo.py
│   └── modelo.pkl
├── DB.py
└── .gitignore
```

---

## Requisitos

- **Java 21**
- **Maven**
- **MongoDB** (local o Atlas)
- **MySQL** (para catálogos)

---

## Cómo correrlo en local

1. Clona el repo:
   ```bash
   git clone https://github.com/Antonygt00/Chambing.git
   cd Chambing/chambing_debug
   ```

2. Crea el archivo de configuración (no está en el repo por seguridad):

   Crea `src/main/resources/application.properties` con algo como:

   ```properties
   server.port=9999

   # MongoDB
   spring.data.mongodb.uri=mongodb://localhost:27017/chambing

   # MySQL (catálogos)
   spring.datasource.url=jdbc:mysql://localhost:3306/chambing_catalogos
   spring.datasource.username=root
   spring.datasource.password=tu_password
   spring.jpa.hibernate.ddl-auto=update

   # JWT (cambia el secreto)
   jwt.secret=tu_secreto_largo_y_seguro
   jwt.expiration=86400000
   ```

3. Levanta el servidor:
   ```bash
   mvn spring-boot:run
   ```

   La API queda en `http://localhost:9999`.

---

## Endpoints principales (API)

### Autenticación
| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/auth/login` | Login (email + password) → JWT |

Body:
```json
{
  "email": "usuario@mail.com",
  "password": "1234"
}
```

Respuesta:
```json
{
  "token": "eyJ...",
  "type": "Bearer"
}
```

### Catálogos (públicos)
| Método | Ruta |
|--------|------|
| `GET` | `/api/catalogos/generos` |
| `GET` | `/api/catalogos/tipos-documento` |

### Empresas
| Método | Ruta | Auth |
|--------|------|------|
| `POST` | `/empresa` | No |
| `GET` | `/empresa/me` | Sí |
| `GET` | `/empresa/{id}/propuestas` | Sí |
| `POST` | `/empresa/{id}/propuestas` | Sí |
| `DELETE` | `/empresa/{id}/propuestas/{idPropuesta}` | Sí |

### Usuarios
| Método | Ruta | Auth |
|--------|------|------|
| `POST` | `/usuario` | No |
| `GET` | `/usuario/me` | Sí |
| `GET` | `/usuario/propuestas` | Sí |
| `POST` | `/usuario/aplicar/{idUsuario}/{idPropuesta}` | Sí |
| `GET` | `/usuario/postulaciones/{idUsuario}` | Sí |

> Usa el header `Authorization: Bearer <token>` en las rutas protegidas.

---

## Predicción (Python)

La carpeta `prediccion/` contiene un modelo de predicción de empleos.

```bash
cd prediccion
pip install -r requirements.txt
python app.py
```

Más detalles en `prediccion/COMO_REENTRENAR.md`.

---

## Notas

- **`application.properties` y `credenciales.txt` no se suben** (están en `.gitignore`).
- El puerto por defecto en local es **9999**.
- En producción la API está en **https://chambing.com.co**.
- CORS en producción actualmente permite el origen `https://chambing.com.co`. Para desarrollo web desde `localhost` hay que agregar ese origen en `SecurityConfig`.

---

## Licencia

Proyecto privado / uso interno.
