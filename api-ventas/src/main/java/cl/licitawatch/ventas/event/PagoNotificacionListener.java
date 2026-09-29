package cl.licitawatch.ventas.event;

import cl.licitawatch.ventas.service.ComprobanteService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PagoNotificacionListener {
    private final ComprobanteService comprobanteService;

    public PagoNotificacionListener(ComprobanteService comprobanteService) {
        this.comprobanteService = comprobanteService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPagoProcesado(PagoProcesadoEvent e) {
        comprobanteService.notificarResultado(e.pagoId());
    }
}
