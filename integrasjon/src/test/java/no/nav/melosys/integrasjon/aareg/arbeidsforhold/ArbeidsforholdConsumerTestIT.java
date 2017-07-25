package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.integrasjon.test.TestConfig;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource("classpath:test.properties" )
public class ArbeidsforholdConsumerTestIT {

    @Autowired
    private ArbeidsforholdConsumerConfig config;

    private ArbeidsforholdConsumer consumer;

    @Before
    public void setUp() {
        ArbeidsforholdConsumerProducer producer = new ArbeidsforholdConsumerProducer();
        producer.setConfig(config);

        consumer = producer.arbeidsforholdConsumer();
    }


    @Test
    public void finnArbeidsforholdPrArbeidstaker() throws Exception {
        FinnArbeidsforholdPrArbeidstakerRequest request = new FinnArbeidsforholdPrArbeidstakerRequest();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent("");
        request.setIdent(norskIdent);

        Regelverker regelverker = new Regelverker();
        regelverker.setValue("ALLE");
        request.setRapportertSomRegelverk(regelverker);

        consumer.finnArbeidsforholdPrArbeidstaker(request);

    }

}