package no.nav.melosys.integrasjon.medl.behandle;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import no.nav.melosys.integrasjon.felles.mdc.CallIdOutInterceptor;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.BehandleMedlemskapV2;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BehandleMedlemskapConsumerConfig {
    private static final String WSDL = "wsdl/no/nav/tjeneste/virksomhet/behandlemedlemskap/v2/BehandleMedlemskapV2.wsdl";
    private static final String NAMESPACE = "http://nav.no/tjeneste/virksomhet/behandlemedlemskap/v2";
    private static final QName SERVICE = new QName(NAMESPACE, "BehandleMedlemskap_v2");
    private static final QName PORT = new QName(NAMESPACE, "BehandleMedlemskap_v2Port");

    @Value("${BehandleMedlemskap_v2.url}")
    private String endpointUrl; // NOSONAR

    BehandleMedlemskapV2 getPort() {
        Map<String, Object> properties = new HashMap<>();

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(SERVICE);
        factoryBean.setEndpointName(PORT);
        factoryBean.setServiceClass(BehandleMedlemskapV2.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(BehandleMedlemskapV2.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
