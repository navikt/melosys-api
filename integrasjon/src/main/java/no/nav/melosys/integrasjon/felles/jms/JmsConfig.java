package no.nav.melosys.integrasjon.felles.jms;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;

import com.ibm.mq.jms.MQQueue;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.compat.jms.internal.JMSC;
import no.nav.melosys.exception.IntegrasjonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.core.JmsTemplate;

/**
 * Konfigurasjon er tatt fra blant annet Foreldrepengers integrasjon mot IBM MQ:
 * http://stash.devillo.no/projects/VEDFP/repos/vl-fordel/browse/web/server/src/main/java/no/nav/foreldrepenger/fordel/web/server/JmsKonfig.java
*/
@Configuration
@EnableJms
public class JmsConfig {

    private static final Logger log = LoggerFactory.getLogger(JmsConfig.class);

    public static final String HENDELSESKØ = "hendelseskø";

    @Value("${queueManager.hostName}")
    private String hostName;

    @Value("${queueManager.port}")
    private int port;

    @Value("${queueManager.name}")
    private String queueManager;

    @Value("${queueManager.channel}")
    private String channel;

    @Value("${queueManager.useSsl}")
    private boolean useSsl;

    @Value("${SBEH.queueName}")
    private String sakBehKøNavn;

    @Bean(name = HENDELSESKØ)
    public Queue hendelseshåndterer() throws JMSException {
        return new MQQueue(sakBehKøNavn);
    }

    @Bean
    public JmsTemplate jmsTemplate() throws JMSException {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(mqQueueConnectionFactory());
        return template;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() throws IntegrasjonException {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        try {
            factory.setConnectionFactory(mqQueueConnectionFactory());
        } catch (JMSException e) {
            log.error("Påkobling mot kø-manager feilet", e);
            throw new IntegrasjonException("Påkobling mot kø-manager feilet", e);
        }
        return factory;
    }

    @Bean
    public ConnectionFactory mqQueueConnectionFactory() throws JMSException {
        MQQueueConnectionFactory connectionFactory = new MQQueueConnectionFactory();

        connectionFactory.setHostName(hostName);
        connectionFactory.setPort(port);
        connectionFactory.setQueueManager(queueManager);
        connectionFactory.setChannel(channel);
        connectionFactory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);

        if (useSsl) {
            // Denne trengs for at IBM MQ libs skal bruke/gjenkjenne samme ciphersuite navn
            // som Oracle JRE:
            // (Uten denne vil ikke IBM MQ libs gjenkjenne "TLS_RSA_WITH_AES_128_CBC_SHA")
            System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "false");

            connectionFactory.setSSLCipherSuite("TLS_RSA_WITH_AES_128_CBC_SHA");
        }

        UserCredentialsConnectionFactoryAdapter credentialQueueConnectionFactory = new UserCredentialsConnectionFactoryAdapter();
        credentialQueueConnectionFactory.setUsername("srvappserver");
        credentialQueueConnectionFactory.setPassword("");
        credentialQueueConnectionFactory.setTargetConnectionFactory(connectionFactory);

        return credentialQueueConnectionFactory;
    }
}
