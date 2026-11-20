package com.example.ronda.recepcion;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * Contenido del QR físico de entrega. El QR no es texto simple sino un JSON serializado como string:
 *
 * <pre>{"tipo": "RONDA-ENTREGA", "publicacionId": 12, "titulo": "Bicicleta rodado 26"}</pre>
 *
 * La cámara (ML Kit) devuelve el string crudo, {@link #parsear(String)} lo convierte en un objeto
 * (el equivalente a JSON.parse()) y con esos datos la app resuelve la confirmación de la recepción.
 */
public final class QrEntrega {
    public static final String TIPO = "RONDA-ENTREGA";

    private final long publicacionId;
    private final String titulo;

    private QrEntrega(long publicacionId, String titulo) {
        this.publicacionId = publicacionId;
        this.titulo = titulo;
    }

    public long getPublicacionId() {
        return publicacionId;
    }

    /** Puede ser null: el título es informativo, lo que identifica la entrega es el publicacionId. */
    public String getTitulo() {
        return titulo;
    }

    /**
     * Parsea el string crudo leído por la cámara.
     *
     * @throws QrInvalidoException si no es JSON, no es de Ronda o le falta un publicacionId válido.
     */
    public static QrEntrega parsear(String crudo) throws QrInvalidoException {
        if (crudo == null || crudo.trim().isEmpty()) {
            throw new QrInvalidoException("El QR está vacío.");
        }

        JsonObject json;
        try {
            JsonElement elemento = JsonParser.parseString(crudo.trim());
            if (!elemento.isJsonObject()) {
                throw new QrInvalidoException("No es un QR de entrega de Ronda.");
            }
            json = elemento.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException error) {
            throw new QrInvalidoException("No es un QR de entrega de Ronda.");
        }

        if (!TIPO.equals(texto(json, "tipo"))) {
            throw new QrInvalidoException("No es un QR de entrega de Ronda.");
        }

        Long publicacionId = entero(json, "publicacionId");
        if (publicacionId == null || publicacionId <= 0) {
            throw new QrInvalidoException("El QR no indica una publicación válida.");
        }

        return new QrEntrega(publicacionId, texto(json, "titulo"));
    }

    /** JSON que tiene que contener el QR de entrega de una publicación (lo imprime el vendedor). */
    public static String contenido(long publicacionId, String titulo) {
        JsonObject json = new JsonObject();
        json.addProperty("tipo", TIPO);
        json.addProperty("publicacionId", publicacionId);
        json.addProperty("titulo", titulo);
        return json.toString();
    }

    private static String texto(JsonObject json, String campo) {
        JsonElement valor = json.get(campo);
        return valor != null && valor.isJsonPrimitive() && valor.getAsJsonPrimitive().isString()
                ? valor.getAsString()
                : null;
    }

    /** Solo números enteros: "12" (string) o 12.5 no se aceptan como id. */
    private static Long entero(JsonObject json, String campo) {
        JsonElement valor = json.get(campo);
        if (valor == null || !valor.isJsonPrimitive() || !valor.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        JsonPrimitive numero = valor.getAsJsonPrimitive();
        try {
            return numero.getAsBigDecimal().longValueExact();
        } catch (ArithmeticException noEsEntero) {
            return null;
        }
    }

    /** QR leído correctamente por la cámara, pero que no es un QR de entrega de Ronda. */
    public static final class QrInvalidoException extends Exception {
        public QrInvalidoException(String mensaje) {
            super(mensaje);
        }
    }
}
