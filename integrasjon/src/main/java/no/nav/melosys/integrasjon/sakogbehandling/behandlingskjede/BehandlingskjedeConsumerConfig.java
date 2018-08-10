package no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import no.nav.melosys.integrasjon.felles.mdc.CallIdOutInterceptor;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BehandlingskjedeConsumerConfig {
    private static final String WSDL = "wsdl/no/nav/tjeneste/virksomhet/sakOgBehandling/v1/Binding.wsdl";
    private static final String NAMESPACE = "http://nav.no/tjeneste/virksomhet/sakOgBehandling/v1/Binding";
    private static final QName SERVICE = new QName(NAMESPACE, "SakOgBehandling_v1");
    private static final QName PORT = new QName(NAMESPACE, "SakOgBehandling_v1Port");

    private String endpointUrl; // NOSONAR

    public BehandlingskjedeConsumerConfig(@Value("${SakOgBehandling_v1.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    SakOgBehandlingV1 getPort() {
        Map<String, Object> properties = new HashMap<>();

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(SERVICE);
        factoryBean.setEndpointName(PORT);
        factoryBean.setServiceClass(SakOgBehandlingV1.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(SakOgBehandlingV1.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
