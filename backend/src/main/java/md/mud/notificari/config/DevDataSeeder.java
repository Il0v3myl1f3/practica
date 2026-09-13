package md.mud.notificari.config;

import java.time.Instant;
import md.mud.notificari.domain.Membership;
import md.mud.notificari.domain.MessageTemplate;
import md.mud.notificari.domain.Organization;
import md.mud.notificari.domain.enumeration.MemberRole;
import md.mud.notificari.repository.MembershipRepository;
import md.mud.notificari.repository.MessageTemplateRepository;
import md.mud.notificari.repository.OrganizationRepository;
import md.mud.notificari.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pregateste baza de dezvoltare cu strictul necesar: organizatia, membership-urile
 * si cele 4 sabloane.
 *
 * <b>Nu creeaza destinatari si nici grupuri.</b> Lista de destinatari ramane goala,
 * ca sa poti adauga manual sau importa din CSV exact pe cine vrei sa testezi — fara
 * sa trimiti, din greseala, catre adrese inventate. Grupurile le creezi tu, cand ai
 * nevoie de ele (la importul CSV se creeaza oricum automat). Din acelasi motiv nu
 * creeaza nici istoric de trimiteri: fara destinatari, trimiterile ar fi catre
 * nimeni, cu zero livrari.
 *
 * Ruleaza o singura data — nu face nimic de indata ce exista o organizatie.
 * Inlocuieste datele aleatoare de la faker, care produceau emailuri ilizibile.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DevDataSeeder.class);

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

    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final MembershipRepository memberships;
    private final MessageTemplateRepository templates;

    public DevDataSeeder(
        UserRepository users,
        OrganizationRepository organizations,
        MembershipRepository memberships,
        MessageTemplateRepository templates
    ) {
        this.users = users;
        this.organizations = organizations;
        this.memberships = memberships;
        this.templates = templates;
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

        for (String[] t : TEMPLATES) {
            templates.save(new MessageTemplate().name(t[0]).description(t[1]).subject(t[2]).body(t[3]).organization(org));
        }

        LOG.info("Seed: 0 destinatari, 0 grupuri, {} sabloane, 0 trimiteri", TEMPLATES.length);
    }
}
