package no.nav.melosys.integrasjon.eessi;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class MelosysEessiRequestInterceptor implements ClientHttpRequestInterceptor {

    private final String apiKeyHeader;
    private final String apiKeyValue;

    public MelosysEessiRequestInterceptor(String apiKeyHeader, String apiKeyValue) {
        this.apiKeyHeader = apiKeyHeader;
        this.apiKeyValue = apiKeyValue;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        HttpHeaders headers = httpRequest.getHeaders();
        headers.add(apiKeyHeader, apiKeyValue);
        return clientHttpRequestExecution.execute(httpRequest, bytes);

    }
}
