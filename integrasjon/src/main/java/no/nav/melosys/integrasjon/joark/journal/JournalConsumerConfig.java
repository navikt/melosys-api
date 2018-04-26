package no.nav.melosys.integrasjon.joark.journal;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import no.nav.melosys.integrasjon.felles.mdc.CallIdOutInterceptor;
import no.nav.tjeneste.virksomhet.journal.v3.JournalV3;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JournalConsumerConfig {
    private static final String WSDL = "wsdl/no/nav/tjeneste/virksomhet/journal/v3/Binding.wsdl";
    private static final String NAMESPACE = "http://nav.no/tjeneste/virksomhet/journal/v3/Binding";
    private static final QName SERVICE = new QName(NAMESPACE, "Journal_v3");
    private static final QName PORT = new QName(NAMESPACE, "Journal_v3Port");

    @Value("${Journal_v3.url}")
    private String endpointUrl; // NOSONAR

    JournalV3 getPort() {
        Map<String, Object> properties = new HashMap<>();

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(SERVICE);
        factoryBean.setEndpointName(PORT);
        factoryBean.setServiceClass(JournalV3.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(JournalV3.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
