package pe.edu.unjbg.studylock.data.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * Contenido académico inicial (semilla).
 *
 * En producción esto lo alimenta el conector del Aula Virtual / Intranet + el generador de
 * preguntas. Mientras el conector no exista, esta semilla permite que toda la app funcione de
 * punta a punta y que el equipo de contenido valide la calidad pedagógica.
 */
object SeedContent {

    private data class SQuestion(
        val prompt: String,
        val options: List<String>,
        val correct: Int,
        val explanation: String,
        val difficulty: Int = 1,
    )

    private data class STopic(val name: String, val questions: List<SQuestion>)

    private data class SCourse(
        val id: String,
        val name: String,
        val professor: String,
        val accent: Long,
        val topics: List<STopic>,
    )

    private val courses = listOf(
        SCourse(
            id = "db",
            name = "Base de Datos",
            professor = "Ing. Marco Quispe",
            accent = 0xFF9073C9,
            topics = listOf(
                STopic(
                    "Modelo relacional",
                    listOf(
                        SQuestion(
                            "¿Cuál es una función principal de una clave primaria?",
                            listOf(
                                "Duplicar registros",
                                "Identificar cada registro de forma única",
                                "Eliminar tablas automáticamente",
                                "Ordenar siempre los registros",
                            ),
                            1,
                            "Una clave primaria identifica sin duplicados cada registro de una tabla.",
                        ),
                        SQuestion(
                            "¿Qué representa una relación en el modelo relacional?",
                            listOf(
                                "Una tabla con filas y columnas",
                                "Un archivo de texto plano",
                                "Un índice de búsqueda",
                                "Un procedimiento almacenado",
                            ),
                            0,
                            "En el modelo relacional una relación es una tabla: filas (tuplas) y columnas (atributos).",
                        ),
                        SQuestion(
                            "¿Qué es una clave foránea?",
                            listOf(
                                "Un atributo que referencia la clave primaria de otra tabla",
                                "Una clave que cifra los datos",
                                "El identificador del servidor",
                                "Un campo calculado automáticamente",
                            ),
                            0,
                            "La clave foránea crea el vínculo entre tablas y protege la integridad referencial.",
                        ),
                    ),
                ),
                STopic(
                    "SQL básico",
                    listOf(
                        SQuestion(
                            "¿Qué comando SQL se utiliza para consultar registros de una tabla?",
                            listOf("INSERT", "SELECT", "DELETE", "UPDATE"),
                            1,
                            "SELECT recupera filas; INSERT, UPDATE y DELETE modifican datos.",
                        ),
                        SQuestion(
                            "¿Qué cláusula se usa para filtrar filas en una consulta?",
                            listOf("GROUP BY", "WHERE", "ORDER BY", "HAVING"),
                            1,
                            "WHERE aplica el filtro antes de agrupar; HAVING filtra grupos ya agregados.",
                        ),
                        SQuestion(
                            "¿Qué comando agrega nuevas filas a una tabla?",
                            listOf("INSERT", "SELECT", "ALTER", "MERGE"),
                            0,
                            "INSERT incorpora filas nuevas; ALTER cambia la estructura de la tabla.",
                        ),
                    ),
                ),
                STopic(
                    "Normalización",
                    listOf(
                        SQuestion(
                            "¿Cuál es el objetivo de la primera forma normal (1FN)?",
                            listOf(
                                "Eliminar valores atómicos",
                                "Eliminar grupos repetidos y valores multivaluados",
                                "Eliminar claves foráneas",
                                "Eliminar índices",
                            ),
                            1,
                            "La 1FN exige valores atómicos: una celda, un valor.",
                        ),
                        SQuestion(
                            "¿Qué dependencia elimina la segunda forma normal (2FN)?",
                            listOf(
                                "Las dependencias transitivas",
                                "Las dependencias parciales de una clave compuesta",
                                "Las claves subrogadas",
                                "Los atributos derivados",
                            ),
                            1,
                            "La 2FN requiere que todo atributo dependa de la clave completa, no de una parte.",
                        ),
                        SQuestion(
                            "¿Qué elimina la tercera forma normal (3FN)?",
                            listOf(
                                "Las dependencias transitivas",
                                "Los grupos repetidos",
                                "Las vistas materializadas",
                                "Las transacciones largas",
                            ),
                            0,
                            "La 3FN elimina dependencias transitivas: un atributo no clave no debe depender de otro no clave.",
                        ),
                    ),
                ),
                STopic(
                    "Transacciones",
                    listOf(
                        SQuestion(
                            "¿Qué garantiza la 'A' de ACID?",
                            listOf("Atomicidad", "Autenticidad", "Agregación", "Alineación"),
                            0,
                            "Atomicidad: la transacción se aplica completa o no se aplica.",
                        ),
                        SQuestion(
                            "¿Qué comando confirma definitivamente una transacción?",
                            listOf("ROLLBACK", "COMMIT", "SAVEPOINT", "GRANT"),
                            1,
                            "COMMIT hace permanentes los cambios de la transacción.",
                        ),
                        SQuestion(
                            "¿Qué hace ROLLBACK?",
                            listOf(
                                "Deshacer los cambios no confirmados",
                                "Repetir la última consulta",
                                "Crear una copia de seguridad",
                                "Bloquear la tabla",
                            ),
                            0,
                            "ROLLBACK revierte la transacción hasta el último punto de guardado o su inicio.",
                        ),
                    ),
                ),
            ),
        ),
        SCourse(
            id = "isw",
            name = "Ingeniería de Software",
            professor = "Mg. Lucía Fernández",
            accent = 0xFF35B6A4,
            topics = listOf(
                STopic(
                    "Requerimientos",
                    listOf(
                        SQuestion(
                            "¿Qué describe un requerimiento funcional?",
                            listOf(
                                "Restricciones de calidad del sistema",
                                "Un comportamiento o función que el sistema debe cumplir",
                                "El presupuesto del proyecto",
                                "La arquitectura de despliegue",
                            ),
                            1,
                            "Los funcionales describen qué hace el sistema; los no funcionales, cómo debe hacerlo.",
                        ),
                        SQuestion(
                            "¿Qué caracteriza a un requerimiento no funcional?",
                            listOf(
                                "Define pantallas y formularios",
                                "Establece atributos de calidad como rendimiento o seguridad",
                                "Describe casos de uso",
                                "Detalla el modelo de datos",
                            ),
                            1,
                            "Los no funcionales son medibles: rendimiento, seguridad, usabilidad, disponibilidad.",
                        ),
                        SQuestion(
                            "¿Qué técnica ayuda a validar requerimientos con el usuario?",
                            listOf(
                                "Entrevistas y prototipos",
                                "Compilar el proyecto",
                                "Revisar el repositorio Git",
                                "Medir el uso de CPU",
                            ),
                            0,
                            "Entrevistas y prototipos permiten confirmar la comprensión antes de construir.",
                        ),
                    ),
                ),
                STopic(
                    "UML y modelado",
                    listOf(
                        SQuestion(
                            "¿Qué diagrama UML modela clases y sus relaciones?",
                            listOf("Diagrama de clases", "Diagrama de Gantt", "Diagrama de red", "Diagrama de despliegue físico"),
                            0,
                            "El diagrama de clases muestra atributos, métodos y relaciones entre clases.",
                        ),
                        SQuestion(
                            "¿Qué representa un caso de uso?",
                            listOf(
                                "La interacción entre actores y el sistema",
                                "El cronograma del proyecto",
                                "La estructura de la base de datos",
                                "El plan de pruebas unitarias",
                            ),
                            0,
                            "Un caso de uso describe un objetivo del actor frente al sistema.",
                        ),
                        SQuestion(
                            "¿Qué relación UML indica herencia?",
                            listOf("Generalización", "Asociación", "Dependencia", "Realización"),
                            0,
                            "La generalización representa que una clase especializa a otra.",
                        ),
                    ),
                ),
                STopic(
                    "Metodologías ágiles",
                    listOf(
                        SQuestion(
                            "¿En qué se basa Scrum?",
                            listOf(
                                "Iteraciones cortas llamadas sprints",
                                "Un único entregable al final del año",
                                "Documentación exhaustiva previa",
                                "Equipos sin roles definidos",
                            ),
                            0,
                            "Scrum trabaja en sprints con incrementos entregables y revisión continua.",
                        ),
                        SQuestion(
                            "¿Qué es un MVP?",
                            listOf(
                                "La versión mínima con valor que permite validar una hipótesis",
                                "El manual de ventas del producto",
                                "La versión final sin errores",
                                "Un prototipo sin usuarios reales",
                            ),
                            0,
                            "El MVP valida aprendizaje con el mínimo esfuerzo de construcción.",
                        ),
                        SQuestion(
                            "¿Qué rol prioriza el product backlog?",
                            listOf("Scrum Master", "Product Owner", "Development Team", "Stakeholder"),
                            1,
                            "El Product Owner maximiza el valor y ordena el backlog.",
                        ),
                    ),
                ),
                STopic(
                    "Pruebas de software",
                    listOf(
                        SQuestion(
                            "¿Qué prueba verifica una unidad aislada de código?",
                            listOf("Prueba unitaria", "Prueba de aceptación", "Prueba de humo", "Prueba de carga"),
                            0,
                            "La prueba unitaria aísla una función o clase y valida su comportamiento.",
                        ),
                        SQuestion(
                            "¿Qué busca una prueba de regresión?",
                            listOf(
                                "Que los cambios no rompan funcionalidad existente",
                                "Medir la velocidad de la red",
                                "Reducir el tamaño del APK",
                                "Generar documentación automática",
                            ),
                            0,
                            "La regresión protege lo que ya funcionaba tras un cambio.",
                        ),
                        SQuestion(
                            "¿Quién realiza la prueba de aceptación?",
                            listOf(
                                "El usuario o cliente",
                                "Solo el líder técnico",
                                "El equipo de infraestructura",
                                "El proveedor de hardware",
                            ),
                            0,
                            "La aceptación valida el sistema contra las necesidades del usuario.",
                        ),
                    ),
                ),
            ),
        ),
        SCourse(
            id = "redes",
            name = "Redes de Computadoras",
            professor = "Ing. Jorge Mamani",
            accent = 0xFFEBAE5F,
            topics = listOf(
                STopic(
                    "Modelo OSI",
                    listOf(
                        SQuestion(
                            "¿Cuántas capas tiene el modelo OSI?",
                            listOf("5", "6", "7", "8"),
                            2,
                            "OSI tiene 7 capas, desde física hasta aplicación.",
                        ),
                        SQuestion(
                            "¿Qué capa se encarga del enrutamiento de paquetes?",
                            listOf("Capa de red", "Capa de enlace", "Capa de sesión", "Capa física"),
                            0,
                            "La capa de red (3) decide la ruta mediante direcciones lógicas como IP.",
                        ),
                        SQuestion(
                            "¿Qué capa garantiza la entrega extremo a extremo?",
                            listOf("Transporte", "Aplicación", "Presentación", "Enlace de datos"),
                            0,
                            "La capa de transporte segmenta, controla flujo y ofrece entrega confiable con TCP.",
                        ),
                    ),
                ),
                STopic(
                    "Direccionamiento IP",
                    listOf(
                        SQuestion(
                            "¿Cuántos bits tiene una dirección IPv4?",
                            listOf("16", "32", "64", "128"),
                            1,
                            "IPv4 usa 32 bits; IPv6 usa 128.",
                        ),
                        SQuestion(
                            "¿Qué máscara corresponde a una red /24?",
                            listOf("255.255.0.0", "255.255.255.0", "255.255.255.128", "255.0.0.0"),
                            1,
                            "/24 deja 24 bits de red y 8 de host: 255.255.255.0.",
                        ),
                        SQuestion(
                            "¿Qué función cumple DHCP?",
                            listOf(
                                "Asignar direcciones IP automáticamente",
                                "Cifrar el tráfico",
                                "Resolver nombres de dominio",
                                "Enrutar paquetes entre redes",
                            ),
                            0,
                            "DHCP entrega configuración de red (IP, máscara, gateway) a los clientes.",
                        ),
                    ),
                ),
                STopic(
                    "Protocolos de transporte",
                    listOf(
                        SQuestion(
                            "¿Qué protocolo es orientado a conexión?",
                            listOf("UDP", "TCP", "ICMP", "ARP"),
                            1,
                            "TCP establece conexión con three-way handshake y confirma entrega.",
                        ),
                        SQuestion(
                            "¿Qué protocolo prioriza baja latencia sin garantizar entrega?",
                            listOf("TCP", "UDP", "FTP", "SMTP"),
                            1,
                            "UDP no confirma entrega: ideal para voz, video y juegos.",
                        ),
                        SQuestion(
                            "¿Qué identifica de forma única una conexión TCP?",
                            listOf(
                                "IP y puerto de origen y destino",
                                "Solo la dirección MAC",
                                "El nombre del host",
                                "La máscara de subred",
                            ),
                            0,
                            "La tupla de 4 valores identifica cada conexión TCP.",
                        ),
                    ),
                ),
                STopic(
                    "Seguridad en redes",
                    listOf(
                        SQuestion(
                            "¿Qué aporta HTTPS frente a HTTP?",
                            listOf(
                                "Cifrado de la comunicación con TLS",
                                "Mayor velocidad de descarga",
                                "Compresión de imágenes",
                                "Resolución de nombres más rápida",
                            ),
                            0,
                            "HTTPS cifra el canal con TLS y autentica el servidor con certificados.",
                        ),
                        SQuestion(
                            "¿Qué hace un firewall?",
                            listOf(
                                "Filtrar tráfico según reglas de seguridad",
                                "Asignar direcciones IP",
                                "Almacenar copias de seguridad",
                                "Traducir lenguajes de programación",
                            ),
                            0,
                            "El firewall permite o bloquea tráfico según políticas definidas.",
                        ),
                        SQuestion(
                            "¿Qué ataque intercepta la comunicación entre dos partes?",
                            listOf("Man-in-the-middle", "Denegación de servicio", "Fuerza bruta", "Phishing"),
                            0,
                            "En MITM el atacante se sitúa entre cliente y servidor para leer o alterar datos.",
                        ),
                    ),
                ),
            ),
        ),
    )

    fun populate(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            courses.forEachIndexed { courseIndex, course ->
                db.insertOrThrow("courses", null, ContentValues().apply {
                    put("id", course.id)
                    put("name", course.name)
                    put("professor", course.professor)
                    put("semester", "2026-I")
                    put("accent", course.accent)
                    put("sort", courseIndex)
                })
                course.topics.forEachIndexed { topicIndex, topic ->
                    val topicId = "${course.id}-t${topicIndex + 1}"
                    db.insertOrThrow("topics", null, ContentValues().apply {
                        put("id", topicId)
                        put("course_id", course.id)
                        put("name", topic.name)
                        put("sort", topicIndex)
                    })
                    topic.questions.forEachIndexed { questionIndex, question ->
                        val questionId = "$topicId-q${questionIndex + 1}"
                        db.insertOrThrow("questions", null, ContentValues().apply {
                            put("id", questionId)
                            put("topic_id", topicId)
                            put("prompt", question.prompt)
                            put("explanation", question.explanation)
                            put("difficulty", question.difficulty)
                            put("sort", questionIndex)
                        })
                        question.options.forEachIndexed { optionIndex, text ->
                            db.insertOrThrow("options", null, ContentValues().apply {
                                put("id", "$questionId-o${optionIndex + 1}")
                                put("question_id", questionId)
                                put("text", text)
                                put("is_correct", if (optionIndex == question.correct) 1 else 0)
                                put("sort", optionIndex)
                            })
                        }
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
