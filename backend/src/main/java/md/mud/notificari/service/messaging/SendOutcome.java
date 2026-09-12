package md.mud.notificari.service.messaging;

/**
 * Rezultatul unei incercari de livrare. Verdictul e un enum, nu un boolean in
 * plus: (esec, dar reincercabil) si (reusit, dar reincercabil) nu inseamna
 * nimic, deci compilatorul nu trebuie sa le permita.
 */
public record SendOutcome(Status status, String providerMessageId, String error) {
    public enum Status {
        /** Providerul a acceptat mesajul. */
        OK,
        /** Esec trecator - merita reincercat dupa backoff. */
        RETRY,
        /** Esec determinist: parola gresita, adresa invalida, bounce dur. Reincercarea nu ajuta. */
        PERMANENT_FAILURE,
    }

    public static SendOutcome ok(String providerMessageId) {
        return new SendOutcome(Status.OK, providerMessageId, null);
    }

    public static SendOutcome retry(String error) {
        return new SendOutcome(Status.RETRY, null, error);
    }

    public static SendOutcome permanent(String error) {
        return new SendOutcome(Status.PERMANENT_FAILURE, null, error);
    }

    public boolean ok() {
        return status == Status.OK;
    }
}
