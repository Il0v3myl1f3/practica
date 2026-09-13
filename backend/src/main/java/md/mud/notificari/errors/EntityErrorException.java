package md.mud.notificari.errors;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause.ProblemDetailWithCauseBuilder;

/**
 * Base for per-entity error exceptions: unlike {@link BadRequestAlertException} (always 400),
 * subclasses pick their own {@link HttpStatus} (e.g. 404 for not-found, 400 for a business rule).
 */
public abstract class EntityErrorException extends ErrorResponseException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String entityName;

    private final String errorKey;

    protected EntityErrorException(HttpStatus status, String defaultMessage, String entityName, String errorKey) {
        super(
            status,
            ProblemDetailWithCauseBuilder.instance()
                .withStatus(status.value())
                .withType(ErrorConstants.DEFAULT_TYPE)
                .withTitle(defaultMessage)
                // Si in "detail", fiindca ExceptionTranslator rescrie "title" cu
                // motivul standard HTTP ("Conflict"). "detail" il lasa in pace daca
                // e deja setat, deci asta e singurul loc in care mesajul in romana
                // ajunge la client - altfel utilizatorul vede cheia "error.xxx".
                .withDetail(defaultMessage)
                .withProperty("message", "error." + errorKey)
                .withProperty("params", entityName)
                .build(),
            null
        );
        this.entityName = entityName;
        this.errorKey = errorKey;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getErrorKey() {
        return errorKey;
    }

    public ProblemDetailWithCause getProblemDetailWithCause() {
        return (ProblemDetailWithCause) this.getBody();
    }
}
