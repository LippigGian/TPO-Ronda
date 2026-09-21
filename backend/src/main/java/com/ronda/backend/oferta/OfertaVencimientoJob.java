package com.ronda.backend.oferta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Caducidad automatica de ofertas: cada minuto (configurable) pasa a VENCIDA
 * toda oferta pendiente cuyo plazo de vigencia ya se cumplio.
 */
@Component
public class OfertaVencimientoJob {
    private static final Logger log = LoggerFactory.getLogger(OfertaVencimientoJob.class);

    private final OfertaService service;

    public OfertaVencimientoJob(OfertaService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.ofertas.revision-vencimiento-ms:60000}")
    public void vencerOfertasExpiradas() {
        int vencidas = service.vencerOfertasExpiradas();
        if (vencidas > 0) {
            log.info("Ofertas vencidas automáticamente: {}", vencidas);
        }
    }
}
