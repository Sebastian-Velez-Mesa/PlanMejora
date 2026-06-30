# NexusCore ERP v1.0 — Guía de Uso y Endpoints REST
**Sistema Educativo | Matriz de Riesgos de Seguridad Informática | SENA ADSO**

---

## Cómo ejecutar el proyecto

```bash
# Desde la raíz del proyecto (donde está pom.xml)
mvn spring-boot:run
```
El sistema inicia en: `http://localhost:8080`

### TLS y keystore local
El servicio está configurado para correr con HTTPS en el puerto 8443 usando un keystore PKCS12. El archivo `src/main/resources/keystore.p12` no se versiona en el repositorio; debe generarse localmente antes de ejecutar la app:

```bash
keytool -genkeypair -alias nexuscore -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore src/main/resources/keystore.p12 -storepass changeit -validity 3650 -dname "CN=localhost, OU=Development, O=NexusCore, L=Local, ST=Local, C=CO"
```

Para despliegue, el keystore debe proporcionarse como secreto o archivo montado por la plataforma, por ejemplo mediante la variable de entorno `SSL_KEY_STORE` y `SSL_KEY_STORE_PASSWORD`.

Consola H2 (base de datos): `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:nexuscoredb`
- Usuario: `nexus_admin`
- Contraseña: `Nexus$3cur3!2024`

---

## Usuarios de Demostración (creados automáticamente al iniciar)

| Usuario | Contraseña   | Rol        | Acceso                          |
|---------|--------------|------------|---------------------------------|
| admin   | Admin@123!   | ROLE_ADMIN | Todos los módulos               |
| user    | User@123!    | ROLE_USER  | Solo lectura financiero y core  |

---

## Flujo de Uso: Paso a Paso

### PASO 1 — Obtener el Token JWT (Login)

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin@123!"
}
```

**Respuesta exitosa:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "username": "admin",
  "role": "ROLE_ADMIN",
  "mensaje": "Autenticación exitosa. Token válido por 1 hora."
}
```

### PASO 2 — Usar el Token en las demás peticiones

```
Authorization: Bearer <token_obtenido_en_paso_1>
```

---

## Endpoints por Módulo

### MÓDULO 1 — Clientes (Activo 1: Datos Personales PII)
> Solo `ROLE_ADMIN`

```http
# Listar todos los clientes
GET /api/customers
Authorization: Bearer {token}

# Obtener cliente por ID
GET /api/customers/{id}
Authorization: Bearer {token}

# Registrar nuevo cliente
POST /api/customers
Authorization: Bearer {token}
Content-Type: application/json
{
  "nombre": "Juan Pérez García",
  "cedulaNit": "1023456789",
  "correo": "juan.perez@email.com",
  "telefono": "3001234567"
}

# Eliminar cliente
DELETE /api/customers/{id}
Authorization: Bearer {token}
```

---

### MÓDULO 2 — Autenticación (Activo 2: Credenciales)

```http
# Login (público)
POST /api/auth/login
{ "username": "admin", "password": "Admin@123!" }

# Registrar nuevo usuario (público)
POST /api/auth/register
{ "username": "nuevo_usuario", "password": "MiClave@123!" }
# Política: mínimo 8 chars, 1 mayúscula, 1 número, 1 carácter especial
```

---

### MÓDULO 3 — Core ERP (Activo 3: Servidor de Aplicaciones)
> Cualquier usuario autenticado

```http
# Ver estado de todos los módulos
GET /api/core/estado
Authorization: Bearer {token}

# Ejecutar proceso central del ERP
POST /api/core/proceso?modulo=FINANCIERO
Authorization: Bearer {token}
```

---

### MÓDULO 4 — Financiero (Activo 4: Transacciones y Facturación)
> GET: ADMIN y USER | POST: solo ADMIN

```http
# Historial completo de transacciones
GET /api/financial/historial
Authorization: Bearer {token}

# Balance ingresos vs egresos
GET /api/financial/balance
Authorization: Bearer {token}

# Filtrar por tipo
GET /api/financial/historial/INGRESO
GET /api/financial/historial/EGRESO
Authorization: Bearer {token}

# Registrar nueva transacción (solo ADMIN)
POST /api/financial/transaccion
Authorization: Bearer {token}
Content-Type: application/json
{
  "referencia": "FAC-2024-001",
  "monto": 5500000.00,
  "tipo": "INGRESO",
  "descripcion": "Pago factura cliente corporativo"
}
```

---

### MÓDULO 5 — Nómina/Salarios (Activo 5: Datos de Empleados)
> Solo `ROLE_ADMIN`

```http
# Listar nómina completa
GET /api/salaries
Authorization: Bearer {token}

# Ver empleado por ID
GET /api/salaries/{id}
Authorization: Bearer {token}

# Total de la nómina mensual
GET /api/salaries/nomina-total
Authorization: Bearer {token}

# Registrar empleado en nómina
POST /api/salaries
Authorization: Bearer {token}
Content-Type: application/json
{
  "empleado": "María López",
  "cedula": "52987654",
  "salarioBase": 2500000.00,
  "cuentaBancaria": "00123456789012",
  "cargo": "Analista de Sistemas"
}
```
> ⚠️ La cuenta bancaria se enmascara en la respuesta: `**********9012`

---

## Resumen de Mitigaciones por Activo

| Activo | Módulo      | Vulnerabilidad Original             | Mitigación Aplicada                        |
|--------|-------------|-------------------------------------|--------------------------------------------|
| 1      | Customer    | HTTP plano, sin control de acceso   | HTTPS, JWT, hasRole('ADMIN'), Audit Logs   |
| 2      | Auth        | MD5/texto plano, sin JWT            | BCrypt strength=12, JWT HS512, 1hr exp.    |
| 3      | Core        | Sin auth, stack traces al cliente   | Auth requerida, correlation IDs, try-catch |
| 4      | Financial   | Sin logs, datos modificables        | SLF4J logs, updatable=false, BigDecimal    |
| 5      | Salary      | Datos bancarios expuestos           | hasRole('ADMIN'), enmascaramiento, AES tip |

---

## Tecnologías Utilizadas

- **Java 17** + **Spring Boot 3.2.5**
- **Spring Security 6** (JWT + BCrypt)
- **Spring Data JPA** + **H2** (demo)
- **JJWT 0.11.5** (tokens HS512)
- **Lombok** (boilerplate)
- **SLF4J / Logback** (logging estructurado)
- **Jakarta Validation** (validación de entrada)
