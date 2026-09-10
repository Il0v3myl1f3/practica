package md.mud.notificari.service.messaging;

public record SendOutcome(boolean ok, String providerMessageId, String error) {
    public static SendOutcome ok(String id) {
        return new SendOutcome(true, id, null);
    }

    public static SendOutcome failed(String error) {
        return new SendOutcome(false, null, error);
    }
}
