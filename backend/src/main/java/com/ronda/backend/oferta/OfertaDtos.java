package com.ronda.backend.oferta;

import com.ronda.backend.publicacion.EstadoPublicacion;
import com.ronda.backend.publicacion.Publicacion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Contratos JSON de ofertas. Mismo estilo que PublicacionDtos y PerfilDtos. */
public final class OfertaDtos {
    private OfertaDtos() { }

    /** Propuesta de precio: se usa tanto para la oferta inicial como para las contraofertas. */
    public record PropuestaRequest(
            @NotNull(message = "Ingresá un precio")
            @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
            @Digits(integer = 10, fraction = 2, message = "El precio no es válido")
            BigDecimal precio,
            @Size(max = 280, message = "El mensaje puede tener hasta 280 caracteres")
            String mensaje) { }

    public record PublicacionResumen(Long id, String titulo, BigDecimal precioPublicado, String foto,
                                     EstadoPublicacion estadoPublicacion) {
        static PublicacionResumen from(Publicacion publicacion) {
            String foto = publicacion.getFotos().isEmpty()
                    ? null
                    : "/uploads/" + publicacion.getFotos().get(0).getArchivo();
            return new PublicacionResumen(publicacion.getId(), publicacion.getTitulo(), publicacion.getPrecio(),
                    foto, publicacion.getEstadoPublicacion());
        }
    }

    /** La otra persona de la negociacion, vista desde quien consulta. */
    public record Contraparte(Long id, String nombre) { }

    /** Punto de entrega: solo se envia cuando la oferta esta ACEPTADA. */
    public record Entrega(String direccion, BigDecimal latitud, BigDecimal longitud) {
        static Entrega from(Publicacion publicacion) {
            return new Entrega(publicacion.getDireccion(), publicacion.getLatitud(), publicacion.getLongitud());
        }
    }

    /**
     * Una oferta vista por uno de sus participantes.
     * miRol y puedoResponder dependen de quien consulta, asi la app no tiene que calcularlos.
     */
    public record Response(Long id, PublicacionResumen publicacion, ParteOferta miRol, Contraparte contraparte,
                           BigDecimal precio, String mensaje, EstadoOferta estado, ParteOferta turno,
                           boolean puedoResponder, Instant venceAt, Instant createdAt, Instant updatedAt,
                           Entrega entrega) {
        static Response from(Oferta oferta, ParteOferta miRol, Contraparte contraparte) {
            Entrega entrega = oferta.getEstado() == EstadoOferta.ACEPTADA
                    ? Entrega.from(oferta.getPublicacion())
                    : null;
            return new Response(oferta.getId(), PublicacionResumen.from(oferta.getPublicacion()), miRol, contraparte,
                    oferta.getPrecio(), oferta.getMensaje(), oferta.getEstado(), oferta.getTurno(),
                    oferta.puedeResponder(miRol), oferta.getVenceAt(), oferta.getCreatedAt(), oferta.getUpdatedAt(),
                    entrega);
        }
    }

    /** "Mis ofertas": enviadas y recibidas en una sola respuesta. */
    public record MisOfertasResponse(List<Response> enviadas, List<Response> recibidas,
                                     long pendientesDeMiRespuesta) { }
}
