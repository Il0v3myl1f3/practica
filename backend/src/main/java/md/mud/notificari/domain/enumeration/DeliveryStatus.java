package md.mud.notificari.domain.enumeration;

/**
 * Ciclul de viata al unei livrari (un rand MessageRecipient):
 *
 * <pre>
 *   PENDING --claim--> SENDING --+--> SENT --(webhook, candva)--> DELIVERED
 *      ^                         |
 *      +---- retry, mai are ----+--> FAILED
 *
 *   SKIPPED = scris la punerea in coada, cand destinatarul nu are adresa pe canal.
 * </pre>
 *
 * SENT e succesul terminal cat timp nu avem webhook-uri. DELIVERED e rezervat
 * confirmarii de la provider si e scris de istoricul de dinaintea cozii, deci
 * orice citire trebuie sa trateze ambele ca succes - vezi {@link #isSuccess()}.
 */
public enum DeliveryStatus {
    PENDING,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    SKIPPED;

    /** Livrarea asteapta dispecerul: fie e in coada, fie e deja luata in lucru. */
    public boolean isInFlight() {
        return this == PENDING || this == SENDING;
    }

    /** Providerul a acceptat mesajul. DELIVERED e inclus pentru randurile de dinaintea cozii. */
    public boolean isSuccess() {
        return this == SENT || this == DELIVERED;
    }
}
