package cl.licitawatch.licitaciones.service;

import cl.licitawatch.licitaciones.entity.EstadoLicitacion;
import cl.licitawatch.licitaciones.repository.LicitacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Cierra automáticamente las licitaciones PUBLICADAS cuya fecha de cierre ya pasó. */
@Component
public class CierreAutomaticoScheduler {
    private static final Logger log = LoggerFactory.getLogger(CierreAutomaticoScheduler.class);
    private final LicitacionRepository repository;

    public CierreAutomaticoScheduler(LicitacionRepository repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 5 * * * *")
    @Transactional
    public void cerrarVencidas() {
        int n = repository.cerrarVencidas(EstadoLicitacion.PUBLICADA, EstadoLicitacion.CERRADA, LocalDate.now());
        if (n > 0) {
            log.info("{} licitaciones cerradas automáticamente por fecha de cierre", n);
        }
    }
}
