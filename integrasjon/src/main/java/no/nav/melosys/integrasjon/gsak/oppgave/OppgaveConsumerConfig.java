package no.nav.melosys.integrasjon.gsak.oppgave;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OppgaveConsumerConfig {

    private final String endpointUrl;

    @Autowired
    public OppgaveConsumerConfig(@Value("${OppgaveAPI_v1.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
