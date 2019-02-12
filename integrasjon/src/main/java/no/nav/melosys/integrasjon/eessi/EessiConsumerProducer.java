package no.nav.melosys.integrasjon.eessi;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EessiConsumerProducer {

    private final String url;
    private final String apiKeyHeader;
    private final String apiKeyValue;
    private final Environment environment;

    private final static String NAIS_PROFIL = "nais";

    @Autowired
    public EessiConsumerProducer(Environment environment,
                                 @Value("${MelosysEessi.url}") String url,
                                 @Value("${MelosysEessi.apiHeader:}") String apiKeyHeader,
                                 @Value("${MelosysEessi.apiKey:}") String apiKeyValue) {
        this.environment = environment;
        this.url = url;
        this.apiKeyHeader = apiKeyHeader;
        this.apiKeyValue = apiKeyValue;
    }

    @Bean
    public EessiConsumer melosysEessiConsumer() {
        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(url).build();

        String[] aktiveProfiler = environment.getActiveProfiles();
        if (Arrays.asList(aktiveProfiler).contains(NAIS_PROFIL)) {
            restTemplate.setInterceptors(Collections.singletonList(new EessiRequestInterceptor(apiKeyHeader, apiKeyValue)));
        }
        return new EessiConsumerImpl(restTemplate);
    }
}
