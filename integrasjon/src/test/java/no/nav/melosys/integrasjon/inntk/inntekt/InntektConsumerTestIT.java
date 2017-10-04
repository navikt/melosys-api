package no.nav.melosys.integrasjon.inntk.inntekt;

import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListe;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Ainntektsfilter;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Formaal;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ObjectFactory;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.PersonIdent;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Uttrekksperiode;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeRequest;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeResponse;


@RunWith(SpringRunner.class)
@TestPropertySource("classpath:test.properties" )
public class InntektConsumerTestIT extends Gen3WsProxyServiceITBase {

    private static final String FORMAALSKODE = "Medlemskap";
    private static final String FORMAALSKODE_URI = "http://nav.no/kodeverk/Kode/Formaal/Medlemskap?v=5";
    private static final String FILTER = "MedlemskapA-inntekt";
    private static final String FILTER_URI = "http://nav.no/kodeverk/Kode/A-inntektsfilter/MedlemskapA-inntekt?v=6";
    private static final String FNR = "FJERNET";

    private ObjectFactory objectFactory = new ObjectFactory();

    @Autowired
    private InntektConsumerConfig config;

    private InntektConsumer consumer;

    @Before
    public void setUp() throws Exception {
        InntektConsumerProducer producer = new InntektConsumerProducer(config);

        consumer = producer.inntektConsumer();
    }

    @Test
    public void hentInntektsopplysninger() throws Exception {
        HentInntektListeRequest request = new HentInntektListeRequest();

        PersonIdent personIdent = objectFactory.createPersonIdent();
        personIdent.setPersonIdent(FNR);
        request.setIdent(personIdent);

        Ainntektsfilter ainntektsfilter = objectFactory.createAinntektsfilter();
        ainntektsfilter.setValue(FILTER);
        ainntektsfilter.setKodeRef(FILTER);
        ainntektsfilter.setKodeverksRef(FILTER_URI);
        request.setAinntektsfilter(ainntektsfilter);

        Uttrekksperiode uttrekksperiode = objectFactory.createUttrekksperiode();
        uttrekksperiode.setMaanedFom(DatatypeFactory.newInstance().newXMLGregorianCalendar("2017-06"));
        uttrekksperiode.setMaanedTom(DatatypeFactory.newInstance().newXMLGregorianCalendar("2017-08"));
        request.setUttrekksperiode(uttrekksperiode);

        Formaal formaal = objectFactory.createFormaal();
        formaal.setValue(FORMAALSKODE);
        formaal.setKodeRef(FORMAALSKODE);
        formaal.setKodeverksRef(FORMAALSKODE_URI);
        request.setFormaal(formaal);

        JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListe.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        HentInntektListe hentInntektListe = new HentInntektListe();
        hentInntektListe.setRequest(request);
        marshaller.marshal(hentInntektListe, System.out);

        HentInntektListeResponse response = consumer.hentInntektListe(request);

        assertThat(response.getArbeidsInntektIdent().getArbeidsInntektMaaned().size()).isGreaterThan(0);
    }


}