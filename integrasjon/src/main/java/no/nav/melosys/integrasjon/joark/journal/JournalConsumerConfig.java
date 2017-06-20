package no.nav.melosys.integrasjon.joark.journal;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.security.SecurityConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import no.nav.melosys.integrasjon.felles.CallIdOutInterceptor;
import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2;

@Component
public class JournalConsumerConfig {
    private static final String WSDL = "wsdl/no/nav/tjeneste/virksomhet/journal/v2/Binding.wsdl";
    private static final String NAMESPACE = "http://nav.no/tjeneste/virksomhet/journal/v2/Binding";
    private static final QName SERVICE = new QName(NAMESPACE, "Journal_v2");
    private static final QName PORT = new QName(NAMESPACE, "Journal_v2Port");

    @Value("${Journal_v2.url}")
    private String endpointUrl; // NOSONAR

    JournalV2 getPort() {
        Map<String, Object> properties = new HashMap<>();
        // FIXME: Brukes kun ifm mock'en og MÅ fjernes når den har blitt JBoss-ifisert
        properties.put(SecurityConstants.MUST_UNDERSTAND, false);

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(SERVICE);
        factoryBean.setEndpointName(PORT);
        factoryBean.setServiceClass(JournalV2.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(JournalV2.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
