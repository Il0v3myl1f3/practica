package md.mud.notificari.service.messaging.discord;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import md.mud.notificari.service.messaging.SendOutcome;
import org.junit.jupiter.api.Test;

/**
 * Formatarea Markdown si clasificarea erorilor - partea care se strica tacit
 * si nu se vede cu ochiul liber, ca la Telegram. JUnit simplu, fara Spring:
 * niciuna dintre cele doua clase nu face vreun apel de retea.
 */
class DiscordMessagingTest {

    // ------------------------------------------------------- DiscordMarkdownFormatter

    @Test
    void boldDevineAsteriscDublu() {
        assertThat(DiscordMarkdownFormatter.chunks("<b>x</b>")).containsExactly("**x**");
    }

    @Test
    void asteriscDinTextSeEscapeaza() {
        assertThat(DiscordMarkdownFormatter.chunks("a * b")).containsExactly("a \\* b");
    }

    @Test
    void textulLungSeTaieInBucatiDeCelMult2000() {
        String textLung = "a".repeat(4500);

        List<String> chunks = DiscordMarkdownFormatter.chunks(textLung);

        assertThat(chunks).hasSize(3);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(2000));
        assertThat(chunks.stream().mapToInt(String::length).sum()).isEqualTo(4500);
    }

    @Test
    void imaginileSuntAruncate() {
        assertThat(DiscordMarkdownFormatter.chunks("<img src=\"poza.png\">text")).containsExactly("text");
    }

    // ------------------------------------------------------- DiscordFailureClassifier

    @Test
    void limitaDeRataSeReincearcaLaMomentulCerutRotunjitInSus() {
        DiscordApiException e = new DiscordApiException(429, null, "You are being rate limited.", DiscordApiException.roundUp(1.5));

        SendOutcome outcome = DiscordFailureClassifier.classify(e);

        assertThat(outcome.status()).isEqualTo(SendOutcome.Status.RETRY);
        assertThat(outcome.retryAfter()).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void dmRefuzatCod50007EDefinitiv() {
        DiscordApiException e = new DiscordApiException(403, 50007, "Cannot send messages to this user", null);

        SendOutcome outcome = DiscordFailureClassifier.classify(e);

        assertThat(outcome.status()).isEqualTo(SendOutcome.Status.PERMANENT_FAILURE);
    }

    @Test
    void panaDeRetelSeReincearca() {
        SendOutcome outcome = DiscordFailureClassifier.classify(new IOException("connection refused"));

        assertThat(outcome.status()).isEqualTo(SendOutcome.Status.RETRY);
    }
}
