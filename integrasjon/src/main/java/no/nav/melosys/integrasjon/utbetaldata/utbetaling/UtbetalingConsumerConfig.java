package no.nav.melosys.integrasjon.utbetaldata.utbetaling;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingSOAPInterceptor;
import no.nav.tjeneste.virksomhet.utbetaling.v1.UtbetalingV1;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UtbetalingConsumerConfig {
    private static final String UTBETALING_V1_WSDL = "wsdl/utbetaling/no/nav/tjeneste/virksomhet/utbetaling/v1/Binding.wsdl";
    private static final String UTBETALING_V1_NAMESPACE = "http://nav.no/tjeneste/virksomhet/utbetaling/v1/Binding";
    private static final QName UTBETALING_V1_SERVICE = new QName(UTBETALING_V1_NAMESPACE, "Utbetaling_v1");
    private static final QName UTBETALING_V1_PORT = new QName(UTBETALING_V1_NAMESPACE, "Utbetaling_v1Port");

    private String endpointUrl;

    public UtbetalingConsumerConfig(@Value("${Utbetaling_v1.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    UtbetalingV1 getPort() {
        Map<String, Object> properties = new HashMap<>();

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(UTBETALING_V1_WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(UTBETALING_V1_SERVICE);
        factoryBean.setEndpointName(UTBETALING_V1_PORT);
        factoryBean.setServiceClass(UtbetalingV1.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CorrelationIdOutgoingSOAPInterceptor());
        return factoryBean.create(UtbetalingV1.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
