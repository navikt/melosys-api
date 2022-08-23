package no.nav.melosys.integrasjon.doksys.dokumentproduksjon;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingSOAPInterceptor;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DokumentproduksjonConsumerConfig {
    private static final String WSDL = "wsdl/no/nav/tjeneste/virksomhet/dokumentproduksjon/v3/Binding.wsdl";
    private static final String NAMESPACE = "http://nav.no/tjeneste/virksomhet/dokumentproduksjon/v3/Binding";
    private static final QName SERVICE = new QName(NAMESPACE, "Dokumentproduksjon_v3");
    private static final QName PORT = new QName(NAMESPACE, "Dokumentproduksjon_v3Port");

    private String endpointUrl; // NOSONAR

    public DokumentproduksjonConsumerConfig(@Value("${Dokumentproduksjon_v3.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    DokumentproduksjonV3 getPort() {
        Map<String, Object> properties = new HashMap<>();

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(SERVICE);
        factoryBean.setEndpointName(PORT);
        factoryBean.setServiceClass(DokumentproduksjonV3.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CorrelationIdOutgoingSOAPInterceptor());
        return factoryBean.create(DokumentproduksjonV3.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
