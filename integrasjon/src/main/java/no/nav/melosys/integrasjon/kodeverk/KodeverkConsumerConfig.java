package no.nav.melosys.integrasjon.kodeverk;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import no.nav.melosys.integrasjon.felles.CallIdOutInterceptor;
import no.nav.tjeneste.virksomhet.kodeverk.v2.KodeverkPortType;

@Component()
public class KodeverkConsumerConfig {

    private static final String KODEVERK_V2_WSDL = "wsdl/no/nav/tjeneste/virksomhet/kodeverk/v2/Kodeverk.wsdl";
    private static final String KODEVERK_V2_NAMESPACE = "http://nav.no/tjeneste/virksomhet/kodeverk/v2/";
    private static final QName KODEVERK_V2_SERVICE = new QName(KODEVERK_V2_NAMESPACE, "Kodeverk_v2");
    private static final QName KODEVERK_V2_PORT = new QName(KODEVERK_V2_NAMESPACE, "Kodeverk_v2");

    private String endpointUrl;

    public KodeverkConsumerConfig(@Value("${koveverk_v2.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    KodeverkPortType getPort() {
        Map<String, Object> properties = new HashMap<>();

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setProperties(properties);
        factoryBean.setWsdlURL(KODEVERK_V2_WSDL);
        factoryBean.setServiceName(KODEVERK_V2_SERVICE);
        factoryBean.setEndpointName(KODEVERK_V2_PORT);
        factoryBean.setServiceClass(KodeverkPortType.class);
        factoryBean.setAddress(getEndpointUrl());
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(KodeverkPortType.class);
    }

    String getEndpointUrl() {
        return endpointUrl;
    }

}
