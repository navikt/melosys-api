package no.nav.melosys.integrasjon.joark.inngaaendejournal;


import javax.xml.namespace.QName;

import no.nav.melosys.integrasjon.felles.mdc.CallIdOutInterceptor;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.InngaaendeJournalV1;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class InngaaendeJournalConsumerConfig {

    private static final String WSDL = "wsdl/no/nav/tjeneste/virksomhet/inngaaendeJournal/v1/Binding.wsdl";
    private static final String NAMESPACE = "http://nav.no/tjeneste/virksomhet/inngaaendeJournal/v1/Binding";
    private static final QName SERVICE = new QName(NAMESPACE, "InngaaendeJournal_v1");
    private static final QName PORT = new QName(NAMESPACE, "InngaaendeJournal_v1Port");

    private String endpointUrl; // NOSONAR

    public InngaaendeJournalConsumerConfig(@Value("${InngaaendeJournal_v1.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    InngaaendeJournalV1 getPort() {
        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setWsdlURL(WSDL);
        factoryBean.setServiceName(SERVICE);
        factoryBean.setEndpointName(PORT);
        factoryBean.setServiceClass(InngaaendeJournalV1.class);
        factoryBean.setAddress(endpointUrl);
        factoryBean.getFeatures().add(new WSAddressingFeature());
        factoryBean.getFeatures().add(new LoggingFeature());
        factoryBean.getOutInterceptors().add(new CallIdOutInterceptor());
        return factoryBean.create(InngaaendeJournalV1.class);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
