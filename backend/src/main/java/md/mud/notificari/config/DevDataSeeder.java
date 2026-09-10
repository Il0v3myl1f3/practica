package md.mud.notificari.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import md.mud.notificari.domain.Membership;
import md.mud.notificari.domain.Message;
import md.mud.notificari.domain.MessageChannel;
import md.mud.notificari.domain.MessageRecipient;
import md.mud.notificari.domain.MessageTemplate;
import md.mud.notificari.domain.Organization;
import md.mud.notificari.domain.Recipient;
import md.mud.notificari.domain.RecipientChannel;
import md.mud.notificari.domain.RecipientGroup;
import md.mud.notificari.domain.enumeration.Channel;
import md.mud.notificari.domain.enumeration.DeliveryStatus;
import md.mud.notificari.domain.enumeration.MemberRole;
import md.mud.notificari.domain.enumeration.MessageStatus;
import md.mud.notificari.repository.MembershipRepository;
import md.mud.notificari.repository.MessageChannelRepository;
import md.mud.notificari.repository.MessageRecipientRepository;
import md.mud.notificari.repository.MessageRepository;
import md.mud.notificari.repository.MessageTemplateRepository;
import md.mud.notificari.repository.OrganizationRepository;
import md.mud.notificari.repository.RecipientChannelRepository;
import md.mud.notificari.repository.RecipientGroupRepository;
import md.mud.notificari.repository.RecipientRepository;
import md.mud.notificari.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the development database with the data set from the UI prototype:
 * 34 recipients across 4 groups, 4 templates and a short send history.
 *
 * Runs once — it does nothing as soon as an organization exists. Replaces
 * JHipster's random faker data, which produced unreadable emails.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DevDataSeeder.class);

    // "Nume Prenume|utilizator|Grup" — ordinea conteaza: canalele se aleg dupa index,
    // exact ca in prototip, ca sa existe si destinatari fara Telegram sau WhatsApp.
    private static final String[] PEOPLE = {
        "Ana Rusu|ana.rusu|Marketing",
        "Victor Ciobanu|victor.ciobanu|Marketing",
        "Elena Postica|elena.postica|Marketing",
        "Dan Ursu|dan.ursu|Marketing",
        "Irina Bejan|irina.bejan|Marketing",
        "Mihai Cazacu|mihai.cazacu|Marketing",
        "Corina Lungu|corina.lungu|Marketing",
        "Radu Gisca|radu.gisca|Marketing",
        "Natalia Coroban|natalia.coroban|Vanzari",
        "Sergiu Balan|sergiu.balan|Vanzari",
        "Olga Damian|olga.damian|Vanzari",
        "Andrei Munteanu|andrei.munteanu|Vanzari",
        "Lilia Sirbu|lilia.sirbu|Vanzari",
        "Pavel Grosu|pavel.grosu|Vanzari",
        "Diana Croitoru|diana.croitoru|Vanzari",
        "Ion Zaharia|ion.zaharia|Vanzari",
        "Marina Chirilov|marina.chirilov|Vanzari",
        "Vasile Rotaru|vasile.rotaru|Vanzari",
        "Cristina Gutu|cristina.gutu|Suport",
        "Alexandru Vrabie|alexandru.vrabie|Suport",
        "Tatiana Melnic|tatiana.melnic|Suport",
        "Nicolae Bordei|nicolae.bordei|Suport",
        "Aliona Frunze|aliona.frunze|Suport",
        "Ghenadie Popa|ghenadie.popa|Suport",
        "Svetlana Doncila|svetlana.doncila|Suport",
        "Eugen Barbu|eugen.barbu|Suport",
        "Veronica Stratan|veronica.stratan|Suport",
        "Adrian Cojocaru|adrian.cojocaru|Suport",
        "Ludmila Verdes|ludmila.verdes|Administratie",
        "Grigore Onea|grigore.onea|Administratie",
        "Rodica Sendrea|rodica.sendrea|Administratie",
        "Petru Andronic|petru.andronic|Administratie",
        "Silvia Bulat|silvia.bulat|Administratie",
        "Valeriu Hincu|valeriu.hincu|Administratie",
    };

    private static final String[][] TEMPLATES = {
        {
            "Felicitare",
            "Mesaj de felicitare pentru aniversari, promovari sau realizari de echipa.",
            "Felicitari!",
            "Buna {{prenume}},<br><br>Iti transmitem felicitari din partea intregii echipe. Iti multumim pentru contributia ta si iti dorim mult succes in continuare!<br><br>Cu drag,<br>Echipa",
        },
        {
            "Anunt general",
            "Informare interna despre schimbari, decizii sau noutati operationale.",
            "Anunt important",
            "Buna {{prenume}},<br><br>Va informam ca [detaliile anuntului]. Modificarea intra in vigoare incepand cu [data].<br><br>Multumim,<br>Echipa",
        },
        {
            "Invitatie eveniment",
            "Invitatie cu data, ora si loc pentru un eveniment intern sau public.",
            "Invitatie: [numele evenimentului]",
            "Buna {{prenume}},<br><br>Te invitam la [numele evenimentului], care va avea loc pe [data], ora [ora], la [locatie].<br><br>Te asteptam!",
        },
        {
            "Reamintire",
            "Reamintire scurta despre un termen limita sau o actiune necesara.",
            "Reamintire: [subiect]",
            "Buna {{prenume}},<br><br>Iti reamintim ca termenul limita pentru [actiune] este [data].<br><br>Multumim,<br>Echipa",
        },
    };

    // subiect | canale | destinatari | zile in urma | status
    private static final String[] HISTORY = {
        "Anunt: program de sarbatori|EMAIL|34|8|SENT",
        "Invitatie: All-hands Q3|EMAIL,TELEGRAM|28|13|SENT",
        "Reamintire: raport saptamanal|TELEGRAM|18|16|SENT",
        "Felicitari, echipa Vanzari!|EMAIL,WHATSAPP|10|20|PARTIAL",
        "Mentenanta planificata|EMAIL|34|23|SENT",
        "Politica de acces revizuita|EMAIL|34|27|SENT",
        "Instruire: securitatea datelor|EMAIL,TELEGRAM|22|30|SENT",
        "Sedinta extraordinara Suport|WHATSAPP|10|34|PARTIAL",
        "Rezultate sondaj intern|EMAIL|34|38|SENT",
    };

    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final MembershipRepository memberships;
    private final RecipientGroupRepository groups;
    private final RecipientRepository recipients;
    private final RecipientChannelRepository recipientChannels;
    private final MessageTemplateRepository templates;
    private final MessageRepository messages;
    private final MessageChannelRepository messageChannels;
    private final MessageRecipientRepository deliveries;

    public DevDataSeeder(
        UserRepository users,
        OrganizationRepository organizations,
        MembershipRepository memberships,
        RecipientGroupRepository groups,
        RecipientRepository recipients,
        RecipientChannelRepository recipientChannels,
        MessageTemplateRepository templates,
        MessageRepository messages,
        MessageChannelRepository messageChannels,
        MessageRecipientRepository deliveries
    ) {
        this.users = users;
        this.organizations = organizations;
        this.memberships = memberships;
        this.groups = groups;
        this.recipients = recipients;
        this.recipientChannels = recipientChannels;
        this.templates = templates;
        this.messages = messages;
        this.messageChannels = messageChannels;
        this.deliveries = deliveries;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (organizations.count() > 0) {
            return;
        }
        Instant now = Instant.now();
        Organization org = organizations.save(new Organization().name("Notificari MUD").slug("mud").createdAt(now));
        users.findAll().forEach(u -> memberships.save(new Membership().user(u).organization(org).role(MemberRole.OWNER)));

        Map<String, RecipientGroup> groupByName = new LinkedHashMap<>();
        List<Recipient> people = new ArrayList<>();
        for (int i = 0; i < PEOPLE.length; i++) {
            String[] parts = PEOPLE[i].split("\\|");
            String[] name = parts[0].split(" ", 2);
            RecipientGroup group = groupByName.computeIfAbsent(parts[2], n -> groups.save(new RecipientGroup().name(n).organization(org)));
            Recipient r = recipients.save(
                new Recipient()
                    .firstName(name[0])
                    .lastName(name.length > 1 ? name[1] : "")
                    .email(parts[1] + "@exemplu.md")
                    .createdAt(now)
                    .organization(org)
                    .recipientGroup(group)
            );
            recipientChannels.save(
                new RecipientChannel().channel(Channel.EMAIL).address(r.getEmail()).active(true).verified(true).recipient(r)
            );
            if (i % 3 != 2) {
                recipientChannels.save(
                    new RecipientChannel().channel(Channel.TELEGRAM).address("tg-" + parts[1]).active(true).verified(false).recipient(r)
                );
            }
            if (i % 4 != 1) {
                recipientChannels.save(
                    new RecipientChannel()
                        .channel(Channel.WHATSAPP)
                        .address("+3736" + String.format("%06d", 100000 + i))
                        .active(true)
                        .verified(false)
                        .recipient(r)
                );
            }
            people.add(r);
        }

        for (String[] t : TEMPLATES) {
            templates.save(
                new MessageTemplate().name(t[0]).description(t[1]).subject(t[2]).body(t[3]).organization(org)
            );
        }

        for (String row : HISTORY) {
            String[] parts = row.split("\\|");
            int count = Integer.parseInt(parts[2]);
            Instant when = now.minus(Long.parseLong(parts[3]), ChronoUnit.DAYS);
            MessageStatus status = MessageStatus.valueOf(parts[4]);
            Message m = messages.save(
                new Message()
                    .subject(parts[0])
                    .bodyHtml("Buna {{prenume}},<br><br>" + parts[0] + ".<br><br>Echipa")
                    .status(status)
                    .recipientCount(count)
                    .createdAt(when)
                    .sentAt(when)
                    .organization(org)
            );
            List<Channel> chans = new ArrayList<>();
            for (String c : parts[1].split(",")) {
                Channel ch = Channel.valueOf(c);
                chans.add(ch);
                messageChannels.save(new MessageChannel().channel(ch).message(m));
            }
            for (int i = 0; i < Math.min(count, people.size()); i++) {
                Recipient r = people.get(i);
                for (Channel ch : chans) {
                    // Un "Partial" trebuie sa aiba si esecuri, altfel starea nu s-ar putea reconstitui.
                    boolean failed = status == MessageStatus.PARTIAL && i % 5 == 0;
                    deliveries.save(
                        new MessageRecipient()
                            .message(m)
                            .recipient(r)
                            .channel(ch)
                            .status(failed ? DeliveryStatus.FAILED : DeliveryStatus.DELIVERED)
                            .errorMessage(failed ? "Destinatarul nu are canalul configurat." : null)
                            .sentAt(when)
                            .deliveredAt(failed ? null : when)
                    );
                }
            }
        }
        LOG.info("Seed: {} destinatari, {} grupuri, {} sabloane, {} trimiteri", people.size(), groupByName.size(), TEMPLATES.length, HISTORY.length);
    }
}
