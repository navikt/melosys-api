package no.nav.melosys.integrasjon.gsak.behandlesak;

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
import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.BehandleSakV1;

@Component
public class BehandleSakConsumerConfig {
    private static final String BEHANDLE_SAK_V_1_WSDL = "wsdl/no/nav/tjeneste/virksomhet/behandleSak/v1/Binding.wsdl";
    private static final String BEHANDLE_SAK_V1_NAMESPACE = "http://nav.no/tjeneste/virksomhet/behandleSak/v1/Binding";
    private static final QName BEHANDLE_SAK_V_1_SERVICE = new QName(BEHANDLE_SAK_V1_NAMESPACE, "BehandleSak_v1");
    private static final QName BEHANDLE_SAK_V_1_PORT = new QName(BEHANDLE_SAK_V1_NAMESPACE, "BehandleSak_v1Port");

    private String endpointUrl;

    public BehandleSakConsumerConfig() {
    }

    public BehandleSakConsumerConfig(@Value("BehandleSak_v1.url") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    BehandleSakV1 getPort() {
        Map<String, Object> properties = new HashMap<>();
        // FIXME: Brukes kun ifm mock'en og MÅ fjernes når den har blitt JBoss-ifisert
        properties.put(SecurityConstants.MUST_UNDERSTAND, false);

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(BEHANDLE_SAK_V_1_WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(BEHANDLE_SAK_V_1_SERVICE);
        factoryBean.setEndpointName(BEHANDLE_SAK_V_1_PORT);
        factoryBean.setServiceClass(BehandleSakV1.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(BehandleSakV1.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
