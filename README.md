# DataTrust

**DataTrust** es una API backend para registrar conjuntos de datos (*datasets*) y describir su esquema, como base para una futura plataforma de evaluación de calidad del dato.

El proyecto se desarrolla incrementalmente. Esta primera entrega implementa el **catálogo de datasets**: alta, consulta individual y listado paginado. La validación de archivos CSV, las reglas de calidad, el cálculo de puntuaciones, el historial de ejecuciones y la autenticación real forman parte de entregas posteriores y **no deben considerarse implementados todavía**.

## Estado del proyecto

| Área | Estado |
|---|---|
| Catálogo de datasets y columnas | Entrega 1: implementado |
| API REST versionada | Implementada para alta y consulta |
| Persistencia PostgreSQL y migraciones Flyway | Implementadas |
| Pruebas unitarias y de integración con Testcontainers | Añadidas; deben ejecutarse localmente para confirmar el resultado |
| Autenticación y autorización reales | Pendientes |
| Reglas de calidad y motor de validación CSV | Pendientes |
| Informes, puntuación e historial de ejecuciones | Pendientes |
| Versionado inmutable del esquema de datasets | Pendiente de cerrar en el diseño antes del motor de validación |

## Tecnologías

- Java 17
- Spring Boot 4.1.1
- Spring Web MVC y Bean Validation
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
```

Sustituye la contraseña de ejemplo por una contraseña local adecuada. **No subas `.env` al repositorio ni compartas ese archivo**. En el repositorio debe mantenerse un `.env.example` sin credenciales reales para documentar las variables necesarias.

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

Para iniciar la aplicación desde Maven, con una base PostgreSQL accesible y las variables de conexión configuradas:

```powershell
.\mvnw.cmd spring-boot:run
```

## API disponible en la entrega 1

Base URL: `http://localhost:8080/api/v1`

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

Respuesta esperada si la operación tiene éxito: `201 Created`, con una cabecera `Location` que identifica el recurso creado. El propietario no se acepta desde el cuerpo de la petición: durante esta entrega lo proporciona un proveedor de usuario temporal.

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

Las migraciones están en `src/main/resources/db/migration`. La migración inicial `V1__catalogo.sql` crea las tablas `datasets` y `dataset_columns`, junto con sus restricciones e índices. Las modificaciones futuras del esquema deben añadirse como nuevas migraciones versionadas; evita editar una migración ya aplicada en entornos compartidos.

## Seguridad y limitaciones actuales

Esta entrega todavía no incluye autenticación real ni autorización por roles. El `FixedCurrentUserProvider` es temporal y devuelve un usuario fijo para desarrollo; no representa una identidad autenticada. No expongas esta versión como servicio público ni la uses con datos sensibles.

También quedan pendientes, entre otros puntos:

- usuarios, autenticación y roles `ADMIN` / `DATA_STEWARD`;
- restricciones de acceso por propietario y administración;
- un modelo de versiones de esquema inmutables;
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
