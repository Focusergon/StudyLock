# StudyLock — mapa general

## ¿Qué puedes hacer en StudyLock?

```mermaid
flowchart TD
    Inicio["Abres StudyLock"] --> PrimeraVez{"¿Es tu primera vez?"}
    PrimeraVez -->|Sí| Bienvenida["Ves la bienvenida"]
    Bienvenida --> Perfil["Escribes tu nombre y correo"]
    Perfil --> Permisos["Das los permisos que necesita Android"]
    Permisos --> ElegirApps["Eliges qué aplicaciones limitar"]
    ElegirApps --> InicioApp["Llegas a Inicio"]
    PrimeraVez -->|No| InicioApp

    InicioApp --> ElegirAccion{"¿Qué quieres hacer?"}
    ElegirAccion --> Repasar["Repasar cursos"]
    ElegirAccion --> Importar["Agregar material y crear preguntas"]
    ElegirAccion --> Conectar["Conectar Aula o Intranet"]
    ElegirAccion --> VerProgreso["Ver tu progreso"]
    ElegirAccion --> Ajustes["Cambiar ajustes"]
    ElegirAccion --> Horario["Revisar tu horario"]

    Repasar --> Preguntas["Responder preguntas de estudio"]
    Preguntas --> Minutos["Ganar minutos según tus respuestas"]
    Minutos --> AbrirApp["Usar una aplicación limitada"]
    Importar --> Crear["Crear preguntas desde texto o archivos"]
    Conectar --> Cursos["Encontrar cursos y archivos"]
    Cursos --> Elegir["Elegir archivos para crear preguntas"]
    Elegir --> Crear
    Crear --> Guardar["Guardar las preguntas en un curso"]
    Guardar --> Repasar
    VerProgreso --> InicioApp
    Ajustes --> InicioApp
    Horario --> InicioApp
```

## ¿Qué pasa cuando abres una app limitada?

```mermaid
flowchart LR
    Persona["Usas el teléfono"] --> AppDetecta["StudyLock ve qué aplicación está abierta"]
    AppDetecta --> Limitada{"¿Está en tu lista de aplicaciones limitadas?"}
    Limitada -->|No| Continua["La aplicación abre normalmente"]
    Limitada -->|Sí| Tiempo{"¿Te quedan minutos?"}
    Tiempo -->|Sí| Reloj["Empieza o continúa el tiempo permitido"]
    Tiempo -->|No| Reto["StudyLock muestra un reto de preguntas"]
    Reto --> Contesta["Respondes hasta 2 preguntas"]
    Contesta --> Premio["StudyLock calcula los minutos ganados"]
    Premio --> Reloj
    Reto -->|No hay preguntas disponibles| Emergencia["Puedes usar el desbloqueo de emergencia"]
    Emergencia --> Reloj
    Reloj -->|Se acaba el tiempo| Reto
    Reloj -->|Sales de la aplicación| Pausa["El reloj pausa mientras no usas esa aplicación"]
    Pausa -->|Vuelves a la aplicación| Reloj
```

## ¿Cómo ganas minutos al estudiar?

```mermaid
flowchart TD
    Estudio["Empiezas un repaso"] --> Tipo{"¿Qué tipo de repaso?"}
    Tipo -->|Curso| Curso["Preguntas de ese curso"]
    Tipo -->|Repaso rápido| Debil["Preguntas de temas que necesitan más práctica"]
    Tipo -->|Práctica| Practica["Preguntas para probar la app"]
    Curso --> Responder["Respondes una pregunta"]
    Debil --> Responder
    Practica --> Responder
    Responder --> Correcta{"¿Tu respuesta es correcta?"}
    Correcta -->|Sí o no| Siguiente{"¿Quedan preguntas?"}
    Siguiente -->|Sí| Responder
    Siguiente -->|No| Cierre["StudyLock termina el repaso"]
    Cierre --> Minutos["Calcula los minutos ganados"]
    Cierre --> TipoRepaso{"¿Fue repaso normal o práctica?"}
    TipoRepaso -->|Repaso normal| Progreso["Actualiza aciertos y cuándo conviene repasar el tema"]
    TipoRepaso -->|Práctica| SinEstadisticas["Da minutos, pero no cambia tus estadísticas"]
    Minutos --> Inicio["Puedes ver el resultado en Inicio y Progreso"]
    Progreso --> Inicio
    SinEstadisticas --> Inicio
```

## ¿Qué puedes cambiar en Ajustes?

```mermaid
flowchart LR
    Ajustes["Ajustes"] --> Meta["Cambiar meta diaria"]
    Ajustes --> Recordatorios["Activar o pausar recordatorios"]
    Ajustes --> Apps["Elegir aplicaciones limitadas"]
    Ajustes --> Modelo["Descargar o revisar el modelo local"]
    Ajustes --> IA["Activar o desactivar preguntas con IA"]
    Ajustes --> Auto["Activar sincronización automática"]
    Ajustes --> Cuentas["Cambiar o sincronizar cuentas"]
```

## ¿Cómo funciona el horario y cuándo llega un aviso?

```mermaid
flowchart TD
    Horario["Abres Horario"] --> Agregar["Escribes curso, día y hora"]
    Agregar --> RevisarHora{"¿La hora de fin es posterior al inicio?"}
    RevisarHora -->|No| Corregir["StudyLock muestra un aviso para corregirla"]
    Corregir --> Agregar
    RevisarHora -->|Sí| GuardarHorario["Guarda la clase"]
    GuardarHorario --> Franja["El horario ayuda a encontrar ratos libres"]
    Franja --> Aviso{"¿Es buen momento para recordarte estudiar?"}
    Aviso -->|Estás en clase o empieza pronto| NoAviso["No envía aviso ahora"]
    Aviso -->|Meta diaria alcanzada| NoAviso
    Aviso -->|Ya envió un aviso hace poco| NoAviso
    Aviso -->|Tienes un rato libre| Recordatorio["Puede enviarte un recordatorio"]
```

## ¿Dónde ves tu avance?

```mermaid
flowchart TD
    Sesion["Terminas un repaso normal"] --> Datos["Se guardan tus respuestas y minutos"]
    Datos --> Inicio["Inicio muestra el resumen del día"]
    Datos --> Progreso["Progreso muestra aciertos e historial"]
    Datos --> Prioridad["Los temas difíciles pueden aparecer antes en próximos repasos"]
```

## ¿Cuándo se sincronizan los cursos solos?

```mermaid
flowchart TD
    Auto["Sincronización automática activada"] --> Modelo{"¿El modelo ya está descargado?"}
    Modelo -->|No| Esperar["Espera a que el modelo esté listo"]
    Modelo -->|Sí| Cuentas{"¿Guardaste las cuentas del Aula y la Intranet?"}
    Cuentas -->|No| EsperarCuentas["Espera a que agregues las cuentas"]
    Cuentas -->|Sí| Wifi{"¿La regla de Wi-Fi se cumple?"}
    Wifi -->|No| EsperarWifi["Espera a estar conectado a Wi-Fi"]
    Wifi -->|Sí| Frecuencia{"¿Ya pasó un día desde el último intento?"}
    Frecuencia -->|No| EsperarDia["Espera al siguiente día"]
    Frecuencia -->|Sí| Sincronizar["Abre Cuentas y empieza a sincronizar"]
```

## ¿Qué explica cada guía?

```mermaid
flowchart LR
    Mapa["Mapa general"] --> Web["Conectar portales y descubrir archivos"]
    Mapa --> Material["Importar archivos y generar preguntas"]
    Web --> GuiaWeb["Ver FLUJO_DETECCION_WEB.md"]
    Material --> GuiaMaterial["Ver FLUJO_GENERACION_PREGUNTAS.md"]
```
