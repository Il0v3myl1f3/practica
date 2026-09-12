package md.mud.notificari.repository.app;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import md.mud.notificari.domain.MessageRecipient;
import md.mud.notificari.domain.enumeration.Channel;
import md.mud.notificari.domain.enumeration.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Tabelul e si coada de trimitere. Interogarile de mai jos sunt cele pe care le
 * foloseste dispecerul.
 *
 * Peste tot, {@code :now} vine ca {@code Instant} din Java, niciodata ca
 * {@code now()} din SQL: coloanele sunt {@code timestamp without time zone} cu
 * UTC inauntru (hibernate.jdbc.time_zone: UTC), iar {@code now()} ar intoarce
 * ora locala a serverului - coada ar merge pe un laptop UTC si ar sta blocata
 * in productie.
 */
public interface AppMessageRecipientRepository extends JpaRepository<MessageRecipient, Long> {
    @Query("select distinct mr from MessageRecipient mr left join fetch mr.recipient where mr.message.id = :messageId")
    List<MessageRecipient> findAllForMessage(Long messageId);

    /** Decide daca un destinatar mai poate fi sters sau doar arhivat. */
    boolean existsByRecipientId(Long recipientId);

    @Query(
        """
        select mr from MessageRecipient mr
          join fetch mr.recipient r
          join fetch mr.message m
         where mr.id = :id
        """
    )
    Optional<MessageRecipient> findForDispatch(@Param("id") Long id);

    /** Canalele care au ceva de trimis chiar acum - un singur query ieftin per tick. */
    @Query(
        """
        select distinct mr.channel from MessageRecipient mr
         where mr.status in :inFlight and mr.nextAttemptAt <= :now
        """
    )
    List<Channel> channelsWithClaimable(@Param("inFlight") List<DeliveryStatus> inFlight, @Param("now") Instant now);

    /**
     * Rezerva un lot de livrari pentru firul curent.
     *
     * {@code for update skip locked} e specific PostgreSQL (H2 nu il parseaza),
     * de aceea e folosit doar de dispecerul asincron; calea inline are varianta
     * JPQL de mai jos. Predicatul include si SENDING: un rand ramas blocat de la
     * un crash redevine vizibil dupa visibility-timeout si se reia singur.
     */
    @Query(
        value = """
        select mr.id from message_recipient mr
         where mr.channel = :channel
           and mr.status in ('PENDING', 'SENDING')
           and mr.next_attempt_at <= :now
         order by mr.next_attempt_at, mr.id
         limit :batch
         for update skip locked
        """,
        nativeQuery = true
    )
    List<Long> selectClaimable(@Param("channel") String channel, @Param("now") Instant now, @Param("batch") int batch);

    /** Varianta pentru modul sincron: un singur mesaj, un singur fir, deci fara lock. */
    @Query(
        """
        select mr.id from MessageRecipient mr
         where mr.message.id = :messageId
           and mr.status in :inFlight
           and mr.nextAttemptAt <= :now
         order by mr.nextAttemptAt, mr.id
        """
    )
    List<Long> selectClaimableForMessage(
        @Param("messageId") Long messageId,
        @Param("inFlight") List<DeliveryStatus> inFlight,
        @Param("now") Instant now
    );

    /**
     * Marcheaza lotul ca fiind in lucru. {@code nextAttemptAt} devine termenul de
     * vizibilitate: daca nu se termina pana atunci, randul e reluat.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update MessageRecipient mr
           set mr.status = :sending,
               mr.attemptCount = mr.attemptCount + 1,
               mr.lastAttemptAt = :now,
               mr.nextAttemptAt = :visibleAgainAt
         where mr.id in :ids
        """
    )
    int markSending(
        @Param("ids") List<Long> ids,
        @Param("sending") DeliveryStatus sending,
        @Param("now") Instant now,
        @Param("visibleAgainAt") Instant visibleAgainAt
    );

    /** Statusurile livrarilor unui mesaj; agregarea se face in Java, sunt cel mult cateva sute. */
    @Query("select mr.status from MessageRecipient mr where mr.message.id = :messageId")
    List<DeliveryStatus> statusesForMessage(@Param("messageId") Long messageId);

    @Query(
        """
        select count(distinct mr.recipient.id) from MessageRecipient mr
         where mr.message.id = :messageId and mr.status in :successful
        """
    )
    long countReachedRecipients(@Param("messageId") Long messageId, @Param("successful") List<DeliveryStatus> successful);

    @Query("select max(mr.sentAt) from MessageRecipient mr where mr.message.id = :messageId")
    Instant lastSentAt(@Param("messageId") Long messageId);
}
