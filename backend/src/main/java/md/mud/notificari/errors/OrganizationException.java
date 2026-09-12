package md.mud.notificari.errors;

import org.springframework.http.HttpStatus;

public final class OrganizationException extends EntityErrorException {

    private static final String ENTITY_NAME = "organization";

    private OrganizationException(HttpStatus status, String defaultMessage, String errorKey) {
        super(status, defaultMessage, ENTITY_NAME, errorKey);
    }

    public static OrganizationException notFound(Long id) {
        return new OrganizationException(HttpStatus.NOT_FOUND, "Organization " + id + " was not found", "notfound");
    }
}
