package no.nav.melosys.integrasjon.medl.medlemskap;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import no.nav.melosys.integrasjon.felles.mdc.CallIdOutInterceptor;
import no.nav.tjeneste.virksomhet.medlemskap.v2.MedlemskapV2;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MedlemskapConsumerConfig {
    private static final String WSDL = "wsdl/no/nav/tjeneste/virksomhet/medlemskap/v2/MedlemskapV2.wsdl";
    private static final String NAMESPACE = "http://nav.no/tjeneste/virksomhet/medlemskap/v2";
    private static final QName SERVICE = new QName(NAMESPACE, "Medlemskap_v2");
    private static final QName PORT = new QName(NAMESPACE, "Medlemskap_v2Port");
    private static final QName RESPONSE = new QName(NAMESPACE,"hentPeriodeListeResponse");

    private String endpointUrl; // NOSONAR

    public MedlemskapConsumerConfig(@Value("${Medlemskap_v2.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    MedlemskapV2 getPort() {
        Map<String, Object> properties = new HashMap<>();

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(SERVICE);
        factoryBean.setEndpointName(PORT);
        factoryBean.setServiceClass(MedlemskapV2.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(MedlemskapV2.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public static QName getResponse() {
        return RESPONSE;
    }
}
