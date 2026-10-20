# Decisiones de diseño: DataTrust

Oct 20, 2026 · @Gonzalo

Registro de decisiones tomadas durante la entrega 1 (catálogo de datasets).

## Stack y entorno

| Tema | Decisión | Motivo |
| --- | --- | --- |
| Lenguaje | Java 17 (`java.version` 17 en `pom.xml`) | Versión del proyecto. El Dockerfile usa imágenes 17 para coincidir con el entorno local. |
| Framework | Spring Boot 4.1.1 | Versión generada por el inicializador. Algunos paquetes y starters son propios de Spring Boot 4. |
| Base de datos | PostgreSQL 16 (`postgres:16-alpine`) | Mismo motor en Compose, Testcontainers y producción. H2 descartado: no coincide en restricciones e índices. |
| Migraciones | Flyway, con la V1 como única migración de la entrega | El esquema se versiona en SQL, no con `ddl-auto`. |
| Validación de esquema | `spring.jpa.hibernate.ddl-auto=validate` | Hibernate comprueba que las entidades coinciden con las tablas al arrancar. |
| Contenedores | Dockerfile multietapa: Maven compila, JRE ejecuta, usuario sin privilegios | Imagen de ejecución pequeña y sin herramientas de compilación. |
| Tests de integración | Testcontainers con PostgreSQL real | Las restricciones y los índices se comprueban contra el motor real. |
| Tests fuera de Docker | Se ejecutan con `mvn verify` desde la máquina de desarrollo | Testcontainers necesita el Docker del host, que no existe durante el `docker build`. |

## Modelo de datos

**Identificadores UUID generados en el dominio.** `Dataset.create` genera el id con `UUID.randomUUID()`. La entidad no usa `@GeneratedValue` y el alta no depende de la base de datos. El `DEFAULT gen_random_uuid()` de la V1 queda como red de seguridad.

**Nombres únicos sin distinguir mayúsculas.** El nombre de un dataset es único en toda la base (índice `ux_datasets_name_lower` sobre `lower(name)`). Los nombres de columna son únicos dentro de cada dataset (`ux_dataset_columns_name` sobre `(dataset_id, lower(name))`).

**Posición única por dataset.** `UNIQUE (dataset_id, position)` en lugar de un trigger. Una restricción es atómica; un trigger con `EXISTS` tiene una condición de carrera.

**Versión codificada en un entero.** Se guarda `major * 1000000 + minor * 1000 + patch` en un `INTEGER`. El dominio valida `minor` y `patch` entre 0 y 999, y `major` entre 0 y 2146, porque `2147 * 1000000` no cabe en un `INTEGER` de PostgreSQL. La API devuelve el texto `"major.minor.patch"`, nunca el entero.

**Formato de dataset: solo `CSV`.** El enum `DatasetFormat` contiene solo `CSV`, y el `CHECK` de la V1 lo refuerza. Un `CHECK` y no una tabla de búsqueda, porque hay un único formato. Si aparece otro, se añade en una migración.

**Tipos de columna con `CHECK`.** `expected_type` es `VARCHAR` con `CHECK` sobre `STRING`, `INTEGER`, `FLOAT`, `BOOLEAN` y `DATE`. Flyway migra sin problemas y añadir un valor es un `ALTER` simple.

**Pendiente: `FLOAT` frente a `DECIMAL`.** Para valores de calidad conviene `DECIMAL`, porque `FLOAT` introduce errores de redondeo en comparaciones de rango. Se mantiene `FLOAT` en la V1 y se decide en la entrega 2.

**Marcas de tiempo con zona horaria.** `created_at` es `TIMESTAMPTZ`, y se mapea a `Instant` en Java.

**Bloqueo optimista con `lock_version`.** Se llama `lock_version` para no confundirla con `version`, que es la versión del dataset. Se mapea con `@Version`, y la entidad nueva la deja en `null`.

**Borrado en cascada solo donde corresponde.** Borrar un dataset borra sus columnas (`ON DELETE CASCADE`). La relación con usuarios y con formatos usa `RESTRICT`, para no borrar catálogo por accidente.

## Arquitectura

**Dominio sin anotaciones de persistencia.** `Dataset` y `DatasetColumn` están en `domain/catalog` sin JPA. `DatasetEntity` y `DatasetColumnEntity` están en `infrastructure/persistence/catalog`, y un adaptador traduce entre ambos. Es más código, pero el dominio no depende de Hibernate, como pidió el cliente.

**Fábricas `create` y `restore`.** `create` valida las invariantes y genera el id. `restore` reconstruye desde la base de datos sin validar, porque los datos ya fueron validados al guardarse y las reglas pueden cambiar. El constructor es privado: no hay forma de saltarse las validaciones.

**Puerto de persistencia en `application/ports`.** `DatasetRepository` define el contrato con objetos de dominio. La capa de aplicación no conoce JPA.

**Un único servicio de aplicación.** `DatasetService` agrupa `register`, `get` y `list`. Tres servicios con una operación cada uno no aportaban nada a este tamaño.

**El servicio recibe valores simples, no DTOs.** No depende de la API: el controlador convierte la petición en argumentos.

**Transacciones en el servicio.** `@Transactional` está en `DatasetService`, porque ahí se decide qué forma una unidad de trabajo. Las lecturas son `readOnly = true`. `open-in-view` está desactivado, así que el adaptador convierte las colecciones a dominio antes de salir de la transacción.

**Duplicados detectados por la base de datos.** El servicio no hace un `SELECT` previo: entre la comprobación y el `INSERT`, otra petición podría crear el mismo nombre. El adaptador captura la violación de `ux_datasets_name_lower` y la traduce a `DuplicateDatasetNameException`.

**Errores de dominio con código estable.** `DatasetValidationException` lleva un `DatasetErrorCode` (`NAME_BLANK`, `VERSION_INVALID`, `COLUMN_NAME_DUPLICATED`…). El cliente trata el error por el código, no por el texto.

**Excepciones en la capa que las detecta.** Las reglas de negocio están en `domain`. Un recurso inexistente está en `application`. Solo `api` conoce HTTP.

## API REST

**Ruta base `/api/v1/datasets`.** Versionada desde el principio.

**Creación con `201` y cabecera `Location`.** `POST /api/v1/datasets` devuelve `201 Created` con `Location` apuntando al recurso.

**El propietario sale del usuario autenticado, nunca del cuerpo.** Si el cliente eligiera el `ownerId`, podría crear datasets a nombre de otro. Se obtiene de `CurrentUserProvider`.

**Listado paginado con límite.** `GET /api/v1/datasets` acepta `page` (mínimo 0) y `size` (entre 1 y 100, por defecto 20). Un `size` mayor devuelve `400`.

**Validación con Bean Validation.** `CreateDatasetRequest` y `ColumnRequest` usan `@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max` y `@NotEmpty`. Los campos numéricos son `Integer`, no `int`, para que un campo ausente dé `400` y no `0`.

**Sin `@Validated` en el controlador.** Con esa anotación, Spring lanza `ConstraintViolationException` desde un proxy, que no cubre `ResponseEntityExceptionHandler` y acaba en `500`. Sin ella, Spring MVC valida los parámetros y la clase base responde `400`.

**Todos los errores en `ProblemDetail`.** `GlobalExceptionHandler` extiende `ResponseEntityExceptionHandler` y responde siempre con `application/problem+json`.

| Caso | Estado | Propiedad adicional |
| --- | --- | --- |
| `DatasetValidationException` | 400 | `code`, `field` |
| Bean Validation y validación de parámetros | 400 | `errors` |
| JSON mal formado, UUID inválido | 400 | — |
| `DatasetNotFoundException` | 404 | `datasetId` |
| `DuplicateDatasetNameException` | 409 | `conflictingName` |
| Cualquier otro error | 500 | `correlationId` |

**El 500 no revela detalles internos.** La excepción se registra en el log con el `correlationId`, y el cliente solo recibe ese identificador.

**Sin actualización ni borrado en la entrega 1.** RF-01 y RF-02 solo piden crear y consultar.

## Seguridad (temporal en la entrega 1)

**Sin autenticación en la entrega 1.** `spring-boot-starter-security` se retiró del `pom.xml` para que la API sea accesible durante el desarrollo. Lo acordó el cliente y debe revertirse en la entrega 3.

**`FixedCurrentUserProvider` como solución temporal.** Devuelve siempre el mismo usuario de desarrollo, hasta que haya autenticación real.

- [ ] **Pendiente (V2):** tabla `users` y clave foránea de `datasets.owner_id`. En la V1 `owner_id` es `NOT NULL` sin clave foránea, porque la tabla no existe todavía.
- [ ] **Pendiente (entrega 3):** permisos de modificación. Solo el propietario o un `ADMIN` puede modificar un dataset. `ADMIN` y `DATA_STEWARD` pueden leer el catálogo.

## Pruebas

**Unitarias del dominio sin Spring.** `DatasetTest` y `DatasetColumnTest` comprueban cada invariante y su código de error. No necesitan Docker.

**Persistencia con PostgreSQL real.** `DatasetPersistenceAdapterTest` usa `@DataJpaTest` con Testcontainers: duplicados sin distinguir mayúsculas, orden de columnas y `Optional` vacío.

**API con `MockMvc`.** `DatasetApiTest` cubre creación con `201` y `Location`, consulta, duplicados con `409`, validación con `400`, `404` y paginación. El contenedor PostgreSQL se comparte entre clases.

**Migración sobre base vacía.** `FlywayMigrationTest` usa un contenedor nuevo para comprobar que la V1 se aplica desde cero.

**Persistencia tras reinicio.** `DatasetPersistenceAcrossRestartTest` cierra un contexto de Spring y abre otro sobre la misma base. Cubre RNF-04 a pequeña escala.

**Reinicio de Compose.** El criterio de aceptación pide reiniciar los contenedores sin borrar el volumen. Se comprobó con `docker compose down` y `docker compose up -d`, sin `-v`.

Si el puerto de Ryuk está bloqueado, `TESTCONTAINERS_RYUK_DISABLED=true` lo desactiva durante la sesión.

## Entrega y ejecución

**Compose con PostgreSQL y aplicación.** `docker-compose.yml` levanta ambos servicios. La base tiene `healthcheck` con `pg_isready`, y la aplicación espera a que esté sana.

**Credenciales en `.env`.** No van en el repositorio. `.env` está en `.gitignore` y en `.dockerignore`.

**Ejemplos en `docs/ejemplos/`.** El JSON de creación se guarda como ejemplo. En Windows, `curl.exe` lo envía con `--data-binary "@fichero.json"`, porque el escapado de comillas en PowerShell no es fiable.

## Pendientes para la entrega 2

- [ ] Migración V2 con la tabla `users` y la clave foránea de `datasets.owner_id`.
- [ ] Sustituir `FixedCurrentUserProvider` por autenticación real.
- [ ] Decidir `FLOAT` frente a `DECIMAL` para `expected_type`.
- [ ] Lectura de CSV en streaming con Commons CSV, sin escribir el fichero en disco.
- [ ] Contrato de la API de validación: `POST /api/v1/datasets/{id}/validations`, multipart, con lectura en streaming.
- [ ] Límites de tamaño configurables con `spring.servlet.multipart.max-file-size`.
- [ ] Test de arquitectura con ArchUnit, aplazado en la entrega 1.

## Qué cambió respecto al plan inicial

| Plan inicial | Lo que se hizo en la entrega 1 | Motivo |
| --- | --- | --- |
| Java 21 | Java 17 | El entorno local tiene JDK 17. |
| Tres servicios de aplicación | `DatasetService` único | Una operación por servicio no aportaba nada. |
| Versión de dataset como texto libre | Entero codificado, expuesto como texto | Pedido de Gonzalo. Pendiente de confirmar con el cliente. |
| Seguridad desde el principio | Sin autenticación hasta la entrega 3 | Acordado con el cliente para cerrar el catálogo antes. |
| Validación con `@Validated` en el controlador | Validación de Spring MVC | `@Validated` lanza una excepción que el manejador no cubre. |
| Subida de CSV en el controlador del catálogo | Eliminada del catálogo | Pertenece a la validación de la entrega 2. |
