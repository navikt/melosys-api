package no.nav.melosys.integrasjon.tps.person;

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
import no.nav.tjeneste.virksomhet.person.v2.binding.PersonV2;

@Component
public class PersonConsumerConfig {
    private static final String PERSON_V2_WSDL = "wsdl/no/nav/tjeneste/virksomhet/person/v2/Binding.wsdl";
    private static final String PERSON_V2_NAMESPACE = "http://nav.no/tjeneste/virksomhet/person/v2/Binding";
    private static final QName PERSON_V2_SERVICE = new QName(PERSON_V2_NAMESPACE, "Person_v2");
    private static final QName PERSON_V2_PORT = new QName(PERSON_V2_NAMESPACE, "Person_v2Port");

    private String endpointUrl; // NOSONAR

    public PersonConsumerConfig(@Value("Person_v2.url") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    PersonV2 getPort() {
        Map<String, Object> properties = new HashMap<>();
        // FIXME: Brukes kun ifm mock'en og MÅ fjernes når den har blitt JBoss-ifisert
        properties.put(SecurityConstants.MUST_UNDERSTAND, false);

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(PERSON_V2_WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(PERSON_V2_SERVICE);
        factoryBean.setEndpointName(PERSON_V2_PORT);
        factoryBean.setServiceClass(PersonV2.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(PersonV2.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
