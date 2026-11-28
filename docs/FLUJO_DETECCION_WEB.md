# Conectar Aula e Intranet

## ¿Cómo conectas tus cuentas?

```mermaid
flowchart TD
    Abrir["Abres Cuentas"] --> Datos["Escribes las direcciones y tus datos de acceso"]
    Datos --> Sincronizar["Pulsas Sincronizar"]
    Sincronizar --> Aula["StudyLock abre el Aula Virtual"]
    Aula --> LoginA{"¿Hace falta iniciar sesión?"}
    LoginA -->|Sí| AccesoA["StudyLock intenta entrar o espera a que termines"]
    LoginA -->|No| LeerA["Lee la página que está abierta"]
    AccesoA --> LeerA
    LeerA --> Intranet{"¿Hay cuenta de Intranet?"}
    Intranet -->|Sí| AbrirI["Abre la Intranet y entra"]
    AbrirI --> LeerI["Lee la página que está abierta"]
    Intranet -->|No| Juntar["Usa solo lo encontrado en el Aula"]
    LeerI --> Juntar
    LeerA --> Juntar
    Juntar --> Arbol["Muestra cursos, secciones y archivos encontrados"]
```

## ¿Cómo decide qué enlace es un curso?

```mermaid
flowchart TD
    Pagina["Página abierta en el navegador de StudyLock"] --> Esperar["Espera un momento a que aparezcan enlaces"]
    Esperar --> Buscar["Busca enlaces visibles en la página"]
    Buscar --> Tipos["Lee enlaces, botones y algunas rutas de la página"]
    Tipos --> Filtrar["Quita menús, enlaces vacíos y archivos que no ayudan"]
    Filtrar --> Candidatos{"¿Quedó algún enlace que parezca importante?"}
    Candidatos -->|No| Nada["No hay nada que clasificar en esta página"]
    Candidatos -->|Sí| Modelo{"¿El modelo de IA está listo?"}
    Modelo -->|Sí| ClasificaIA["La IA propone un tipo para cada enlace"]
    Modelo -->|No| Reglas["StudyLock usa reglas sencillas de texto y dirección"]
    ClasificaIA --> Valida["StudyLock revisa que la respuesta tenga formato correcto"]
    Valida -->|No sirve| Reglas
    Valida -->|Sirve| TiposEnlace["Cada enlace queda como Curso, Sílabo, Sección u Otro"]
    Reglas --> TiposEnlace
    TiposEnlace --> Revisar{"¿Activaste el modo diagnóstico?"}
    Revisar -->|Sí| Confirmar["Tú revisas y confirmas los enlaces"]
    Revisar -->|No| Automatica["StudyLock acepta cursos o sílabos con confianza suficiente"]
    Confirmar --> AbrirCursos["Abre los enlaces aceptados"]
    Automatica --> AbrirCursos
```

## ¿Cómo revisa las páginas de un curso?

```mermaid
flowchart TD
    Entrada["StudyLock abre un curso o un sílabo"] --> Cola["Guarda la dirección en una lista por revisar"]
    Cola --> HayMas{"¿Quedan páginas y no se alcanzó el límite?"}
    HayMas -->|No| Fin["Termina y muestra lo encontrado"]
    HayMas -->|Sí| Sacar["Toma la siguiente dirección"]
    Sacar --> Visto{"¿Ya abrió esa dirección?"}
    Visto -->|Sí| HayMas
    Visto -->|No| Abrir["Abre la página"]
    Abrir --> Enlaces["Lee sus enlaces y busca archivos"]
    Enlaces --> Archivos{"¿Encontró archivos?"}
    Archivos -->|Sí| Guardar["Guarda el título y los archivos como una sección"]
    Archivos -->|No| Seguir["No crea una sección todavía"]
    Guardar --> BuscarSecciones["Busca enlaces que parezcan unidades o temas"]
    Seguir --> BuscarSecciones
    BuscarSecciones --> MismoSitio["Solo sigue direcciones del mismo sitio y relacionadas"]
    MismoSitio --> Cola
```

## ¿Qué pasa con un sílabo encontrado?

```mermaid
flowchart LR
    Aula["Cursos encontrados en el Aula"] --> Emparejar["Compara los nombres de cursos y sílabos"]
    Intranet["Sílabos encontrados en la Intranet"] --> Emparejar
    Emparejar --> Coincide{"¿Un sílabo coincide con un curso?"}
    Coincide -->|Sí| Dentro["Añade el sílabo dentro de ese curso"]
    Coincide -->|No| Suelto["Lo guarda como material agregado y muestra un aviso"]
    Dentro --> Arbol["Árbol final para revisar y elegir archivos"]
    Suelto --> Arbol
```

## ¿Qué revisar si no aparecen cursos?

```mermaid
flowchart TD
    NoCurso["No aparece ningún curso o sílabo"] --> Enlaces{"¿StudyLock encontró enlaces?"}
    Enlaces -->|No| Pagina["Revisar si abrió el portal correcto y terminó el acceso"]
    Enlaces -->|Sí| Clasificacion{"¿Aparecen clasificados como Curso o Sílabo?"}
    Clasificacion -->|No| Filtro["Revisar el texto y las direcciones que el filtro acepta"]
    Clasificacion -->|Sí| Aceptacion{"¿Quedaron confirmados?"}
    Aceptacion -->|No| Confirmar["Revisar el modo diagnóstico y las marcas elegidas"]
    Aceptacion -->|Sí| Archivos{"¿Aparecen archivos dentro del curso?"}
    Archivos -->|No| Recorrido["Revisar si el sitio muestra enlaces y si cargó a tiempo"]
    Archivos -->|Sí| Guia["Continuar con FLUJO_GENERACION_PREGUNTAS.md"]
```
