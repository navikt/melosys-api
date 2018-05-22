package no.nav.melosys.service.dokumentmottak;

import javax.jms.JMSException;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.compat.jms.internal.JMSC;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * Konfigurasjon er tatt fra Foreldrepengers integrasjon mot IBM MQ:
 * http://stash.devillo.no/projects/VEDFP/repos/vl-fordel/browse/web/server/src/main/java/no/nav/foreldrepenger/fordel/web/server/JmsKonfig.java
*/
@Profile("!local")
@Configuration
@EnableJms
public class DokumentmottakConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(DokumentmottakConsumerConfig.class);

    @Value("${mqGateway.hostName}")
    private String hostName;

    @Value("${mqGateway.port}")
    private int port;

    @Value("${mqGateway.channel}")
    private String channel;

    @Value("${mqGateway.useSsl}")
    private boolean useSsl;

    @Value("${DokMot.queueManager}")
    private String queueManager;

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        try {
            factory.setConnectionFactory(mqQueueConnectionFactory());
        } catch (JMSException e) {
            log.error("Konfigurasjon av DokMot-kø feilet", e);
            throw new IntegrasjonException("Konfigurasjon av DokMot-kø feilet", e);
        }
        return factory;
    }

    @Bean
    public MQQueueConnectionFactory mqQueueConnectionFactory() throws JMSException {

        final MQQueueConnectionFactory factory = new MQQueueConnectionFactory();

        factory.setHostName(hostName);
        factory.setPort(port);
        if (channel != null) {
            factory.setChannel(channel);
        }
        factory.setQueueManager(queueManager);
        factory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);

        if (useSsl) {
            // Denne trengs for at IBM MQ libs skal bruke/gjenkjenne samme ciphersuite navn
            // som Oracle JRE:
            // (Uten denne vil ikke IBM MQ libs gjenkjenne "TLS_RSA_WITH_AES_128_CBC_SHA")
            System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "false");

            factory.setSSLCipherSuite("TLS_RSA_WITH_AES_128_CBC_SHA");
        }

        return factory;
    }
}
