package com.ronda.backend.publicacion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PublicacionDtos {
    private PublicacionDtos() { }

    public record CreateRequest(
            @NotBlank @Size(max = 120) String titulo,
            @NotBlank @Size(max = 4000) String descripcion,
            @NotBlank @Size(max = 80) String categoria,
            @NotNull @DecimalMin(value = "0.01") BigDecimal precio,
            @NotNull EstadoArticulo estadoArticulo,
            @NotBlank @Size(max = 255) String direccion,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitud,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitud,
            @Size(max = 80) String zona) { }

    public record ChangeStatusRequest(@NotNull EstadoPublicacion estado) { }

    public record Response(Long id, String titulo, String descripcion, String categoria,
                           BigDecimal precio, EstadoArticulo estadoArticulo,
                           EstadoPublicacion estadoPublicacion, String direccion,
                           BigDecimal latitud, BigDecimal longitud, Instant createdAt, List<String> fotos) {
        static Response from(Publicacion publicacion) {
            return new Response(publicacion.getId(), publicacion.getTitulo(), publicacion.getDescripcion(),
                    publicacion.getCategoria(), publicacion.getPrecio(), publicacion.getEstadoArticulo(),
                    publicacion.getEstadoPublicacion(), publicacion.getDireccion(), publicacion.getLatitud(),
                    publicacion.getLongitud(), publicacion.getCreatedAt(), publicacion.getFotos().stream()
                            .map(foto -> "/uploads/" + foto.getArchivo()).toList());
        }
    }

    /** Item del Home: no expone la dirección exacta ni las coordenadas del vendedor. */
    public record Resumen(Long id, String titulo, String categoria, BigDecimal precio,
                          EstadoArticulo estadoArticulo, EstadoPublicacion estadoPublicacion,
                          String zona, Instant createdAt, List<String> fotos, Integer distanciaKm) {
        static Resumen from(Publicacion publicacion, Integer distanciaKm) {
            return new Resumen(publicacion.getId(), publicacion.getTitulo(), publicacion.getCategoria(),
                    publicacion.getPrecio(), publicacion.getEstadoArticulo(), publicacion.getEstadoPublicacion(),
                    publicacion.getZona(), publicacion.getCreatedAt(), publicacion.getFotos().stream()
                            .map(foto -> "/uploads/" + foto.getArchivo()).toList(), distanciaKm);
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements,
                                  int totalPages, boolean last) {
        static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages(), page.isLast());
        }
    }
}
