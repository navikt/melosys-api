package no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus;

import javax.jms.Queue;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

@Configuration
public class BehandlingstatusClientConfigTest {

    private DestinationManager destinationManager;

    public BehandlingstatusClientConfigTest() {
        destinationManager = new DestinationManager();
    }

    @Bean
    public JmsTemplate jmsTemplate() {
        return new JmsTemplate(connectionFactory());
    }

    @Bean
    public Queue hendelseshåndterer() {
        return destinationManager.createQueue("mock_queue");
    }

    @Bean
    public MockQueueConnectionFactory connectionFactory() {
        return new MockQueueConnectionFactory(destinationManager, new ConfigurationManager());
    }

}
