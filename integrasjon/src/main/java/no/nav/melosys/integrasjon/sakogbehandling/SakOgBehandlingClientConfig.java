package no.nav.melosys.integrasjon.sakogbehandling;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.naming.*;

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
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;

@Profile("!mocking") //FIXME MELOSYS-1034
@Configuration
public class SakOgBehandlingClientConfig {

    private static final Logger log = LoggerFactory.getLogger(SakOgBehandlingClientConfig.class);

    @Value("${mqGateway.hostName}")
    private String hostName;

    @Value("${mqGateway.port}")
    private int port;

    @Value("${mqGateway.name}")
    private String queueManager;

    @Value("${SBEH.queueManager}")
    private String queueName;

    @Bean
    public SakOgBehandlingClient sakOgBehandlingClient() {
        try {
            return new SakOgBehandlingClientImpl(jmsTemplate(), hendelseshåndterer());
        } catch (JMSException e) {
            log.error("Konfigurasjon av SBEH-kø feilet", e);
            throw new IntegrasjonException("Konfigurasjon av SBEH-kø feilet", e);
        }
    }

    @Bean
    public JmsTemplate jmsTemplate() throws JMSException {
        JmsMessagingTemplate template = new JmsMessagingTemplate();
        template.setConnectionFactory(mqQueueConnectionFactory());
        return template.getJmsTemplate();
    }

    @Bean
    public Queue hendelseshåndterer() throws JMSException {
        return new MQQueue(queueName);
    }

    @Bean
    public MQQueueConnectionFactory mqQueueConnectionFactory() throws JMSException {
        final MQQueueConnectionFactory factory = new MQQueueConnectionFactory();

        factory.setHostName(hostName);
        factory.setPort(port);
        factory.setQueueManager(queueManager);
        factory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);

        return factory;
    }
}