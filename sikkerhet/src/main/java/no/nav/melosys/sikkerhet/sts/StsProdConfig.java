package no.nav.melosys.sikkerhet.sts;

import java.util.HashMap;
import javax.xml.namespace.QName;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.attachment.reference.ReferenceResolver;
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.neethi.Policy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"!test & !local-mock"})
public class StsProdConfig implements StsConfig {

    private final StsLoginConfig login;

    public StsProdConfig(StsLoginConfig stsLoginConfig) {
        this.login = stsLoginConfig;
    }

    @Override
    public <T> T wrapWithSts(T port, StsClientType samlTokenType) {
        Client client = ClientProxy.getClient(port);
        switch (samlTokenType) {
            case SECURITYCONTEXT_TIL_SAML -> configureStsForOnBehalfOfWithOidc(client);
            case SYSTEM_SAML -> configureStsForSystemUser(client);
            default -> throw new IllegalArgumentException("Unknown enum value: " + samlTokenType);
        }
        return port;
    }

    private void configureStsForOnBehalfOfWithOidc(Client client) {
        STSClient stsClient = createBasicSTSClient(StsClientType.SECURITYCONTEXT_TIL_SAML, client.getBus());
        stsClient.setOnBehalfOf(new OnBehalfOfWithOidcCallbackHandler());
        client.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);
        client.getRequestContext().put(SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, false);
        setEndpointPolicyReference(client, login.getStsPolicy());
    }

    private void configureStsForSystemUser(Client client) {
        new WSAddressingFeature().initialize(client, client.getBus());

        STSClient stsClient = createBasicSTSClient(StsClientType.SYSTEM_SAML, client.getBus());
        client.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);
        setEndpointPolicyReference(client, login.getStsPolicy());
    }

    private STSClient createBasicSTSClient(StsClientType type, Bus bus) {
        STSClient stsClient = new NAVSTSClient(bus, type);
        stsClient.setWsdlLocation("wsdl/ws-trust-1.4-service.wsdl");
        stsClient.setServiceQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl", "SecurityTokenServiceProvider"));
        stsClient.setEndpointQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl", "SecurityTokenServiceSOAP"));
        stsClient.setEnableAppliesTo(false);
        stsClient.setAllowRenewing(false);

        try {
            // Endpoint must be set on clients request context
            // as the wrapping requestcontext is not available
            // when creating the client from WSDL (ref cxf-users mailinglist)
            stsClient.getClient().getRequestContext().put(Message.ENDPOINT_ADDRESS, login.getSecurityTokenServiceUrl());
        } catch (BusException | EndpointException e) {
            throw new IllegalStateException("Failed to set endpoint address of STSClient", e);
        }

        stsClient.getOutInterceptors().add(new LoggingOutInterceptor());
        stsClient.getInInterceptors().add(new LoggingInInterceptor());

        HashMap<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, login.getUsername());
        properties.put(SecurityConstants.PASSWORD, login.getPassword());
        stsClient.setProperties(properties);
        return stsClient;
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
