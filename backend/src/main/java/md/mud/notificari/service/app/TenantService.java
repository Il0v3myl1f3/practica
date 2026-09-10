package md.mud.notificari.service.app;

import java.time.Instant;
import java.util.Locale;
import md.mud.notificari.domain.Membership;
import md.mud.notificari.domain.Organization;
import md.mud.notificari.domain.User;
import md.mud.notificari.domain.enumeration.MemberRole;
import md.mud.notificari.repository.MembershipRepository;
import md.mud.notificari.repository.app.AppMembershipRepository;
import md.mud.notificari.repository.OrganizationRepository;
import md.mud.notificari.repository.UserRepository;
import md.mud.notificari.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the organization the current user works in.
 *
 * Multi-tenancy is invisible in the UI: every user belongs to exactly one
 * organization, created on first use. All app queries are scoped through here.
 */
@Service
@Transactional
public class TenantService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final AppMembershipRepository appMembershipRepository;
    private final OrganizationRepository organizationRepository;

    public TenantService(
        UserRepository userRepository,
        MembershipRepository membershipRepository,
        AppMembershipRepository appMembershipRepository,
        OrganizationRepository organizationRepository
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.appMembershipRepository = appMembershipRepository;
        this.organizationRepository = organizationRepository;
    }

    public User currentUser() {
        return SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .orElseThrow(() -> new IllegalStateException("Utilizatorul curent nu a putut fi identificat."));
    }

    @Transactional(readOnly = true)
    public Organization currentOrganization() {
        User user = currentUser();
        return appMembershipRepository.findFirstForUser(user.getId())
            .map(Membership::getOrganization)
            .orElseGet(() -> createDefaultFor(user));
    }

    private Organization createDefaultFor(User user) {
        String label = user.getLogin();
        Organization org = new Organization()
            .name("Organizația " + label)
            .slug(slugify(label) + "-" + System.currentTimeMillis())
            .createdAt(Instant.now());
        org = organizationRepository.save(org);
        membershipRepository.save(new Membership().user(user).organization(org).role(MemberRole.OWNER));
        return org;
    }

    private String slugify(String value) {
        String slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "org" : slug;
    }
}
