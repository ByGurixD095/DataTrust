# DataTrust

**DataTrust** es una API backend para registrar conjuntos de datos (*datasets*) y describir su esquema, como base para una futura plataforma de evaluación de calidad del dato.

El proyecto se desarrolla incrementalmente. El catálogo de datasets permite alta, consulta individual y listado paginado. La API también incluye registro público como `DATA_STEWARD`, inicio de sesión y autenticación stateless mediante JWT.

## Estado del proyecto

| Área | Estado |
|---|---|
| Catálogo de datasets y columnas | Entrega 1: implementado |
| API REST versionada | Implementada para alta y consulta |
| Persistencia PostgreSQL y migraciones Flyway | Implementadas |
| Pruebas unitarias y de integración con Testcontainers | Añadidas; deben ejecutarse localmente para confirmar el resultado |
| Registro, inicio de sesión y autenticación JWT | Implementados |
| Reglas de calidad y motor de validación CSV | Pendientes |
| Informes, puntuación e historial de ejecuciones | Pendientes |
| Versionado inmutable del esquema de datasets | Pendiente de cerrar en el diseño antes del motor de validación |

## Tecnologías

- Java 17
- Spring Boot 4.1.1
- Spring Web MVC, Bean Validation y Spring Security
- JWT Bearer firmado con HS256
- Spring Data JPA / Hibernate
- PostgreSQL 16
- Flyway para versionar el esquema de la base de datos
- Maven Wrapper
- Docker y Docker Compose
- JUnit, MockMvc y Testcontainers para pruebas

## Arquitectura

El código sigue una separación por responsabilidades para evitar que las reglas de negocio dependan del framework o de la persistencia:

```text
com.datatrust.datatrust
├── api/                 # Controladores REST, DTO y gestión de errores HTTP
├── application/         # Casos de uso y puertos
├── domain/              # Modelo y reglas de negocio
├── infrastructure/      # Persistencia JPA y adaptadores
└── common/              # Componentes compartidos
```

El dominio no debe depender de Spring ni de JPA. La capa de aplicación coordina los casos de uso a través de puertos, mientras que la infraestructura implementa los adaptadores concretos.

## Requisitos previos

Para ejecutar la aplicación completa en local necesitas:

- Git.
- JDK 17.
- Docker Desktop con Docker Compose disponible.

Las pruebas de integración usan Testcontainers y necesitan acceso al Docker del equipo. No se ejecutan durante la construcción de la imagen Docker: se lanzan desde el entorno de desarrollo.

## Configuración local

La aplicación utiliza variables de entorno para conectarse a PostgreSQL. Crea un archivo `.env` en la raíz del proyecto, junto a `docker-compose.yml`, con valores locales propios:

```dotenv
POSTGRES_DB=datatrust
POSTGRES_USER=datatrust
POSTGRES_PASSWORD=cambia_esta_clave_local
JWT_SECRET=pega-aqui-la-clave-aleatoria-en-base64
```

Genera una clave JWT aleatoria de al menos 32 bytes y guarda su valor codificado en Base64 en `JWT_SECRET`. En PowerShell puedes generarla con:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Sustituye también la contraseña de PostgreSQL por una propia. Puedes partir de [`.env.example`](.env.example). **No subas `.env` al repositorio ni compartas ese archivo**. La aplicación falla al arrancar si `JWT_SECRET` no está configurada, no es Base64 o decodifica menos de 32 bytes.

## Ejecutar con Docker Compose

Desde la raíz del proyecto:

```bash
docker compose up --build -d
```

Comprueba el estado de los servicios:

```bash
docker compose ps
docker compose logs -f app
```

La API se publica en `http://localhost:8080`. PostgreSQL utiliza el puerto `5432` del equipo anfitrión según la configuración actual de Compose. Si no necesitas conectarte directamente a PostgreSQL desde el host, conviene limitar esa publicación a la interfaz local.

Para detener los contenedores sin borrar los datos:

```bash
docker compose down
```

Para detenerlos y **eliminar también el volumen de PostgreSQL** (se perderán los datos locales):

```bash
docker compose down -v
```

Las migraciones Flyway se aplican al iniciar la aplicación. Hibernate está configurado con `ddl-auto=validate`, por lo que valida el esquema, pero no lo crea ni lo modifica automáticamente.

## Ejecutar y verificar con Maven

En Windows PowerShell, desde la raíz del proyecto:

```powershell
.\mvnw.cmd verify
```

En Linux o macOS:

```bash
./mvnw verify
```

El comando `verify` ejecuta las pruebas configuradas y las comprobaciones del ciclo de vida de Maven. Las pruebas que utilizan Testcontainers requieren Docker en ejecución. Si el wrapper no tiene descargada la distribución de Maven, necesitará acceso a Internet para obtenerla.

Para iniciar la aplicación desde Maven, con una base PostgreSQL accesible y las variables de conexión y `JWT_SECRET` configuradas en el entorno:

```powershell
.\mvnw.cmd spring-boot:run
```

## API

Base URL: `http://localhost:8080/api/v1`

### Registro e inicio de sesión

`POST /auth/register` crea una cuenta pública con rol `DATA_STEWARD` y devuelve un JWT. El rol no se acepta desde la petición; las cuentas `ADMIN` deben provisionarse por un canal administrativo que no forma parte de esta API.

```json
{
  "username": "ana",
  "password": "una-clave-local-segura",
  "displayName": "Ana García",
  "email": "ana@example.com"
}
```

La contraseña debe tener entre 12 y 72 caracteres y no superar 72 bytes en UTF-8 (límite de BCrypt). Los nombres de usuario y email son únicos sin distinguir mayúsculas.

`POST /auth/login` recibe `username` y `password`. Ambos endpoints devuelven `accessToken`, `tokenType` (`Bearer`), `expiresAt` y `userId`. El JWT dura 15 minutos y debe enviarse en los endpoints protegidos:

```http
Authorization: Bearer <accessToken>
```

No hay refresh token ni endpoint de revocación; cerrar sesión en el cliente elimina el token, que de todos modos expira a los 15 minutos. Los endpoints de datasets requieren autenticación. El `ownerId` siempre se obtiene del sujeto autenticado del JWT, nunca del cuerpo de la petición.

### Crear un dataset

`POST /api/v1/datasets`

Ejemplo de cuerpo JSON:

```json
{
  "name": "clientes",
  "description": "Datos de clientes para pruebas",
  "format": "CSV",
  "major": 1,
  "minor": 0,
  "patch": 0,
  "columns": [
    {
      "name": "id",
      "position": 1,
      "expectedType": "INTEGER",
      "required": true
    },
    {
      "name": "email",
      "position": 2,
      "expectedType": "STRING",
      "required": true
    },
    {
      "name": "edad",
      "position": 3,
      "expectedType": "INTEGER",
      "required": false
    }
  ]
}
```

Respuesta esperada si la operación tiene éxito: `201 Created`, con una cabecera `Location` que identifica el recurso creado. Requiere el bearer token del usuario autenticado.

### Consultar un dataset

`GET /api/v1/datasets/{id}`

Sustituye `{id}` por el UUID del dataset creado.

### Listar datasets

`GET /api/v1/datasets?page=0&size=20`

- `page`: índice de página, empezando en `0`.
- `size`: tamaño de página entre `1` y `100`; por defecto, `20`.

### Errores

Los errores de la API utilizan el formato `application/problem+json`. Entre los estados contemplados están:

- `400 Bad Request`: petición inválida o datos que no cumplen las validaciones.
- `404 Not Found`: dataset inexistente.
- `409 Conflict`: nombre de dataset duplicado, sin distinguir mayúsculas y minúsculas.
- `500 Internal Server Error`: error inesperado; la respuesta incluye un identificador de correlación, no detalles internos de la excepción.

## Migraciones de base de datos

Las migraciones están en `src/main/resources/db/migration`. `V1__catalogo.sql` crea las tablas de datasets y versiones; `V2__users.sql` crea `users` y añade la clave foránea de `datasets.owner_id`. Las modificaciones futuras del esquema deben añadirse como nuevas migraciones versionadas; evita editar una migración ya aplicada en entornos compartidos.

## Seguridad y limitaciones actuales

Los tokens se firman y validan con HS256; el secreto Base64 se configura exclusivamente mediante `JWT_SECRET`. Las contraseñas se almacenan con BCrypt (coste 12), nunca en texto plano. Los tokens incluyen UUID de usuario, username y rol. Un usuario inactivo no puede iniciar sesión, aunque un JWT emitido antes de desactivarlo sigue vigente hasta expirar.

La autenticación identifica al usuario y protege las rutas, pero todavía no hay políticas de autorización por rol ni aislamiento de lectura por propietario para datasets. También quedan pendientes, entre otros puntos:

- reglas de calidad configurables y motor de validación de CSV;
- cálculo de puntuación, informes e historial de ejecuciones;
- documentación OpenAPI y comprobaciones de arquitectura automatizadas.

## Decisiones y seguimiento

Las decisiones técnicas y tareas pendientes de la primera entrega se documentan en [`docs/decisionesYtoDo.md`](docs/decisionesYtoDo.md). Ese documento complementa este README, pero no sustituye la descripción del estado real del código.

## Desarrollo

1. Crea una rama para cada cambio significativo.
2. Añade pruebas para nuevas reglas de dominio y casos de uso.
3. Añade una migración Flyway cuando cambie el esquema persistido.
4. Ejecuta `mvnw.cmd verify` antes de integrar cambios en Windows, o `./mvnw verify` en Linux/macOS.
5. No incluyas secretos, archivos `.env`, carpetas `target/` ni artefactos de compilación en los commits.
