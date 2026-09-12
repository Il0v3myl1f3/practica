package md.mud.notificari.service.messaging.dispatch;

import md.mud.notificari.domain.enumeration.Channel;
import md.mud.notificari.service.messaging.Delivery;

/**
 * O livrare gata de plecare, complet detasata de sesiunea Hibernate.
 *
 * Trebuie sa fie complet materializata: pasul urmator e I/O de retea, in afara
 * oricarei tranzactii, unde un lazy load ar arunca.
 */
public record DeliveryJob(Long deliveryId, Long messageId, Channel channel, int attempt, Delivery delivery) {}
