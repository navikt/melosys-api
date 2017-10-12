package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import no.nav.melosys.integrasjon.felles.mdc.CallIdOutInterceptor;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3;

@Component
public class ArbeidsforholdConsumerConfig {
    private static final String WSDL = "wsdl/no/nav/tjeneste/virksomhet/arbeidsforhold/v3/Binding.wsdl";
    private static final String NAMESPACE = "http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/Binding";
    private static final QName SERVICE = new QName(NAMESPACE, "Arbeidsforhold_v3");
    private static final QName PORT = new QName(NAMESPACE, "Arbeidsforhold_v3Port");

    private String endpointUrl; // NOSONAR

    public ArbeidsforholdConsumerConfig(@Value("${Arbeidsforhold_v3.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    ArbeidsforholdV3 getPort() {
        Map<String, Object> properties = new HashMap<>();

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(SERVICE);
        factoryBean.setEndpointName(PORT);
        factoryBean.setServiceClass(ArbeidsforholdV3.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(ArbeidsforholdV3.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

}
