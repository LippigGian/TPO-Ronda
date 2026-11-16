package com.ronda.backend.notificacion;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificacionPrecioTest {
    @Test
    void formateaPreciosConElFormatoArgentino() {
        assertThat(NotificacionService.precio(new BigDecimal("80000"))).isEqualTo("$ 80.000");
        assertThat(NotificacionService.precio(new BigDecimal("80000.00"))).isEqualTo("$ 80.000");
        assertThat(NotificacionService.precio(new BigDecimal("1234567.5"))).isEqualTo("$ 1.234.567,50");
        assertThat(NotificacionService.precio(new BigDecimal("999"))).isEqualTo("$ 999");
    }
}
