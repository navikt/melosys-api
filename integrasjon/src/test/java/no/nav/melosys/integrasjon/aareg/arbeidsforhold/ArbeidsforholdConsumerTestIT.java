package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import no.nav.melosys.integrasjon.test.TpsTestData;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

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

    public void xml() throws JAXBException, FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning, FinnArbeidsforholdPrArbeidstakerUgyldigInput, IntegrasjonException {
        JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.arbeidsforhold.v3.FinnArbeidsforholdPrArbeidstakerResponse.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        FinnArbeidsforholdPrArbeidstakerRequest request = new FinnArbeidsforholdPrArbeidstakerRequest();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(TpsTestData.STD_FNR);
        request.setIdent(norskIdent);

        Regelverker regelverker = new Regelverker();
        regelverker.setValue("ALLE");
        request.setRapportertSomRegelverk(regelverker);

        FinnArbeidsforholdPrArbeidstakerResponse response = consumer.finnArbeidsforholdPrArbeidstaker(request);

        no.nav.tjeneste.virksomhet.arbeidsforhold.v3.FinnArbeidsforholdPrArbeidstakerResponse xmlRoot = new no.nav.tjeneste.virksomhet.arbeidsforhold.v3.FinnArbeidsforholdPrArbeidstakerResponse();
        xmlRoot.setParameters(response);
        marshaller.marshal(xmlRoot, System.out);

    }
}