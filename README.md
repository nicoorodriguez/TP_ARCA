# TP ARCA — Sistema de gestión con Java y Spring Boot

Proyecto desarrollado en Java con Spring Boot, orientado a la implementación de una aplicación de gestión con persistencia de datos y componentes para la integración con servicios de ARCA (ex AFIP).

## Tecnologías utilizadas

- **Java 20**
- **Spring Boot 3.2.3**
- **Spring Web** para el desarrollo de servicios web
- **Spring Data JPA** para el acceso a datos
- **H2 Database** como base de datos local
- **Maven** para la gestión de dependencias
- **Lombok** para reducir código repetitivo
- **Swagger / OpenAPI** para la documentación de la API
- **iText y Apache POI** para trabajar con documentos PDF y planillas
- **Bouncy Castle, JAX-WS y JAXB** como dependencias para la integración con servicios de ARCA/AFIP

## Estructura del proyecto

```text
TP-JPA/
├── data/                          # Archivos de datos locales
├── frontend/                      # Código del frontend
├── src/                           # Código fuente Java y recursos
├── pom.xml                        # Configuración Maven
└── ticket-homo-wsfe.properties    # Configuración relacionada con WSFE
```

> Las carpetas `target/` y `.idea/` se generan localmente y normalmente no se incluyen en el repositorio.

## Requisitos

- JDK **20**
- Maven (o un wrapper Maven, si está disponible en el proyecto)
- IDE compatible con Java, por ejemplo IntelliJ IDEA o VS Code

## Instalación y ejecución

1. Clonar el repositorio:

   ```bash
   git clone https://github.com/pedronav1310/TP_ARCA.git
   ```

2. Entrar al directorio del proyecto:

   ```bash
   cd TP_ARCA
   ```

3. Compilar el proyecto:

   ```bash
   mvn clean install
   ```

4. Iniciar la aplicación:

   ```bash
   mvn spring-boot:run
   ```

> La ejecución puede requerir configurar previamente propiedades locales, la base de datos y, para los servicios de ARCA, las credenciales y certificados correspondientes.

## Documentación de la API

El proyecto incluye la dependencia **springdoc OpenAPI**. Si la aplicación expone la interfaz Swagger UI con la configuración predeterminada, se puede consultar en:

```text
http://localhost:8080/swagger-ui/index.html
```

La dirección depende del puerto y la configuración del servidor.

## Integración con ARCA / AFIP

El proyecto incluye dependencias orientadas a trabajar con servicios de ARCA/AFIP, como **WSAA** (autenticación) y **WSFE** (facturación electrónica). Su funcionamiento depende de la configuración del entorno, los certificados y las credenciales autorizadas.

**Seguridad:** no subir al repositorio contraseñas, claves privadas, certificados ni archivos de configuración con datos sensibles.
