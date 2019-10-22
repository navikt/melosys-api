package no.nav.melosys.integrasjon.tps.aktoer;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import no.nav.melosys.integrasjon.felles.mdc.CallIdOutInterceptor;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AktorConsumerConfig {
    private static final String AKTOER_V_2_WSDL = "wsdl/no/nav/tjeneste/virksomhet/aktoer/v2/Binding.wsdl";
    private static final String AKTOER_V_2_NAMESPACE = "http://nav.no/tjeneste/virksomhet/aktoer/v2/Binding/";
    private static final QName AKTOER_V_2_SERVICE = new QName(AKTOER_V_2_NAMESPACE, "Aktoer");
    private static final QName AKTOER_V_2_PORT = new QName(AKTOER_V_2_NAMESPACE, "Aktoer_v2Port");

    private String endpointUrl;

    public AktorConsumerConfig(@Value("${Aktoer_v2.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    AktoerV2 getPort() {
        Map<String, Object> properties = new HashMap<>();

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(AKTOER_V_2_WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(AKTOER_V_2_SERVICE);
        factoryBean.setEndpointName(AKTOER_V_2_PORT);
        factoryBean.setServiceClass(AktoerV2.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(AktoerV2.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
