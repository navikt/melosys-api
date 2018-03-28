package no.nav.melosys.integrasjon.joark.behandleinngaaendejournal;

import javax.xml.namespace.QName;

import no.nav.melosys.integrasjon.felles.mdc.CallIdOutInterceptor;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.BehandleInngaaendeJournalV1;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BehandleInngaaendeJournalConsumerConfig {

    private static final String WSDL = "wsdl/no/nav/tjeneste/virksomhet/behandleInngaaendeJournal/v1/Binding.wsdl";
    private static final String NAMESPACE = "http://nav.no/tjeneste/virksomhet/behandleInngaaendeJournal/v1/Binding";
    private static final QName SERVICE = new QName(NAMESPACE, "BehandleInngaaendeJournal_v1");
    private static final QName PORT = new QName(NAMESPACE, "BehandleInngaaendeJournal_v1Port");

    private String endpointUrl; // NOSONAR

    @Autowired
    public BehandleInngaaendeJournalConsumerConfig(@Value("${BehandleInngaaendeJournal_v1.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    BehandleInngaaendeJournalV1 getPort() {
        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(WSDL);
        factoryBean.setServiceName(SERVICE);
        factoryBean.setEndpointName(PORT);
        factoryBean.setServiceClass(BehandleInngaaendeJournalV1.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(BehandleInngaaendeJournalV1.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

}
