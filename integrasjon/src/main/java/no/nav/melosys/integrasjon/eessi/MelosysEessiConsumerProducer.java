package no.nav.melosys.integrasjon.eessi;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class MelosysEessiConsumerProducer {

    private final String url;
    private final String apiKeyHeader;
    private final String apiKeyValue;
    private final Environment environment;

    @Autowired
    public MelosysEessiConsumerProducer(Environment environment,
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
        RestTemplateBuilder builder = new RestTemplateBuilder()
            .rootUri(url);

        String[] aktiveProfiler = environment.getActiveProfiles();
        if (Arrays.asList(aktiveProfiler).contains("nais")) {
            builder.interceptors(new MelosysEessiRequestInterceptor(apiKeyHeader, apiKeyValue));
        }
        return new EessiConsumerImpl(builder.build());
    }
}
