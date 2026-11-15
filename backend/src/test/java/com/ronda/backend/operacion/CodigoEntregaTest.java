package com.ronda.backend.operacion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodigoEntregaTest {
    @Test
    void reconoceElFormatoDelQrDeEntrega() {
        assertThat(CodigoEntrega.publicacionId("RONDA-ENTREGA-12")).contains(12L);
    }

    @Test
    void toleraEspaciosYMinusculasQueAgreganAlgunosGeneradores() {
        assertThat(CodigoEntrega.publicacionId("  ronda-entrega-7\n")).contains(7L);
    }

    @Test
    void rechazaTextosQueNoSonUnQrDeRonda() {
        assertThat(CodigoEntrega.publicacionId(null)).isEmpty();
        assertThat(CodigoEntrega.publicacionId("")).isEmpty();
        assertThat(CodigoEntrega.publicacionId("https://ronda.com")).isEmpty();
        assertThat(CodigoEntrega.publicacionId("RONDA-ENTREGA-")).isEmpty();
        assertThat(CodigoEntrega.publicacionId("RONDA-ENTREGA-12-extra")).isEmpty();
        assertThat(CodigoEntrega.publicacionId("RONDA-ENTREGA-99999999999999999999")).isEmpty();
    }
}
