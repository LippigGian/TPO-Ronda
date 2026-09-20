package com.ronda.backend.oferta;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita las tareas programadas (@Scheduled) que usa OfertaVencimientoJob. */
@Configuration
@EnableScheduling
public class OfertaSchedulingConfig {
}
