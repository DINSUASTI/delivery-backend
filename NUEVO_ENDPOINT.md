# Nuevo Endpoint: Consulta de Estado de Solicitudes

## Descripción
Se ha creado un nuevo endpoint GET que permite consultar el estado completo de una solicitud (webhook) por su ID, retornando información de las tablas `webhook_response` y `webhook_log`.

## Idempotencia Implementada
Se ha implementado idempotencia en el webhook para evitar procesar solicitudes duplicadas. Cada solicitud genera una clave única basada en:
- eventType
- source
- fileName
- fileType
- tamaño del archivo

Si se recibe una solicitud idéntica, se retorna un código 409 (CONFLICT) con información de la solicitud existente.

## Endpoint

```
GET /webhook/{id}/status
```

## Parámetros

- **id** (Path Parameter, requerido): Identificador de la solicitud (webhook response)

## Respuesta Exitosa (200 OK)

```json
{
  "responseId": 1,
  "eventType": "PDF_UPLOAD",
  "source": "pdf-webhook",
  "fileName": "documento.pdf",
  "fileType": "application/pdf",
  "state": "PROCESSING",
  "receivedAt": "2026-04-28T10:30:00Z",
  "message": "Solicitud en procesamiento",
  "logs": [
    {
      "id": 1,
      "timestamp": "2026-04-28T10:30:00Z",
      "eventType": "PDF_UPLOAD",
      "source": "pdf-webhook",
      "action": "RECEIVED",
      "message": "Solicitud recibida con archivo: documento.pdf",
      "statusCode": 0,
      "payloadSummary": "Archivo: documento.pdf, Tipo: application/pdf"
    },
    {
      "id": 2,
      "timestamp": "2026-04-28T10:32:00Z",
      "eventType": "PDF_UPLOAD",
      "source": "pdf-webhook",
      "action": "PROCESSING",
      "message": "Procesando solicitud",
      "statusCode": 202,
      "payloadSummary": "En proceso"
    }
  ]
}
```

## Respuesta No Encontrada (404 Not Found)

```json
{
  "status": "NOT_FOUND",
  "message": "Solicitud con ID 999 no encontrada"
}
```

## Respuesta de Solicitud Duplicada (409 Conflict)

```json
{
  "status": "DUPLICATE_REQUEST",
  "responseId": 1,
  "eventType": "PDF_UPLOAD",
  "fileName": "documento.pdf",
  "fileType": "application/pdf",
  "state": "PROCESSING",
  "receivedAt": "2026-04-28T10:30:00Z",
  "message": "Esta solicitud ya fue procesada anteriormente",
  "idempotencyKey": "a1b2c3d4e5f6..."
}
```

## Ejemplo de Uso

### Con cURL:
```bash
curl -X GET http://localhost:8080/webhook/1/status
```

### Con Postman:
1. Crear una solicitud GET
2. URL: `http://localhost:8080/webhook/{id}/status`
3. Enviar

### Con JavaScript/Fetch:
```javascript
fetch('http://localhost:8080/webhook/1/status')
  .then(response => response.json())
  .then(data => console.log(data))
  .catch(error => console.error('Error:', error));
```

## Cambios Realizados

### 1. Nueva Utilidad: `IdempotencyKeyGenerator.java`
- **Ubicación**: `src/main/java/com/prueba/delivery/util/IdempotencyKeyGenerator.java`
- Genera claves únicas SHA-256 basadas en parámetros de la solicitud
- Evita procesar solicitudes duplicadas

### 2. Entidad Actualizada: `WebhookResponse.java`
- Se agregó campo `idempotencyKey` (único, nullable, longitud 64)
- Se agregaron getters/setters para el nuevo campo
- Se corrigió el campo `message` que había sido sobrescrito

### 3. Repositorio Actualizado: `WebhookResponseRepository.java`
- Se agregó método: `findByIdempotencyKey(String idempotencyKey)`
- Permite buscar solicitudes existentes por clave de idempotencia

### 4. Controlador Actualizado: `WebhookController.java`
- Se implementó lógica de idempotencia en `receiveTestWebhook`
- Verifica existencia de clave antes de procesar
- Retorna 409 CONFLICT para solicitudes duplicadas
- Registra auditoría en `webhook_log` con acción "DUPLICATE_REQUEST"

### 5. Actualización: `WebhookLogRepository.java`
- Se cambió el método: `findByResponseIdOrderByIdAsc(Long responseId)`
- Ordena logs por ID ascendente (orden cronológico)

### 6. Nuevo DTO: `RequestStatusResponse.java`
- **Ubicación**: `src/main/java/com/prueba/delivery/dto/RequestStatusResponse.java`
- Combina WebhookResponse + List<WebhookLog>
- Contiene clase interna WebhookLogEntry para los registros

## Estados de la Solicitud

Los posibles estados (`WebhookStatus`) son:
- `PENDING`: Solicitud recibida, pendiente de procesamiento
- `PROCESSING`: En proceso de envío a servicio externo
- `SUCCESS`: Completada exitosamente
- `FAILED_TEMPORARY`: Error transitorio (reintentable)
- `FAILED_PERMANENT`: Error definitivo (no reintentable)

## Flujo de Información

1. Se recibe una solicitud en `/webhook/test`
2. Se genera una clave de idempotencia SHA-256 basada en los parámetros
3. Se verifica si ya existe una solicitud con la misma clave
4. **Si existe**: Se retorna 409 CONFLICT y se registra en `webhook_log` con acción "DUPLICATE_REQUEST"
5. **Si no existe**: Se crea un registro en `webhook_response` con estado `PENDING`
6. Se crea un registro inicial en `webhook_log` con acción `RECEIVED`
7. Se intenta llamar al servicio externo
8. El estado se actualiza según el resultado
9. Se agregan más registros en `webhook_log` con cada evento importante

Al consultar con `GET /webhook/{id}/status`, se obtiene:
- La información completa de la solicitud (de `webhook_response`)
- Todos los eventos/logs asociados (de `webhook_log`) ordenados por ID ascendente (orden cronológico)
