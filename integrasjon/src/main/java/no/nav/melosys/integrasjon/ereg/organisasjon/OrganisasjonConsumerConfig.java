package no.nav.melosys.integrasjon.ereg.organisasjon;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import no.nav.melosys.integrasjon.felles.CallIdOutInterceptor;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4;

@Component
public class OrganisasjonConsumerConfig {
    private static final String WSDL = "wsdl/no/nav/tjeneste/virksomhet/organisasjon/v4/Binding.wsdl";
    private static final String NAMESPACE = "http://nav.no/tjeneste/virksomhet/organisasjon/v4/Binding";
    private static final QName SERVICE = new QName(NAMESPACE, "Organisasjon_v4");
    private static final QName PORT = new QName(NAMESPACE, "Organisasjon_v4Port");

    private String endpointUrl; // NOSONAR

    public OrganisasjonConsumerConfig(@Value("${Organisasjon_v4.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
    
    OrganisasjonV4 getPort() {
        Map<String, Object> properties = new HashMap<>();

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(SERVICE);
        factoryBean.setEndpointName(PORT);
        factoryBean.setServiceClass(OrganisasjonV4.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(OrganisasjonV4.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
