package md.mud.notificari.service.messaging;

import java.time.Duration;

/**
 * Rezultatul unei incercari de livrare. Verdictul e un enum, nu un boolean in
 * plus: (esec, dar reincercabil) si (reusit, dar reincercabil) nu inseamna
 * nimic, deci compilatorul nu trebuie sa le permita.
 *
 * {@code retryAfter} e pauza ceruta explicit de provider (Telegram o trimite in
 * {@code parameters.retry_after} la 429). Cand exista, dispecerul o respecta in
 * locul backoff-ului propriu, daca e mai lunga: o pauza de flood poate cere o ora,
 * iar reincercarile noastre din 30 in 30 de secunde ar consuma toate incercarile
 * pe refuzuri.
 */
public record SendOutcome(Status status, String providerMessageId, String error, Duration retryAfter) {
    public enum Status {
        /** Providerul a acceptat mesajul. */
        OK,
        /** Esec trecator - merita reincercat dupa backoff. */
        RETRY,
        /** Esec determinist: parola gresita, adresa invalida, bounce dur. Reincercarea nu ajuta. */
        PERMANENT_FAILURE,
    }

    public static SendOutcome ok(String providerMessageId) {
        return new SendOutcome(Status.OK, providerMessageId, null, null);
    }

    public static SendOutcome retry(String error) {
        return new SendOutcome(Status.RETRY, null, error, null);
    }

    /** Reincercare la momentul cerut de provider, nu la cel calculat de noi. */
    public static SendOutcome retry(String error, Duration retryAfter) {
        return new SendOutcome(Status.RETRY, null, error, retryAfter);
    }

    public static SendOutcome permanent(String error) {
        return new SendOutcome(Status.PERMANENT_FAILURE, null, error, null);
    }

    public boolean ok() {
        return status == Status.OK;
    }
}
