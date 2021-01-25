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

public class StsConfigurationUtil {

    private static final String STS_URL_KEY = "securityTokenService.url";
    private static final String STS_USER_USERNAME = "systemuser.username";
    @SuppressWarnings("squid:S2068")
    private static final String STS_USER_PASSWORD = "systemuser.password";
    private static final String SPRING_ACTIVE_PROFILES = "spring.profiles.active";

    private StsConfigurationUtil() {
        throw new IllegalAccessError("Skal ikke instansieres");
    }

    public static <T> T wrapWithSts(T port, NAVSTSClient.StsClientType samlTokenType) {

        //Ignorer sts-kall ved mock-kjøring
        final String aktivProfil = System.getProperty(SPRING_ACTIVE_PROFILES);
        if (aktivProfil != null && aktivProfil.equals("local-mock")) {
            return port;
        }

        Client client = ClientProxy.getClient(port);
        switch (samlTokenType) {
            case SECURITYCONTEXT_TIL_SAML:
                configureStsForOnBehalfOfWithOidc(client);
                break;
            case SYSTEM_SAML:
                configureStsForSystemUser(client);
                break;
            default:
                throw new IllegalArgumentException("Unknown enum value: " + samlTokenType);
        }
        return port;
    }

    private static void configureStsForOnBehalfOfWithOidc(Client client) {
        String location = requireProperty(STS_URL_KEY);
        String username = requireProperty(STS_USER_USERNAME);
        String password = requireProperty(STS_USER_PASSWORD);

        STSClient stsClient = createBasicSTSClient(StsClientType.SECURITYCONTEXT_TIL_SAML, client.getBus(), location, username, password);
        stsClient.setOnBehalfOf(new OnBehalfOfWithOidcCallbackHandler());
        client.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);
        client.getRequestContext().put(SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT, false);
        setEndpointPolicyReference(client, "classpath:stsPolicy.xml");
    }

    private static void configureStsForSystemUser(Client client) {
        String location = requireProperty(STS_URL_KEY);
        String username = requireProperty(STS_USER_USERNAME);
        String password = requireProperty(STS_USER_PASSWORD);

        new WSAddressingFeature().initialize(client, client.getBus());

        STSClient stsClient = createBasicSTSClient(StsClientType.SYSTEM_SAML, client.getBus(), location, username, password);
        client.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);
        setEndpointPolicyReference(client, "classpath:stsPolicy.xml");
    }

    private static String requireProperty(String key) {
        String property = System.getProperty(key);
        if (property == null) {
            throw new IllegalStateException("Required property " + key + " not available.");
        }
        return property;
    }

    private static STSClient createBasicSTSClient(StsClientType type, Bus bus, String location, String username, String password) {
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
            stsClient.getClient().getRequestContext().put(Message.ENDPOINT_ADDRESS, location);
        } catch (BusException | EndpointException e) {
            throw new IllegalStateException("Failed to set endpoint address of STSClient", e);
        }

        stsClient.getOutInterceptors().add(new LoggingOutInterceptor());
        stsClient.getInInterceptors().add(new LoggingInInterceptor());

        HashMap<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, username);
        properties.put(SecurityConstants.PASSWORD, password);
        stsClient.setProperties(properties);
        return stsClient;
    }

    private static void setEndpointPolicyReference(Client client, String uri) {
        Policy policy = resolvePolicyReference(client, uri);
        setClientEndpointPolicy(client, policy);
    }

    private static Policy resolvePolicyReference(Client client, String uri) {
        PolicyBuilder policyBuilder = client.getBus().getExtension(PolicyBuilder.class);
        ReferenceResolver resolver = new RemoteReferenceResolver("", policyBuilder);
        return resolver.resolveReference(uri);
    }

    private static void setClientEndpointPolicy(Client client, Policy policy) {
        Endpoint endpoint = client.getEndpoint();
        EndpointInfo endpointInfo = endpoint.getEndpointInfo();

        PolicyEngine policyEngine = client.getBus().getExtension(PolicyEngine.class);
        SoapMessage message = new SoapMessage(Soap12.getInstance());
        EndpointPolicy endpointPolicy = policyEngine.getClientEndpointPolicy(endpointInfo, null, message);
        policyEngine.setClientEndpointPolicy(endpointInfo, endpointPolicy.updatePolicy(policy, message));
    }

}
