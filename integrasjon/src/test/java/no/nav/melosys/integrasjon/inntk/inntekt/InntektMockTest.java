package no.nav.melosys.integrasjon.inntk.inntekt;

import static no.nav.melosys.integrasjon.inntk.InntektFasade.FILTER;
import static no.nav.melosys.integrasjon.inntk.InntektFasade.FILTER_URI;
import static no.nav.melosys.integrasjon.inntk.InntektFasade.FORMAALSKODE;
import static no.nav.melosys.integrasjon.inntk.InntektFasade.FORMAALSKODE_URI;
import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.datatype.DatatypeFactory;

import org.junit.Test;

import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Ainntektsfilter;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Formaal;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ObjectFactory;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.PersonIdent;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Uttrekksperiode;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeRequest;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeResponse;

public class InntektMockTest {
    @Test
    public void hentInntektListe() throws Exception {
        InntektMock mock = new InntektMock();
        HentInntektListeRequest request = new HentInntektListeRequest();

        ObjectFactory objectFactory = new ObjectFactory();
        PersonIdent personIdent = objectFactory.createPersonIdent();
        personIdent.setPersonIdent("99999999992");
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

        HentInntektListeResponse response = mock.hentInntektListe(request);
        assertThat(response.getArbeidsInntektIdent().getArbeidsInntektMaaned().size()).isGreaterThan(0);
    }

}