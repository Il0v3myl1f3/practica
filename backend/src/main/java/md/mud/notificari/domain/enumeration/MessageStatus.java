package md.mud.notificari.domain.enumeration;

/**
 * DRAFT = ciorna. PARTIAL/FAILED se calculeaza din MessageRecipient.
 */
public enum MessageStatus {
    DRAFT,
    QUEUED,
    SENT,
    PARTIAL,
    FAILED,
}
