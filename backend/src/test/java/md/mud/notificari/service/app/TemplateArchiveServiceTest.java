package md.mud.notificari.service.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import md.mud.notificari.service.app.AppDtos.TemplateView;
import org.junit.jupiter.api.Test;

/**
 * Dus-intorsul sabloane -> ZIP -> sabloane.
 *
 * Singurul test scris de mana din proiect: arhivarea si citirea HTML-ului sunt
 * partea in care se pierd tacut date (diacritice, ghilimele, nume de fisier),
 * si nu se vede cu ochiul liber cand se strica.
 */
class TemplateArchiveServiceTest {

    private final TemplateArchiveService service = new TemplateArchiveService();

    @Test
    void pastreazaTotLaDusIntors() {
        List<TemplateView> original = List.of(
            new TemplateView(
                1L,
                "Felicitare",
                "Pentru aniversari & promovari",
                "Felicitari!",
                "Buna <span data-var=\"prenume\">{{prenume}}</span>,<br><br>Felicitari!"
            ),
            new TemplateView(2L, "Anunt \"important\"", "Cu ghilimele", "Anunt <b>important</b>", "<p>Text si &amp; entitati.</p>")
        );

        List<TemplateView> back = service.read(new ByteArrayInputStream(service.export(original)));

        assertThat(back).hasSize(2);
        assertThat(back.get(0).name()).isEqualTo("Felicitare");
        assertThat(back.get(0).subject()).isEqualTo("Felicitari!");
        assertThat(back.get(0).description()).isEqualTo("Pentru aniversari & promovari");
        assertThat(back.get(0).body()).contains("data-var=\"prenume\"").contains("{{prenume}}");
        assertThat(back.get(1).subject()).isEqualTo("Anunt <b>important</b>");
        assertThat(back.get(1).body()).contains("&amp;");
    }

    @Test
    void numeleCuCaractereInterziseInFisierSeIntoarceIntact() {
        // "Reamintire: raport" e un nume plauzibil, dar ":" nu poate sta intr-un
        // nume de fisier — de asta numele adevarat calatoreste intr-un <meta>.
        List<TemplateView> back = service.read(
            new ByteArrayInputStream(service.export(List.of(new TemplateView(1L, "Reamintire: raport", "", "", "Corp."))))
        );

        assertThat(back.get(0).name()).isEqualTo("Reamintire: raport");
        assertThat(back.get(0).subject()).isEqualTo("Reamintire: raport");
    }

    @Test
    void doiSabloaneCuAcelasiNumeNuSeSuprascriuInArhiva() {
        byte[] zip = service.export(
            List.of(new TemplateView(1L, "Acelasi", "", "A", "unu"), new TemplateView(2L, "Acelasi", "", "B", "doi"))
        );

        List<TemplateView> back = service.read(new ByteArrayInputStream(zip));

        assertThat(back).hasSize(2);
        assertThat(back.get(1).subject()).isEqualTo("B");
        assertThat(back.get(1).body()).isEqualTo("doi");
    }

    @Test
    void fisierFaraBodyEsteTratatCaFragment() {
        List<TemplateView> back = service.read(new ByteArrayInputStream(zipOf("Manual.html", "<p>Doar un fragment.</p>")));

        assertThat(back.get(0).name()).isEqualTo("Manual");
        assertThat(back.get(0).body()).isEqualTo("<p>Doar un fragment.</p>");
    }

    @Test
    void arhivaFaraHtmlSauFisierStricatSuntRespinse() {
        assertThatThrownBy(() -> service.read(new ByteArrayInputStream(zipOf("citeste-ma.txt", "nimic"))))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.read(new ByteArrayInputStream("nu sunt o arhiva".getBytes(StandardCharsets.UTF_8))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] zipOf(String name, String content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }
}
