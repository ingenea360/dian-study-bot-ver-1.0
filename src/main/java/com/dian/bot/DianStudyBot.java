package com.dian.bot;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DianStudyBot {

    // ==========================================
    // CONFIGURACIÓN SEGURA DE ACCESOS (VARIABLES DE ENTORNO)
    // ==========================================
    private static final String TELEGRAM_BOT_TOKEN = System.getenv("TELEGRAM_BOT_TOKEN") != null 
            ? System.getenv("TELEGRAM_BOT_TOKEN") 
            : "TU_TELEGRAM_BOT_TOKEN_AQUI";

    private static final String TELEGRAM_CHAT_ID = System.getenv("TELEGRAM_CHAT_ID") != null 
            ? System.getenv("TELEGRAM_CHAT_ID") 
            : "TU_TELEGRAM_CHAT_ID_AQUI";

    private static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY") != null 
            ? System.getenv("GEMINI_API_KEY") 
            : "TU_GEMINI_API_KEY_AQUI";

    // Carpeta de documentos (descargados automáticamente de Google Drive)
    private static final String CARPETA_DOCUMENTOS = "./documentos";

    // Intervalo en minutos para enviar preguntas periódicas automáticas
    private static final int FRECUENCIA_AUTOMATICA_MINUTOS = 45;

    private static final Random random = new Random();
    private static final List<File> listaDocumentosEstudio = new ArrayList<>();
    private static final List<JsonObject> bancoPreguntasLocal = new ArrayList<>();
    private static String perfilCargoTexto = "";
    private static String indicadoresEvaluacionTexto = "";
    private static long ultimoUpdateId = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🏛️ TUTOR IA DE ESTUDIO - CONCURSO DIAN 2676");
        System.out.println("   Sincronizado con Google Drive (170 documentos)");
        System.out.println("   4 Opciones (A, B, C, D) - Juicio Situacional");
        System.out.println("=================================================");

        // 1. Escanear todo el material temático descargado de Google Drive
        File dirDoc = new File(CARPETA_DOCUMENTOS);
        escanearMaterialEstudio(dirDoc);
        System.out.println("📚 Materiales temáticos cargados: " + listaDocumentosEstudio.size());

        // 2. Cargar documentos rectores (cargo.pdf e indicadores.pdf de forma inteligente en cualquier subcarpeta)
        cargarDocumentosRectores(dirDoc);

        // 3. Cargar banco de respaldo de alta fidelidad CNSC
        inicializarBancoPreguntasLocal();

        // 4. Notificar en Telegram la activación en la nube
        enviarMensajeTelegram("☁️ *¡Tutor DIAN 2676 Conectado y en la Nube (24/7)!* 🏛️\n\n" +
                "📚 *Materiales de estudio sincronizados:* " + listaDocumentosEstudio.size() + " documentos\n" +
                "📋 *Alineado con:*\n" +
                "• Perfil y funciones de tu empleo (`cargo.pdf`)\n" +
                "• Indicadores y criterios de desempeño (`indicadores a evaluar.pdf`)\n\n" +
                "📝 *Formato de evaluación:*\n" +
                "• Casos reales de Juicio Situacional CNSC\n" +
                "• 4 Opciones de respuesta (*A, B, C, D*)\n" +
                "• Retroalimentación jurídica inmediata con confeti 🎉\n\n" +
                "⏱️ Preguntas automáticas cada *" + FRECUENCIA_AUTOMATICA_MINUTOS + " minutos*.\n" +
                "💡 Escribe *pregunta* en el chat en cualquier momento para ponerte a prueba.");

        // 5. Disparar de inmediato la primera pregunta contextualizada
        generarYEnviarPregunta();

        // 6. Temporizador para preguntas periódicas
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("\n⏰ Disparador programado: enviando caso situacional...");
                generarYEnviarPregunta();
            } catch (Exception e) {
                System.out.println("Error en envío automático: " + e.getMessage());
            }
        }, FRECUENCIA_AUTOMATICA_MINUTOS, FRECUENCIA_AUTOMATICA_MINUTOS, TimeUnit.MINUTES);

        // 7. Escuchar comandos en tiempo real
        System.out.println("👂 Bot a la escucha en Telegram. Escribe 'pregunta' en el chat...");
        iniciarEscuchaTelegram();
    }

    private static void cargarDocumentosRectores(File dirRaiz) {
        // Búsqueda inteligente del archivo de cargo
        File fileCargo = buscarArchivoRecursivo(dirRaiz, "cargo");
        if (fileCargo != null && fileCargo.exists()) {
            System.out.println("📄 Analizando perfil del cargo desde: " + fileCargo.getAbsolutePath());
            perfilCargoTexto = extraerTextoPdf(fileCargo);
            if (perfilCargoTexto.length() > 3000) {
                perfilCargoTexto = perfilCargoTexto.substring(0, 3000);
            }
            System.out.println("   -> Perfil del cargo cargado (" + perfilCargoTexto.length() + " caracteres).");
        } else {
            perfilCargoTexto = "Servidor público de la DIAN encargado de procesos técnicos, tributarios, aduaneros, fiscalización y soporte funcional.";
            System.out.println("ℹ️ Usando perfil funcional general de la DIAN.");
        }

        // Búsqueda inteligente de indicadores
        File fileIndicadores = buscarArchivoRecursivo(dirRaiz, "indicador");
        if (fileIndicadores != null && fileIndicadores.exists()) {
            System.out.println("📊 Analizando indicadores de evaluación desde: " + fileIndicadores.getAbsolutePath());
            indicadoresEvaluacionTexto = extraerTextoPdf(fileIndicadores);
            if (indicadoresEvaluacionTexto.length() > 2500) {
                indicadoresEvaluacionTexto = indicadoresEvaluacionTexto.substring(0, 2500);
            }
            System.out.println("   -> Indicadores de desempeño cargados (" + indicadoresEvaluacionTexto.length() + " caracteres).");
        } else {
            indicadoresEvaluacionTexto = "Evaluación de competencias laborales, juicio situacional, ética pública, probidad y debido proceso administrativo.";
            System.out.println("ℹ️ Usando indicadores de desempeño generales CNSC.");
        }
    }

    private static File buscarArchivoRecursivo(File dir, String terminoClave) {
        if (!dir.exists()) return null;
        File[] archivos = dir.listFiles();
        if (archivos == null) return null;

        for (File archivo : archivos) {
            if (archivo.isDirectory()) {
                File encontrado = buscarArchivoRecursivo(archivo, terminoClave);
                if (encontrado != null) return encontrado;
            } else {
                String nombre = archivo.getName().toLowerCase();
                if (nombre.contains(terminoClave.toLowerCase()) && (nombre.endsWith(".pdf") || nombre.endsWith(".docx"))) {
                    return archivo;
                }
            }
        }
        return null;
    }

    private static void escanearMaterialEstudio(File directorio) {
        if (!directorio.exists()) return;
        File[] archivos = directorio.listFiles();
        if (archivos == null) return;

        for (File archivo : archivos) {
            if (archivo.isDirectory()) {
                escanearMaterialEstudio(archivo);
            } else {
                String nombre = archivo.getName().toLowerCase();
                if (nombre.endsWith(".docx") || nombre.endsWith(".pdf") || nombre.endsWith(".txt")) {
                    listaDocumentosEstudio.add(archivo);
                }
            }
        }
    }

    private static synchronized void generarYEnviarPregunta() {
        try {
            String nombreDoc = "Normativa y Directrices DIAN";
            String textoTema = "Procedimientos tributarios, principios de legalidad, debido proceso, ética pública y régimen funcional de la DIAN.";

            if (!listaDocumentosEstudio.isEmpty()) {
                File docSeleccionado = listaDocumentosEstudio.get(random.nextInt(listaDocumentosEstudio.size()));
                nombreDoc = docSeleccionado.getName();
                textoTema = extraerTexto(docSeleccionado);
                if (textoTema.length() < 150) {
                    textoTema = "Procedimientos, principios de legalidad, ética pública y régimen funcional de la DIAN.";
                }
                if (textoTema.length() > 2500) {
                    int maxInicio = Math.max(0, textoTema.length() - 2000);
                    int inicio = random.nextInt(maxInicio + 1);
                    textoTema = textoTema.substring(inicio, Math.min(textoTema.length(), inicio + 2000));
                }
                System.out.println("📖 Material de estudio base: " + docSeleccionado.getName());
            }

            System.out.println("🤖 Generando caso situacional de 4 opciones...");
            JsonObject preguntaJson = consultarGemini(nombreDoc, textoTema);

            if (preguntaJson == null) {
                System.out.println("ℹ️ Utilizando caso calibrado del banco local de la DIAN 2676...");
                preguntaJson = obtenerPreguntaDelBancoLocal();
            }

            String tema = preguntaJson.has("tema") ? preguntaJson.get("tema").getAsString() : nombreDoc.replace(".docx", "").replace(".pdf", "").replace("_", " ");
            String indicadorEvaluado = preguntaJson.has("indicador_evaluado") ? preguntaJson.get("indicador_evaluado").getAsString() : "Competencia Funcional / Comportamental";
            String caso = preguntaJson.get("caso").getAsString();
            String pregunta = preguntaJson.get("pregunta").getAsString();
            JsonArray opciones = preguntaJson.getAsJsonArray("opciones");
            int correctIndex = preguntaJson.get("correct_index").getAsInt();
            String explicacion = preguntaJson.get("explicacion").getAsString();

            if (explicacion.length() > 195) {
                explicacion = explicacion.substring(0, 192) + "...";
            }

            // 1. Mensaje informativo previo
            StringBuilder mensajeCaso = new StringBuilder();
            mensajeCaso.append("🏛️ *SIMULACRO DIAN 2676 — JUICIO SITUACIONAL*\n");
            mensajeCaso.append("🎯 *Indicador Evaluable:* _").append(indicadorEvaluado).append("_\n");
            mensajeCaso.append("📚 *Eje / Guía:* `").append(tema).append("`\n\n");
            mensajeCaso.append("📋 *Caso Laboral:*\n").append(caso).append("\n\n");
            mensajeCaso.append("❓ *Conducta Esperada:*\n").append(pregunta).append("\n\n");
            mensajeCaso.append("🔹 *Opciones de Respuesta:*\n");

            char letra = 'A';
            for (JsonElement op : opciones) {
                mensajeCaso.append("*").append(letra++).append(")* ").append(op.getAsString()).append("\n\n");
            }
            mensajeCaso.append("👇 _Selecciona tu respuesta en el cuestionario interactivo abajo:_");

            enviarMensajeTelegram(mensajeCaso.toString());

            // 2. Cuestionario Quiz interactivo oficial
            enviarQuizTelegram(pregunta, opciones, correctIndex, explicacion);
            System.out.println("✅ Pregunta y Quiz enviados exitosamente a Telegram.");

        } catch (Exception e) {
            System.out.println("❌ Error generando/enviando pregunta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String extraerTexto(File archivo) {
        String nombre = archivo.getName().toLowerCase();
        try {
            if (nombre.endsWith(".docx")) {
                try (ZipFile zip = new ZipFile(archivo)) {
                    ZipEntry entry = zip.getEntry("word/document.xml");
                    if (entry != null) {
                        try (InputStream is = zip.getInputStream(entry)) {
                            String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                            return xml.replaceAll("</w:p>", "\n")
                                      .replaceAll("<[^>]+>", " ")
                                      .replaceAll("\\s+", " ")
                                      .trim();
                        }
                    }
                }
            } else if (nombre.endsWith(".pdf")) {
                return extraerTextoPdf(archivo);
            } else if (nombre.endsWith(".txt")) {
                return Files.readString(archivo.toPath(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            System.out.println("Aviso al extraer texto de " + archivo.getName() + ": " + e.getMessage());
        }
        return "";
    }

    private static String extraerTextoPdf(File archivo) {
        try (PDDocument document = PDDocument.load(archivo)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            System.out.println("Aviso leyendo PDF " + archivo.getName() + ": " + e.getMessage());
            return "";
        }
    }

    private static JsonObject consultarGemini(String nombreArchivo, String fragmentoTexto) {
        String[] modelos = {"gemini-flash-latest", "gemini-flash-lite-latest", "gemini-2.5-flash"};

        String prompt = "Actúa como un psicómetra y evaluador experto de la Comisión Nacional del Servicio Civil (CNSC) para el concurso de méritos DIAN 2676.\n\n" +
                "CRITERIOS DE CONTEXTUALIZACIÓN OBLIGATORIOS:\n" +
                "1. PERFIL Y RESPONSABILIDADES DEL EMPLEO:\n\"" + perfilCargoTexto.replace("\"", "'") + "\"\n\n" +
                "2. INDICADORES DE DESEMPEÑO Y CONDUCTAS EVALUABLES:\n\"" + indicadoresEvaluacionTexto.replace("\"", "'") + "\"\n\n" +
                "3. FUENTE TEMÁTICA / NORMATIVA ESPECÍFICA ('" + nombreArchivo + "'):\n\"" + fragmentoTexto.replace("\"", "'") + "\"\n\n" +
                "INSTRUCCIONES DE CONSTRUCCIÓN DEL CASO SITUACIONAL (METODOLOGÍA CNSC):\n" +
                "• Diseña un caso laboral verosímil y detallado que enfrente directamente el funcionario en este cargo específico de la DIAN.\n" +
                "• El caso debe poner a prueba uno de los INDICADORES DE DESEMPEÑO evaluables definidos en el documento rector.\n" +
                "• La pregunta debe formular cuál es la conducta, decisión o procedimiento legal y correcto a seguir.\n" +
                "• Construye EXACTAMENTE 4 OPCIONES DE RESPUESTA (A, B, C, D):\n" +
                "    - Una opción CORRECTA que cumple plenamente con la norma, la ética pública y el indicador óptimo.\n" +
                "    - Tres DISTRACTORES plausibles que representan errores de procedimiento comunes, omisiones o conductas deficientes.\n" +
                "    - IMPORTANTE: Cada opción en la lista debe ser sintetizada (MÁXIMO 95 caracteres por opción) para ser compatible con la interfaz del Quiz de Telegram.\n" +
                "• El índice de la opción correcta (0 para A, 1 para B, 2 para C, 3 para D).\n" +
                "• Una justificación jurídica concisa (MÁXIMO 180 caracteres) citando la norma, artículo o manual aplicable.\n\n" +
                "Responde OBLIGATORIAMENTE en formato JSON plano (sin formato ```json):\n" +
                "{\n" +
                "  \"tema\": \"Tema evaluado\",\n" +
                "  \"indicador_evaluado\": \"Nombre del indicador o competencia evaluada\",\n" +
                "  \"caso\": \"Descripción detallada del caso situacional laboral...\",\n" +
                "  \"pregunta\": \"Frente a esta situación, ¿cuál es la conducta que debe adoptar el servidor público?\",\n" +
                "  \"opciones\": [\"Opción A (máx 95 chars)\", \"Opción B (máx 95 chars)\", \"Opción C (máx 95 chars)\", \"Opción D (máx 95 chars)\"],\n" +
                "  \"correct_index\": 0,\n" +
                "  \"explicacion\": \"Según la norma X y la función del cargo...\"\n" +
                "}";

        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(part);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contents);
        byte[] inputBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);

        for (String modelo : modelos) {
            try {
                String urlApi = "https://generativelanguage.googleapis.com/v1beta/models/" + modelo + ":generateContent?key=" + GEMINI_API_KEY;
                URL url = new URL(urlApi);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(30000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(inputBytes, 0, inputBytes.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (InputStream is = conn.getInputStream()) {
                        String respuesta = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        JsonObject jsonRes = JsonParser.parseString(respuesta).getAsJsonObject();
                        String textoGenerado = jsonRes.getAsJsonArray("candidates")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("content")
                                .getAsJsonArray("parts")
                                .get(0).getAsJsonObject()
                                .get("text").getAsString();

                        textoGenerado = textoGenerado.trim();
                        if (textoGenerado.startsWith("```json")) {
                            textoGenerado = textoGenerado.substring(7);
                        } else if (textoGenerado.startsWith("```")) {
                            textoGenerado = textoGenerado.substring(3);
                        }
                        if (textoGenerado.endsWith("```")) {
                            textoGenerado = textoGenerado.substring(0, textoGenerado.length() - 3);
                        }
                        return JsonParser.parseString(textoGenerado.trim()).getAsJsonObject();
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static JsonObject obtenerPreguntaDelBancoLocal() {
        return bancoPreguntasLocal.get(random.nextInt(bancoPreguntasLocal.size()));
    }

    private static void inicializarBancoPreguntasLocal() {
        JsonObject c1 = new JsonObject();
        c1.addProperty("tema", "Conflicto de Intereses y Régimen Disciplinario");
        c1.addProperty("indicador_evaluado", "Comportamiento Ético y Probidad en la Función Pública");
        c1.addProperty("caso", "Usted es servidor público de la DIAN asignado al área operativa. Al recibir un expediente tributario asignado por reparto para su trámite, observa que el representante legal suplente de la sociedad investigada es su hermano. Su superior le solicita no demorar la actuación debido a la cercanía del vencimiento de términos.");
        c1.addProperty("pregunta", "Frente a esta situación, ¿cuál es la conducta procedente que usted debe adoptar conforme al régimen disciplinario?");
        JsonArray op1 = new JsonArray();
        op1.add("Continuar la revisión dejando constancia por escrito de imparcialidad.");
        op1.add("Declarar de inmediato el impedimento por escrito y abstenerse de actuar.");
        op1.add("Pedir verbalmente a un compañero que firme mientras usted proyecta los actos.");
        op1.add("Devolver el expediente al reparto general por carga de trabajo sin informar.");
        c1.add("opciones", op1);
        c1.addProperty("correct_index", 1);
        c1.addProperty("explicacion", "Art. 40 Ley 1952 de 2019: El servidor debe declarar inmediatamente el impedimento y abstenerse de toda actuación.");
        bancoPreguntasLocal.add(c1);

        JsonObject c2 = new JsonObject();
        c2.addProperty("tema", "Derecho de Petición y Transparencia Administrativa");
        c2.addProperty("indicador_evaluado", "Orientación al Usuario y Cumplimiento Normativo");
        c2.addProperty("caso", "Un ciudadano presenta una solicitud formal ante la DIAN requiriendo copias simples de actos administrativos de carácter general y circulares informativas emitidas por la entidad durante el último mes. El documento no contiene solicitud de conceptos ni información bajo reserva legal.");
        c2.addProperty("pregunta", "¿Cuál es el término legal perentorio que tiene la entidad para resolver y poner a disposición las copias?");
        JsonArray op2 = new JsonArray();
        op2.add("10 días hábiles siguientes a la fecha de radicación de la petición.");
        op2.add("15 días hábiles conforme al término ordinario de peticiones de interés general.");
        op2.add("30 días hábiles al requerir compilación documental previa.");
        op2.add("5 días hábiles mediante respuesta verbal inmediata en ventanilla.");
        c2.add("opciones", op2);
        c2.addProperty("correct_index", 0);
        c2.addProperty("explicacion", "Art. 14 Ley 1755 de 2015: Las peticiones de documentos y de información deben resolverse dentro de los 10 días siguientes.");
        bancoPreguntasLocal.add(c2);

        JsonObject c3 = new JsonObject();
        c3.addProperty("tema", "Reserva Tributaria y Acceso a la Información");
        c3.addProperty("indicador_evaluado", "Seguridad de la Información y Protección de Datos");
        c3.addProperty("caso", "Un concejal municipal radica una solicitud de información exigiendo a la Dirección Seccional de la DIAN copias completas de las declaraciones de renta presentadas por varios contratistas del municipio, argumentando que las requiere para ejercer control político local.");
        c3.addProperty("pregunta", "¿Cuál es la respuesta y procedimiento que debe adoptar el servidor público competente?");
        JsonArray op3 = new JsonArray();
        op3.add("Entregar las copias completas en cumplimiento del deber de colaboración política.");
        op3.add("Negar la solicitud por estar amparada bajo estricta reserva legal tributaria.");
        op3.add("Permitir la consulta presencial de los expedientes sin expedir fotocopias.");
        op3.add("Remitir la solicitud al Ministerio de Hacienda para que autorice la entrega.");
        c3.add("opciones", op3);
        c3.addProperty("correct_index", 1);
        c3.addProperty("explicacion", "Art. 583 Estatuto Tributario: La información de las declaraciones tributarias goza de reserva legal estricta.");
        bancoPreguntasLocal.add(c3);

        JsonObject c4 = new JsonObject();
        c4.addProperty("tema", "Seguridad de Plataforma y Gobernanza Tecnológica");
        c4.addProperty("indicador_evaluado", "Gestión de Eventos y Cumplimiento de Estándares TIC");
        c4.addProperty("caso", "Durante una jornada de alta congestión operativa, a un funcionario de la misma dependencia se le bloquea su usuario de red. Este le solicita a usted que le preste provisionalmente sus credenciales y clave de acceso institucional para no suspender el trámite de usuarios que esperan en sala.");
        c4.addProperty("pregunta", "De acuerdo con las políticas y directrices de seguridad de la información de la DIAN, usted debe:");
        JsonArray op4 = new JsonArray();
        op4.add("Facilitar la clave temporalmente supervisando directamente la labor de su compañero.");
        op4.add("Rechazar la solicitud, reportar la novedad a soporte y recordar que es personal.");
        op4.add("Crear un usuario genérico de emergencia en el terminal para continuar.");
        op4.add("Compartir el acceso si cuenta con el visto bueno verbal del coordinador.");
        c4.add("opciones", op4);
        c4.addProperty("correct_index", 1);
        c4.addProperty("explicacion", "Manual de Seguridad de la Información DIAN: Las credenciales de acceso son estrictamente personales e intransferibles.");
        bancoPreguntasLocal.add(c4);

        JsonObject c5 = new JsonObject();
        c5.addProperty("tema", "Procedimiento Administrativo Tributario - Notificaciones");
        c5.addProperty("indicador_evaluado", "Rigor Jurídico y Procedimiento Técnico Administrativo");
        c5.addProperty("caso", "La dependencia profiere un requerimiento especial a un contribuyente y lo notifica electrónicamente a la dirección de correo registrada en el RUT. En el sistema consta que el correo fue enviado exitosamente el lunes 5 de octubre.");
        c5.addProperty("pregunta", "¿Cuándo se entiende legalmente surtida la notificación electrónica según el Estatuto Tributario?");
        JsonArray op5 = new JsonArray();
        op5.add("En el momento exacto en que el contribuyente abre y lee el mensaje.");
        op5.add("A los cinco (5) días hábiles siguientes contados desde el envío del correo.");
        op5.add("Al día hábil siguiente de ingresar al servidor del contribuyente.");
        op5.add("Diez (10) días calendario posteriores a la confirmación de entrega.");
        c5.add("opciones", op5);
        c5.addProperty("correct_index", 1);
        c5.addProperty("explicacion", "Art. 566-1 Estatuto Tributario: La notificación electrónica se entiende surtida 5 días hábiles después del envío del correo.");
        bancoPreguntasLocal.add(c5);
    }

    private static void enviarMensajeTelegram(String texto) {
        try {
            String urlApi = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage";
            URL url = new URL(urlApi);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            JsonObject body = new JsonObject();
            body.addProperty("chat_id", TELEGRAM_CHAT_ID);
            body.addProperty("text", texto);
            body.addProperty("parse_mode", "Markdown");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            conn.getResponseCode();
        } catch (Exception e) {
            System.out.println("Error enviando mensaje a Telegram: " + e.getMessage());
        }
    }

    private static void enviarQuizTelegram(String pregunta, JsonArray opciones, int correctIndex, String explicacion) {
        try {
            String urlApi = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendPoll";
            URL url = new URL(urlApi);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            if (pregunta.length() > 290) {
                pregunta = pregunta.substring(0, 287) + "...";
            }

            JsonArray opcionesSanitizadas = new JsonArray();
            for (JsonElement op : opciones) {
                String txt = op.getAsString();
                if (txt.length() > 98) {
                    txt = txt.substring(0, 95) + "...";
                }
                opcionesSanitizadas.add(txt);
            }

            if (explicacion.length() > 195) {
                explicacion = explicacion.substring(0, 192) + "...";
            }

            JsonObject body = new JsonObject();
            body.addProperty("chat_id", TELEGRAM_CHAT_ID);
            body.addProperty("question", pregunta);
            body.add("options", opcionesSanitizadas);
            body.addProperty("type", "quiz");
            body.addProperty("correct_option_id", correctIndex);
            body.addProperty("explanation", explicacion);
            body.addProperty("is_anonymous", false);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int codigo = conn.getResponseCode();
            if (codigo == 200) {
                System.out.println("-> Quiz interactivo publicado exitosamente.");
            } else {
                System.out.println("Aviso enviando quiz. Código: " + codigo);
            }
        } catch (Exception e) {
            System.out.println("Error enviando quiz a Telegram: " + e.getMessage());
        }
    }

    private static void iniciarEscuchaTelegram() {
        while (true) {
            try {
                String urlApi = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/getUpdates?offset=" + (ultimoUpdateId + 1) + "&timeout=20";
                URL url = new URL(urlApi);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                if (conn.getResponseCode() == 200) {
                    try (InputStream is = conn.getInputStream()) {
                        String resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        JsonObject json = JsonParser.parseString(resp).getAsJsonObject();
                        if (json.get("ok").getAsBoolean()) {
                            JsonArray updates = json.getAsJsonArray("result");
                            for (JsonElement elem : updates) {
                                JsonObject upd = elem.getAsJsonObject();
                                ultimoUpdateId = upd.get("update_id").getAsLong();

                                if (upd.has("message")) {
                                    JsonObject msg = upd.getAsJsonObject("message");
                                    if (msg.has("text")) {
                                        String comando = msg.get("text").getAsString().trim().toLowerCase();
                                        if (comando.contains("pregunta") || comando.contains("quiz")) {
                                            enviarMensajeTelegram("⏳ *Generando caso situacional de 4 opciones alineado a tu cargo...*");
                                            generarYEnviarPregunta();
                                        } else if (comando.startsWith("/start") || comando.contains("hola") || comando.contains("ayuda")) {
                                            enviarMensajeTelegram("👋 ¡Hola! Soy tu Simulador DIAN 2676 en la Nube.\n\nEscribe *pregunta* (o */pregunta*) en el chat para recibir un nuevo caso de Juicio Situacional.");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {}
        }
    }
}
