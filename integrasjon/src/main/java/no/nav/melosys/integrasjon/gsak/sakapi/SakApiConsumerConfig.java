package no.nav.melosys.integrasjon.gsak.sakapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SakApiConsumerConfig {

    private String endpointUrl;

    @Autowired
    public SakApiConsumerConfig(@Value("${SakAPI_v1.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
