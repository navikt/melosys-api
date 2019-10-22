package no.nav.melosys.integrasjon.inntk.inntekt;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import no.nav.melosys.integrasjon.felles.mdc.CallIdOutInterceptor;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InntektConsumerConfig {
    private static final String WSDL = "wsdl/no/nav/tjeneste/virksomhet/inntekt/v3/Binding.wsdl";
    private static final String NAMESPACE = "http://nav.no/tjeneste/virksomhet/inntekt/v3/Binding";
    private static final QName SERVICE = new QName(NAMESPACE, "Inntekt_v3");
    private static final QName PORT = new QName(NAMESPACE, "Inntekt_v3Port");

    private String endpointUrl; // NOSONAR

    public InntektConsumerConfig(@Value("${Inntekt_v3.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    InntektV3 getPort() {
        Map<String, Object> properties = new HashMap<>();

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(SERVICE);
        factoryBean.setEndpointName(PORT);
        factoryBean.setServiceClass(InntektV3.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(InntektV3.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
