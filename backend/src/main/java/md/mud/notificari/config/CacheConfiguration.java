package md.mud.notificari.config;

import java.time.Duration;
import org.ehcache.config.builders.*;
import org.ehcache.jsr107.Eh107Configuration;
import org.hibernate.cache.jcache.ConfigSettings;
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private final javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration;

    public CacheConfiguration(JHipsterProperties jHipsterProperties) {
        var ehcache = jHipsterProperties.getCache().getEhcache();

        jcacheConfiguration = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Object.class,
                Object.class,
                ResourcePoolsBuilder.heap(ehcache.getMaxEntries())
            )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ehcache.getTimeToLiveSeconds())))
                .build()
        );
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(javax.cache.CacheManager cacheManager) {
        return hibernateProperties -> hibernateProperties.put(ConfigSettings.CACHE_MANAGER, cacheManager);
    }

    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer() {
        return cm -> {
            createCache(cm, md.mud.notificari.repository.UserRepository.USERS_BY_LOGIN_CACHE);
            createCache(cm, md.mud.notificari.repository.UserRepository.USERS_BY_EMAIL_CACHE);
            createCache(cm, md.mud.notificari.domain.User.class.getName());
            createCache(cm, md.mud.notificari.domain.Authority.class.getName());
            createCache(cm, md.mud.notificari.domain.User.class.getName() + ".authorities");
            createCache(cm, md.mud.notificari.domain.Organization.class.getName());
            createCache(cm, md.mud.notificari.domain.Membership.class.getName());
            createCache(cm, md.mud.notificari.domain.RecipientGroup.class.getName());
            createCache(cm, md.mud.notificari.domain.Recipient.class.getName());
            createCache(cm, md.mud.notificari.domain.Recipient.class.getName() + ".channelses");
            createCache(cm, md.mud.notificari.domain.RecipientChannel.class.getName());
            createCache(cm, md.mud.notificari.domain.MessageTemplate.class.getName());
            createCache(cm, md.mud.notificari.domain.Message.class.getName());
            createCache(cm, md.mud.notificari.domain.Message.class.getName() + ".channelses");
            createCache(cm, md.mud.notificari.domain.Message.class.getName() + ".attachmentses");
            createCache(cm, md.mud.notificari.domain.MessageChannel.class.getName());
            createCache(cm, md.mud.notificari.domain.MessageAttachment.class.getName());
            // MessageRecipient si Message.deliverieses nu sunt cache-uite: tabelul e coada
            // de trimitere, scrisa cu UPDATE-uri in masa care ocolesc cache-ul de nivel 2.
            // Daca reintroduci @Cache pe entitate, adauga si liniile de aici - altfel
            // Hibernate pica la pornire cu "cache not found".
            // jhipster-needle-ehcache-add-entry
        };
    }

    private void createCache(javax.cache.CacheManager cm, String cacheName) {
        javax.cache.Cache<Object, Object> cache = cm.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        } else {
            cm.createCache(cacheName, jcacheConfiguration);
        }
    }
}
