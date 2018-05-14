package no.nav.melosys.integrasjon.gsak.dokumentmottak;

import javax.jms.ConnectionFactory;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.MockQueue;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

@Configuration
public class DokumentmottakConfigTest {

    private final String DESTINASJON = "mock";

    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(dokumentmottakContainerFactoryTest());
        jmsTemplate.setDefaultDestinationName(DESTINASJON);
        return jmsTemplate;
    }

    @Bean
    public MockQueue mockQueue() {
        return destinationManager().createQueue(DESTINASJON);
    }

    @Bean
    public ConnectionFactory dokumentmottakContainerFactoryTest() {
        ConfigurationManager configurationManager = new ConfigurationManager();
        return new MockQueueConnectionFactory(destinationManager(), configurationManager);
    }

    private DestinationManager destinationManager() {
        return new DestinationManager();
    }
}
