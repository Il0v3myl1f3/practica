package md.mud.notificari.errors;

import org.springframework.http.HttpStatus;

/**
 * Erorile de cont si de administrare a utilizatorilor: login sau email deja luat,
 * parola gresita ori prea scurta.
 *
 * Inlocuieste perechile de exceptii generate de JHipster (cate una in {@code service}
 * si una aici, legate prin {@code ExceptionTranslator}) cu acelasi pattern folosit de
 * restul entitatilor: o singura clasa, cu fabrici statice.
 */
public final class UserException extends EntityErrorException {

    private static final String ENTITY_NAME = "userManagement";

    private UserException(HttpStatus status, String defaultMessage, String errorKey) {
        super(status, defaultMessage, ENTITY_NAME, errorKey);
    }

    public static UserException loginAlreadyUsed() {
        return new UserException(HttpStatus.BAD_REQUEST, "Acest nume de utilizator este deja folosit.", "userexists");
    }

    public static UserException emailAlreadyUsed() {
        return new UserException(HttpStatus.BAD_REQUEST, "Acest email este deja folosit.", "emailexists");
    }

    public static UserException invalidPassword() {
        return new UserException(HttpStatus.BAD_REQUEST, "Parola nu este corecta.", "invalidpassword");
    }
}
