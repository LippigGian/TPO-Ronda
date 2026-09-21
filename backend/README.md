# Backend de Ronda

Base compartida de la API REST. Spring Boot 4.0.8, Java 21, Maven y PostgreSQL 17.
Android sigue en `app/` y conserva su compilacion Gradle independiente.

## Alcance de esta rama

- Aplicacion ejecutable, PostgreSQL persistente con Docker Compose y configuracion por variables.
- Spring Web, Data JPA, Validation, Security y Actuator.
- Flyway aplica las migraciones al arrancar; Hibernate valida el esquema y no modifica tablas.
- `GET /api/v1/status`: comprueba que la API responde.
- `GET /actuator/health`: comprueba tambien PostgreSQL; devuelve 503 si no esta saludable.
- El resto de las rutas devuelve 403 hasta que cada feature defina sus permisos.

Registro y login por contraseña entregan sesiones JWT. El registro deja la cuenta sin verificar y envia un OTP.
`POST /api/v1/auth/otp/request` solicita un código para `LOGIN`, `REGISTRO` o `RECUPERO_CONTRASENA`.
`POST /api/v1/auth/otp/resend` genera y envía un código nuevo e invalida el anterior pendiente.
`POST /api/v1/auth/otp/verify` valida el código y crea la sesión JWT. Los códigos vencen a los 10 minutos,
se almacenan como hash y admiten hasta cinco intentos fallidos.
Docker Compose incluye Mailpit para desarrollo: los emails se pueden ver en `http://localhost:8025`.

### Usuario de prueba local

Flyway crea una única cuenta de desarrollo para facilitar las pruebas del equipo. Está disponible
también en bases existentes después de reiniciar el backend para aplicar la migración:

```text
Email: demo@ronda.com
Contraseña: ronda123
```

Es un usuario solo para desarrollo; no usar esas credenciales en un entorno público.

## Publicaciones

Las rutas de publicaciones requieren un JWT válido, excepto la lectura de archivos bajo `/uploads/**`.

| Método | Ruta | Uso |
| --- | --- | --- |
| `POST` | `/api/v1/publicaciones` | Crea una publicación para el usuario autenticado. |
| `GET` | `/api/v1/publicaciones/mias` | Lista las publicaciones propias, incluidas pausadas o vendidas. |
| `PATCH` | `/api/v1/publicaciones/{id}/estado` | Cambia el estado a `ACTIVA`, `PAUSADA` o `VENDIDA`. |
| `POST` | `/api/v1/publicaciones/{id}/fotos` | Carga de 1 a 5 fotos con `multipart/form-data`, usando la clave `fotos`. |
| `GET` | `/api/v1/publicaciones` | Home: listado paginado de publicaciones activas con búsqueda, filtros y orden. |
| `GET` | `/api/v1/publicaciones/{id}` | Detalle: fotos, descripción, vendedor con reputación y permiso sobre la dirección. |
| `GET` | `/api/v1/publicaciones/categorias` | Categorías que hoy tienen publicaciones activas (para el filtro). |

Al crear una publicación se envían `titulo`, `descripcion`, `categoria`, `precio`,
`estadoArticulo` (`NUEVO`, `COMO_NUEVO` o `USADO`), `direccion`, `latitud` y `longitud`.
Las fotos se conservan en el volumen Docker `uploads_data`, por lo que no se pierden al reiniciar
los contenedores.

### Explorar publicaciones (Home)

`GET /api/v1/publicaciones` acepta estos parámetros, todos opcionales y combinables:

| Parámetro | Uso |
| --- | --- |
| `q` | Texto libre sobre título y descripción. |
| `categoria` | Categoría exacta (sin distinguir mayúsculas). |
| `precioMin`, `precioMax` | Rango de precio. |
| `estadoArticulo` | `NUEVO`, `COMO_NUEVO` o `USADO`. |
| `lat`, `lng`, `radioKm` | Cercanía: solo publicaciones dentro del radio. Con `lat` y `lng` se informa `distanciaKm` aproximada. |
| `orden` | `RECIENTES` (por defecto), `PRECIO_ASC` o `PRECIO_DESC`. |
| `page`, `size` | Paginación desde 0. `size` por defecto 10 y máximo 50. |

La respuesta es `{content, page, size, totalElements, totalPages, last}`. Cada item incluye la `zona` del vendedor
pero **nunca** la dirección exacta ni las coordenadas: eso solo se ve en el detalle, con el permiso correspondiente.

Los datos de `V11__seed_publicaciones_demo.sql` crean el vendedor `vendedor@ronda.com` (misma contraseña que el demo)
con publicaciones de prueba. Las migraciones de esta feature usan `V10` y `V11` para no chocar con las de otras ramas.

### Detalle de la publicación

`GET /api/v1/publicaciones/{id}` devuelve el detalle completo, los datos del vendedor (`nombre`, `miembroDesde`,
`reputacion`) y dos banderas según quién mire: `esPropia` y `direccionVisible`.
La `direccion`, `latitud` y `longitud` exactas **solo se envían cuando `direccionVisible` es true**; si no, van en `null`.
Hoy solo las ve el dueño. Cuando exista el módulo de ofertas (punto 7), `PublicacionService.detalle` debe marcarla
visible también para quien tenga una oferta aceptada (hay un `TODO` en ese método). La reputación llega en cero hasta
que existan las calificaciones (puntos 2 y 9). Las publicaciones pausadas o vendidas devuelven 404 a terceros.

## Ofertas y negociación

Una oferta es una negociación entre un comprador y el vendedor de una publicación. El campo `precio`
es siempre la última propuesta y `turno` indica quién tiene que responder: quien tiene el turno puede
aceptar, rechazar o contraofertar. Cada contraoferta le pasa el turno a la otra parte y renueva el plazo.

| Método | Ruta | Uso |
| --- | --- | --- |
| `POST` | `/api/v1/publicaciones/{id}/ofertas` | Oferta inicial del comprador: `precio` (distinto al publicado) y `mensaje` opcional. |
| `GET` | `/api/v1/ofertas/mias` | "Mis ofertas": `enviadas`, `recibidas` y `pendientesDeMiRespuesta`. |
| `GET` | `/api/v1/ofertas/{id}` | Una oferta, solo para sus participantes. |
| `POST` | `/api/v1/ofertas/{id}/aceptar` | Acepta. Rechaza automáticamente las demás pendientes de la publicación. |
| `POST` | `/api/v1/ofertas/{id}/rechazar` | Rechaza. |
| `POST` | `/api/v1/ofertas/{id}/contraofertar` | Nueva propuesta: `precio` y `mensaje` opcional. |

- Estados: `PENDIENTE`, `ACEPTADA`, `RECHAZADA` y `VENCIDA`. Solo `PENDIENTE` admite respuestas.
- Vencimiento: pasado `vence_at`, la oferta caduca sola. Un job revisa cada minuto y además cada
  endpoint de ofertas vence las expiradas antes de responder. El plazo se configura con
  `OFERTAS_VIGENCIA_MINUTOS` en `.env` (por defecto 2880 = 48 h; para probar el vencimiento usar `2`).
- Con la oferta `ACEPTADA` la respuesta incluye `entrega` (dirección y coordenadas) y el detalle de la
  publicación pasa a mostrar la dirección exacta al comprador.
- Los errores de estos endpoints devuelven el motivo en el campo `detail`
  (por ejemplo, "Tenés que esperar la respuesta de la otra parte").

## Opcion A: ejecutar todo con Docker Desktop

Instalar Docker Desktop y usar contenedores Linux. Desde la raiz del repositorio:

```powershell
cd backend
Copy-Item .env.example .env
notepad .env
```

Completar `DB_PASSWORD` con una contrasena local de letras y numeros, guardar y ejecutar:

```powershell
docker compose up --build -d
docker compose logs -f backend
```

Cuando se solicite un OTP, abrir `http://localhost:8025` para leer el email de prueba en Mailpit.

Compose lee `.env` automaticamente. La base queda en un volumen persistente y el backend
espera a que PostgreSQL este disponible. No hace falta instalar Java o Maven con esta opcion.

Comprobar desde otra terminal:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/status
Invoke-RestMethod http://localhost:8080/actuator/health
```

Respuestas esperadas:

```json
{"service":"ronda-backend","status":"ok"}
```

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

Detener conservando los datos: `docker compose down`. No agregar `-v` si se quieren conservar.
La contrasena de PostgreSQL se establece al crear el volumen por primera vez; editar `.env`
despues no modifica la contrasena de una base existente.

## Opcion B: Spring en el IDE y PostgreSQL en Docker

Requisitos: JDK 21, Maven 3.9 y Docker Desktop.
En este equipo, Java 24 presento un fallo de sockets Unix al iniciar Tomcat en Windows.
Para la ejecucion local se recomienda configurar JDK 21; Docker y CI ya utilizan Java 21.

Abrir `backend/pom.xml` como proyecto Maven. Configurar el directorio de trabajo en `backend`.
Preparar `.env` como en la opcion A y ejecutar desde `backend`:

```powershell
docker compose up -d db
mvn spring-boot:run
```

Spring importa `.env` como Java Properties desde el directorio de trabajo. Las variables del
entorno tienen prioridad. No ejecutar simultaneamente otro backend en el mismo puerto.
Si se habia iniciado la opcion A, detenerlo con `docker compose stop backend`.

## Opcion C: PostgreSQL instalado sin Docker

Crear una base `ronda` y un usuario `ronda` con permiso para crear esquemas en esa base.
Configurar `.env` con los mismos datos y ejecutar `mvn spring-boot:run` desde `backend`.
Flyway creara el esquema `ronda` automaticamente.

| Variable | Valor predeterminado / uso |
| --- | --- |
| `DB_NAME` | `ronda` |
| `DB_USER` | `ronda` |
| `DB_PASSWORD` | Obligatoria; no se versiona |
| `DB_PORT` | `5432`, puerto local de PostgreSQL |
| `DB_URL` | Opcional; reemplaza la URL JDBC completa para Spring |
| `SERVER_PORT` | `8080`, puerto HTTP |
| `JWT_EXPIRATION_SECONDS` | `3600`, duración de la sesión JWT en segundos |

Ejemplo de `DB_URL`: `jdbc:postgresql://localhost:5432/ronda`.
En Compose, el backend usa el nombre de servicio `db` y el puerto interno 5432;
`DB_PORT` cambia solamente el puerto publicado en la computadora.

## Pruebas

Desde `backend`:

```powershell
mvn verify
```

Compila, ejecuta las pruebas HTTP/seguridad sin base y genera `target/ronda-backend-0.0.1-SNAPSHOT.jar`.
Para verificar tambien arranque, migraciones, PostgreSQL real y health:

```powershell
docker compose up -d db
mvn -Pintegration verify
```

Las pruebas de integracion usan la conexion configurada y aplican migraciones: usar una base
local o dedicada a pruebas, nunca una base de produccion. No usan H2 ni omiten fallas de conexion.
El workflow `.github/workflows/backend.yml` ejecuta ambas clases de pruebas con PostgreSQL 17
en una base descartable al abrir un PR que modifica el backend y al integrar cambios en main.

## Conexion desde Android

- Emulador Android estandar: `http://10.0.2.2:8080/`.
- Telefono fisico: `http://IP_LOCAL_DE_LA_PC:8080/`, en la misma red y con el puerto permitido
  en el firewall solo para la red de desarrollo.
- En el telefono, `localhost` es el propio telefono, no la computadora.
- Android necesita permiso `INTERNET` y una configuracion de seguridad de red que permita
  HTTP exclusivamente para desarrollo. Las versiones publicadas deben usar HTTPS.

Esta rama no cambia la configuracion ni las pantallas de Android.

## Trabajo en equipo

1. Integrar `codex/backend-base` a `main` mediante PR.
2. Cada integrante incorpora `origin/main` a su rama de trabajo.
3. Cada feature agrega sus controladores, servicios, repositorios, entidades y migraciones
   en un paquete propio bajo `com.ronda.backend`, por ejemplo `auth` o `publicaciones`.
4. Agregar endpoints bajo `/api/v1` y definir expresamente sus permisos en `SecurityConfig`.
5. Crear migraciones `V2__descripcion.sql`, `V3__descripcion.sql`, etc. Coordinar la numeracion
   antes de integrar y no editar una migracion que ya se aplico en un entorno compartido.
6. Probar cada rama con su backend y base local. La instancia compartida ejecuta exclusivamente
   el codigo desplegado; cambiar de rama en Android no cambia ese servidor.

Si Hibernate valida entidades nuevas contra un esquema incompleto, agregar la migracion;
no reemplazar `ddl-auto: validate` por `update` para ocultar el problema.

## Despliegue posterior

El Dockerfile permite ejecutar el backend con Java 21 sin instalar Maven en el servidor.
Se necesitan una base persistente, variables de entorno y HTTPS en el servicio de hosting.
El Compose incluido es para desarrollo local. `.env` queda excluido de Git y del contexto Docker.
Para producción, reemplazar Mailpit por un proveedor SMTP real mediante `MAIL_HOST`, `MAIL_PORT`,
`MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` y las credenciales correspondientes.
