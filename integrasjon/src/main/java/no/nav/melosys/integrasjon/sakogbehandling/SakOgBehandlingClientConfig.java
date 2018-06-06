package no.nav.melosys.integrasjon.sakogbehandling;

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
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.core.JmsTemplate;

@Configuration
public class SakOgBehandlingClientConfig {

    private static final Logger log = LoggerFactory.getLogger(SakOgBehandlingClientConfig.class);

    @Value("${mqGateway02.hostName}")
    private String hostName;

    @Value("${mqGateway02.port}")
    private int port;

    @Value("${mqGateway02.name}")
    private String queueManager;

    @Value("${mqGateway02.channel}")
    private String channel;

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
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(mqQueueConnectionFactory());
        return template;
    }

    @Bean
    public Queue hendelseshåndterer() throws JMSException {
        return new MQQueue(queueName);
    }

    @Bean
    public ConnectionFactory mqQueueConnectionFactory() throws JMSException {
        MQQueueConnectionFactory connectionFactory = new MQQueueConnectionFactory();

        connectionFactory.setHostName(hostName);
        connectionFactory.setPort(port);
        connectionFactory.setQueueManager(queueManager);
        connectionFactory.setChannel(channel);
        connectionFactory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);

        UserCredentialsConnectionFactoryAdapter credentialQueueConnectionFactory = new UserCredentialsConnectionFactoryAdapter();
        credentialQueueConnectionFactory.setUsername("srvappserver");
        credentialQueueConnectionFactory.setPassword("");
        credentialQueueConnectionFactory.setTargetConnectionFactory(connectionFactory);

        return credentialQueueConnectionFactory;
    }
}
