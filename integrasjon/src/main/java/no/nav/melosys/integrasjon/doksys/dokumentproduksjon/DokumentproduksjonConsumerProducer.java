package no.nav.melosys.integrasjon.doksys.dokumentproduksjon;

import no.nav.melosys.integrasjon.reststs.RestSTSService;
import no.nav.melosys.integrasjon.reststs.SecurityTokenServiceSTSClient;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.attachment.reference.ReferenceResolver;
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.neethi.Policy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DokumentproduksjonConsumerProducer {

    private final DokumentproduksjonConsumerConfig config;
    private final RestSTSService restSTSService;
    private final String stsPolicy;


    public DokumentproduksjonConsumerProducer(DokumentproduksjonConsumerConfig config, RestSTSService restSTSService, @Value("${stsPolicy.url}") String stsPolicy) {
        this.config = config;
        this.restSTSService = restSTSService;
        this.stsPolicy = stsPolicy;
    }

    @Bean
    @ConditionalOnProperty(name="dokumentproduksjon.uten.token", havingValue = "false", matchIfMissing = true)
    public DokumentproduksjonConsumer dokumentproduksjonConsumer() {
        DokumentproduksjonV3 port = config.getPort();
        Client client = ClientProxy.getClient(port);

        configureClient(client);

        return new DokumentproduksjonConsumerImpl(port);
    }

    @Bean
    @ConditionalOnProperty(name="dokumentproduksjon.uten.token", havingValue="true")
    public DokumentproduksjonConsumer dokumentproduksjonConsumerForLocalAndTesting() {
        DokumentproduksjonV3 port = config.getPort();

        return new DokumentproduksjonConsumerImpl(port);
    }

    private void configureClient(Client client) {

        STSClient stsClient = new SecurityTokenServiceSTSClient(client.getBus(), restSTSService);
        client.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);
        client.getRequestContext().put(SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, false);
        setEndpointPolicyReference(client, stsPolicy);
    }

    private void setEndpointPolicyReference(Client client, String uri) {
        Policy policy = resolvePolicyReference(client, uri);
        if (policy == null) {
            throw new IllegalStateException("Failed to resolve policy reference: " + uri);
        }
        setClientEndpointPolicy(client, policy);
    }

    private Policy resolvePolicyReference(Client client, String uri) {
        PolicyBuilder policyBuilder = client.getBus().getExtension(PolicyBuilder.class);
        ReferenceResolver resolver = new RemoteReferenceResolver("", policyBuilder);
        return resolver.resolveReference(uri);
    }

    private void setClientEndpointPolicy(Client client, Policy policy) {
        Endpoint endpoint = client.getEndpoint();
        EndpointInfo endpointInfo = endpoint.getEndpointInfo();

        PolicyEngine policyEngine = client.getBus().getExtension(PolicyEngine.class);
        SoapMessage message = new SoapMessage(Soap12.getInstance());
        EndpointPolicy endpointPolicy = policyEngine.getClientEndpointPolicy(endpointInfo, null, message);
        policyEngine.setClientEndpointPolicy(endpointInfo, endpointPolicy.updatePolicy(policy, message));
    }
}
