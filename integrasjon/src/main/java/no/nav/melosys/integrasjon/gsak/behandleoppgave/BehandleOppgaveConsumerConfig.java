package no.nav.melosys.integrasjon.gsak.behandleoppgave;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import no.nav.melosys.integrasjon.felles.mdc.CallIdOutInterceptor;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.BehandleOppgaveV1;
import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.BehandleSakV1;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BehandleOppgaveConsumerConfig {
    private static final String BEHANDLE_OPPGAVE_V_1_WSDL = "behandleoppgave/wsdl/BehandleOppgaveV1.wsdl";
    private static final String BEHANDLE_OPPGAVE_V_1_NAMESPACE = "http://nav.no/tjeneste/virksomhet/behandleoppgave/v1";
    private static final QName BEHANDLE_OPPGAVE_V_1_SERVICE = new QName(BEHANDLE_OPPGAVE_V_1_NAMESPACE, "BehandleOppgave_v1");
    private static final QName BEHANDLE_OPPGAVE_V_1_PORT = new QName(BEHANDLE_OPPGAVE_V_1_NAMESPACE, "BehandleOppgaveV1");

    private String endpointUrl;

    public BehandleOppgaveConsumerConfig() {
    }

    public BehandleOppgaveConsumerConfig(@Value("${BehandleOppgave_v1.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    BehandleOppgaveV1 getPort() {
        Map<String, Object> properties = new HashMap<>();

        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(BEHANDLE_OPPGAVE_V_1_WSDL);
        factoryBean.setProperties(properties);
        factoryBean.setServiceName(BEHANDLE_OPPGAVE_V_1_SERVICE);
        factoryBean.setEndpointName(BEHANDLE_OPPGAVE_V_1_PORT);
        factoryBean.setServiceClass(BehandleSakV1.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(BehandleOppgaveV1.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
