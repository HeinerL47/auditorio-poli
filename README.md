# Sistema de Reservas - Auditorio Politecnico Grancolombiano

Proyecto **Spring Boot 3.3 + Java 17 + Thymeleaf + Spring Security + JPA + MySQL 8**.

Aplica la identidad visual del Politecnico Grancolombiano (azul institucional,
cian, tipografia Open Sans, logo institucional y cierre "Somos diferentes,
somos Poli.") tanto en la interfaz web como en los correos automaticos.

---

## Requisitos previos

- JDK 17
- Maven 3.8+ (IntelliJ lo trae embebido)
- **MySQL 8.x** corriendo en `localhost:3306`
- (Opcional) Cuenta de Gmail con verificacion en 2 pasos para envio real de correos.

---

## 1. Crear la base de datos MySQL

Conectarse a MySQL y ejecutar:

```sql
CREATE DATABASE auditorio_poli CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

(o ejecutar el script `src/main/resources/db/setup-mysql.sql`).

> Las tablas las crea Hibernate automaticamente al iniciar la aplicacion.
> Los usuarios demo los inserta `DataSeeder.java` solo la primera vez.

---

## 2. Abrir y ejecutar en IntelliJ

1. Descomprimir el ZIP.
2. `File` -> `Open...` -> seleccionar la carpeta `auditorio-poli`.
3. Esperar a que IntelliJ descargue las dependencias de Maven.
4. (Opcional) Configurar las variables de entorno (paso 3).
5. Ejecutar la clase `co.edu.poligran.auditorio.AuditorioApplication`.
6. Abrir <http://localhost:8080>.

---

## 3. Variables de entorno (Run -> Edit Configurations -> Environment variables)

| Nombre | Cuando se necesita | Valor |
|---|---|---|
| `DB_USER` | Si tu MySQL NO usa `root` | tu usuario de MySQL |
| `DB_PASS` | Si tu MySQL NO usa `root` | tu clave de MySQL |
| `MAIL_ENABLED` | Solo si quieres enviar correos reales | `true` |
| `MAIL_USERNAME` | Solo si `MAIL_ENABLED=true` | tu correo de Gmail |
| `MAIL_PASSWORD` | Solo si `MAIL_ENABLED=true` | App Password de 16 caracteres |
| `MAIL_FROM` | Opcional | remitente (por defecto = `MAIL_USERNAME`) |
| `APP_PUBLIC_URL` | Opcional | URL publica para el boton "Ir al Auditorio" en correos (por defecto `http://localhost:8080`) |

### Generar un App Password de Google

1. Entra a tu cuenta de Google -> **Seguridad**.
2. Activa la **Verificacion en 2 pasos** si no la tienes.
3. Busca **"App passwords"** -> <https://myaccount.google.com/apppasswords>
4. Crea una nueva con nombre "Auditorio Poli" -> te genera un codigo de **16 caracteres**.

Al iniciar deberias ver:
```
INFO  c.e.p.a.s.NotificacionService - Correo configurado correctamente. Remitente: tu.correo@gmail.com
```

---

## Cuentas demo (sembradas la primera vez)

| Rol | Tipo | Correo | Contrasena |
|---|---|---|---|
| Admin del auditorio | — | admin@poligran.edu.co | admin123 |
| Operativo | — | operativo@poligran.edu.co | ops123 |
| Solicitante | Docente | docente@poligran.edu.co | docente123 |
| Solicitante | Administrativo | admin.staff@poligran.edu.co | staff123 |
| Solicitante | Externo | externo@empresa.com | externo123 |

> **Importante:** estos correos son ficticios y NO recibiran realmente los
> mails. Para probar el envio de verdad, registra un usuario externo con tu
> correo personal en `/registro`.

---

## Cuando se envian correos

| Evento | Destinatarios |
|---|---|
| Crear reserva | Solicitante + Admin |
| Aprobar reserva | Solicitante + Operativos |
| Rechazar reserva | Solicitante |
| Cancelar reserva | Solicitante + Admin + Operativos |
| 1 hora antes del evento (auto) | Solicitante + Operativos |

Todos los correos comparten una plantilla HTML con el **manual de marca del
Politecnico Grancolombiano**:

- Cabecera azul institucional `#0F385A` con barra cian `#1FB2DE`.
- **Logo institucional embebido** (no depende de imagenes externas).
- Tipografia **Open Sans** (con respaldo seguro para clientes de correo).
- Saludo en azul con barra lateral cian.
- **Badge de color** segun el estado: verde APROBADA, amarillo PENDIENTE,
  rosa RECHAZADA / CANCELADA, cian RECORDATORIO.
- Tabla con los datos de la reserva (ID, solicitante, tipo de evento,
  seccion, inicio, fin, costo).
- Boton "Ir al Auditorio" hacia `APP_PUBLIC_URL`.
- Pie azul oscuro con el cierre **"Somos diferentes, somos Poli."**

---

## Roles del sistema

- **SOLICITANTE** (Docente / Administrativo / Externo): crea, consulta y cancela sus reservas.
- **ADMIN_AUDITORIO**: aprueba/rechaza, gestiona tarifas, bloqueos y reportes.
- **OPERATIVO**: solo lectura; ve calendario y reportes para preparar logistica.

## Funcionalidades

- Login y registro publico (solo externos)
- Calendario interactivo con FullCalendar
- Crear reserva con validaciones automaticas (horario 8:00-21:00, max 4h, minimo 1 semana de anticipacion, maximo 1 mes, mismo dia, ano actual, bloqueos, solapamiento)
- Cotizacion automatica para externos
- Aprobar / rechazar (admin) con observacion
- Cancelar (hasta 4h antes)
- Bloqueos fijos recurrentes (precargados: clases de Bienestar Lun/Mie 10-12 en B3, mantenimiento dominical)
- Tarifas configurables por seccion (B1, B2, B3, COMPLETO)
- Notificaciones por correo via Gmail SMTP, con plantilla HTML y manual de marca Poli (logo embebido + tipografia + paleta + tagline)
- Recordatorios automaticos 1 hora antes (scheduler cada 5 min)
- Reportes con graficos (Chart.js): por hora, dia, mes, internos vs externos
- Exportar historial a CSV

## Estructura

```
src/main/java/co/edu/poligran/auditorio/
  AuditorioApplication.java
  config/        (Security, DataSeeder)
  controller/    (Auth, Dashboard, Reserva, Admin, Calendario, Reporte)
  model/         (Usuario, Reserva, Bloqueo, Tarifa, enums)
  repository/    (JPA repositories)
  service/       (ReservaService, NotificacionService, ReporteService, RecordatorioScheduler)
src/main/resources/
  application.properties
  db/setup-mysql.sql
  static/css/styles.css
  static/images/poli-logo.jpg
  templates/               (vistas Thymeleaf)
```

## Resetear la base de datos

```sql
DROP DATABASE auditorio_poli;
CREATE DATABASE auditorio_poli CHARACTER SET utf8mb4;
```

## Errores comunes

- **`Authentication failed`** al enviar correo: Gmail rechazo la clave. Asegurate de usar un **App Password** (no tu clave normal de Gmail) y de tener activada la verificacion en 2 pasos.
- **`Access denied for user`** (MySQL): revisa `DB_USER` y `DB_PASS`.
- **`Communications link failure`**: MySQL no esta corriendo en `localhost:3306`.
- **`Unknown database`**: ejecuta `CREATE DATABASE auditorio_poli;`.
- **`Port 8080 already in use`**: ya tienes algo en ese puerto. En IntelliJ agrega la variable `PORT=8090` (o cualquier otro libre).
