package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import no.nav.melosys.integrasjon.test.TpsTestData;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;

@RunWith(SpringRunner.class)
public class ArbeidsforholdConsumerTestIT extends Gen3WsProxyServiceITBase {

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
        norskIdent.setIdent(TpsTestData.STD_FNR);
        request.setIdent(norskIdent);

        Regelverker regelverker = new Regelverker();
        regelverker.setValue("ALLE");
        request.setRapportertSomRegelverk(regelverker);

        FinnArbeidsforholdPrArbeidstakerResponse response =
                consumer.finnArbeidsforholdPrArbeidstaker(request);
        assertThat(response.getArbeidsforhold().size()).isGreaterThan(0);
    }

}