# Agregar material y crear preguntas

## ¿De dónde puedes traer material?

```mermaid
flowchart TD
    Entrada{"¿De dónde viene el material?"}
    Entrada -->|Aula o Intranet| Portal["Eliges archivos encontrados al sincronizar"]
    Entrada -->|Archivo| Archivo["Eliges un archivo del teléfono"]
    Entrada -->|Texto| TextoPegado["Pegas texto en Importar"]

    Portal --> Descargar["StudyLock descarga el archivo usando tu sesión"]
    Archivo --> LeerArchivo["StudyLock lee el archivo"]
    TextoPegado --> LeerTexto["StudyLock usa el texto pegado"]
    Descargar --> Bytes{"¿Se pudo leer el material?"}
    LeerArchivo --> Bytes
    Bytes -->|No| Error["Muestra un aviso y no crea preguntas"]
    Bytes -->|Sí| Tipo{"¿Es un PDF?"}
    Tipo -->|Sí| PDF["Intenta extraer el texto del PDF"]
    Tipo -->|No| TextoArchivo["Lee el contenido como texto"]
    PDF --> Contenido["Obtiene el texto para estudiar"]
    TextoArchivo --> Contenido
    LeerTexto --> Contenido
    Contenido --> Preguntas["Crea preguntas con IA local o con reglas"]
    Preguntas --> Previsualizar["Muestra los temas y preguntas"]
    Previsualizar --> Guardar["Confirmas para guardar"]
    Guardar --> Cursos["Las preguntas aparecen en tus cursos"]
```

## ¿Cómo crea preguntas la IA?

```mermaid
flowchart TD
    Texto["Texto del material"] --> Regla["StudyLock prepara preguntas sencillas como respaldo"]
    Texto --> IAActiva{"¿La IA está activada y descargada?"}
    IAActiva -->|No| Respaldo["Usa las preguntas de respaldo"]
    IAActiva -->|Sí| Recortar["Toma hasta 6000 caracteres del tema"]
    Recortar --> Gemma["Gemma trabaja en el teléfono"]
    Gemma --> Formato["Devuelve preguntas en formato JSON"]
    Formato --> Revisar["StudyLock revisa preguntas y opciones"]
    Revisar --> Valida{"¿Hay al menos 2 preguntas correctas y utilizables?"}
    Valida -->|No| Respaldo
    Valida -->|Sí| Unir["Usa preguntas de IA y añade respaldo no repetido"]
    Regla --> Respaldo
    Unir --> Guardar["Guarda preguntas, respuestas y temas"]
    Respaldo --> Guardar
```

## ¿Cómo revisa las preguntas?

```mermaid
flowchart LR
    Pregunta["Pregunta creada"] --> Cuatro["Debe tener 4 opciones diferentes"]
    Cuatro --> Correcta["Debe indicar cuál es la respuesta correcta"]
    Correcta --> Explicacion["Debe incluir una explicación"]
    Explicacion --> Cita["Si viene de IA, la cita debe existir en el material"]
    Cita --> Resultado{"¿Pasa las revisiones?"}
    Resultado -->|Sí| Conservar["StudyLock puede usar la pregunta"]
    Resultado -->|No| Descartar["StudyLock la descarta y recurre al respaldo"]
```

## ¿Por qué el texto del PDF puede salir raro?

```mermaid
flowchart TD
    TextoExtraido["Texto extraído del PDF"] --> Palabras{"¿Las palabras ya están separadas o mal leídas?"}
    Palabras -->|Sí| Origen["El problema puede venir de cómo está hecho el PDF"]
    Origen --> Extractor["El extractor actual no hace OCR ni entiende todas las fuentes PDF"]
    Extractor --> Respaldo["Las reglas y la IA reciben ese texto tal como salió"]
    Palabras -->|No| Modelo{"¿La pregunta parece mal formateada?"}
    Modelo -->|Sí| Salida["La respuesta del modelo pudo tener mala forma aunque pase revisiones básicas"]
    Modelo -->|No| Bien["El texto de entrada y la pregunta se ven bien"]
    Respaldo --> Revisar["Revisar primero el texto extraído, luego la pregunta generada"]
    Salida --> Revisar
```

## ¿Cómo se descarga el modelo?

```mermaid
flowchart TD
    Ajustes["Se solicita descargar el modelo"] --> Internet{"¿Hay conexión y se cumple la regla de Wi-Fi?"}
    Internet -->|No| Esperar["La descarga espera o se reintenta"]
    Internet -->|Sí| Descargar["Descarga Gemma en segundo plano"]
    Descargar --> Comprobar["Revisa tamaño y huella del archivo"]
    Comprobar --> Correcto{"¿El archivo es válido?"}
    Correcto -->|No| Reintentar["Vuelve a intentar la descarga"]
    Correcto -->|Sí| Listo["La IA local queda lista"]
    Listo --> Uso["Se puede usar para preguntas y clasificación de enlaces"]
```
