package no.nav.melosys.integrasjon.gsak.oppgave;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OppgaveConsumerProducerDelegator {
    private OppgaveConsumerProducer producer;

    @Autowired
    public OppgaveConsumerProducerDelegator(OppgaveConsumerProducer producer) {
        this.producer = producer;
    }


    public OppgaveConsumer arbeidsfordelingConsumerForEndUser() {
        return producer.oppgaveConsumer();
    }


    public OppgaveSelftestConsumer arbeidsfordelingSelftestConsumerForSystemUser() {
        return producer.oppgaveSelftestConsumer();
    }
}
