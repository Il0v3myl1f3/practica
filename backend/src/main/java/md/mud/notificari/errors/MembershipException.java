package md.mud.notificari.errors;

import org.springframework.http.HttpStatus;

public final class MembershipException extends EntityErrorException {

    private static final String ENTITY_NAME = "membership";

    private MembershipException(HttpStatus status, String defaultMessage, String errorKey) {
        super(status, defaultMessage, ENTITY_NAME, errorKey);
    }

    public static MembershipException notFound(Long id) {
        return new MembershipException(HttpStatus.NOT_FOUND, "Membership " + id + " was not found", "notfound");
    }
}
