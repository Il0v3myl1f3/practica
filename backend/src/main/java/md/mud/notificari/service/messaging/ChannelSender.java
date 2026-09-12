package md.mud.notificari.service.messaging;

import md.mud.notificari.domain.enumeration.Channel;

/**
 * Un provider de livrare pentru un canal. Pot exista mai multe implementari
 * pentru acelasi canal (de exemplu "smtp" si "mock" pentru EMAIL); alegerea se
 * face din configuratie, la pornire, de catre {@link ChannelSenderRegistry}.
 *
 * Un client real de Telegram sau WhatsApp inseamna o clasa noua care implementeaza
 * interfata asta plus doua linii in yaml. Nimic din AppService sau din dispecer
 * nu trebuie atins.
 */
public interface ChannelSender {
    Channel channel();

    /** Id stabil, folosit in {@code application.messaging.channels.<canal>.provider}. */
    String providerId();

    SendOutcome send(Delivery delivery);

    /**
     * Chemat o data la pornire, doar pentru providerul ales pentru canal. Daca
     * arunca, aplicatia nu porneste - preferam o eroare clara la boot in locul
     * unei trimiteri care esueaza abia in productie.
     */
    default void validateConfiguration() {
        // implicit nu e nimic de verificat
    }
}
