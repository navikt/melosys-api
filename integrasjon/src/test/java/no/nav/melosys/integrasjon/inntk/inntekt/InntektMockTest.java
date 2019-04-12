package no.nav.melosys.integrasjon.inntk.inntekt;

import javax.xml.datatype.DatatypeFactory;

import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse;
import org.junit.Test;

import static no.nav.melosys.integrasjon.inntk.InntektService.*;
import static org.assertj.core.api.Assertions.assertThat;

public class InntektMockTest {
    @Test
    public void hentInntektListe() throws Exception {
        InntektMock mock = new InntektMock();
        HentInntektListeBolkRequest request = new HentInntektListeBolkRequest();

        ObjectFactory objectFactory = new ObjectFactory();
        PersonIdent personIdent = objectFactory.createPersonIdent();
        personIdent.setPersonIdent("99999999992");
        request.getIdentListe().add(personIdent);

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

        HentInntektListeBolkResponse response = mock.hentInntektListeBolk(request);
        assertThat(response.getArbeidsInntektIdentListe().get(0).getArbeidsInntektMaaned().size()).isGreaterThan(0);
    }

}