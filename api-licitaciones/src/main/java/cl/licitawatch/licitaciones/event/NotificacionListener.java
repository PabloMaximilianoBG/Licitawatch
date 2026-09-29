package cl.licitawatch.licitaciones.event;

import cl.licitawatch.licitaciones.client.NotificacionClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Envía las notificaciones solo si la transacción se confirmó, de forma asíncrona (no bloquea al usuario). */
@Component
public class NotificacionListener {
    private final NotificacionClient client;

    public NotificacionListener(NotificacionClient client) {
        this.client = client;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificacion(NotificacionEvent e) {
        client.enviar(e.usuarioId(), e.tipo(), e.destinatario(), e.datos());
    }
}
