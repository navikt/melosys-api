package no.nav.melosys.integrasjon.inntk.inntekt;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeHarIkkeTilgangTilOensketAInntektsfilter;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeUgyldigInput;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeRequest;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeResponse;

public class InntektMock implements InntektConsumer {

    private static final String FNR = "99999999992";

    @Override
    public HentInntektListeResponse hentInntektListe(HentInntektListeRequest request) throws HentInntektListeSikkerhetsbegrensning, HentInntektListeUgyldigInput, HentInntektListeHarIkkeTilgangTilOensketAInntektsfilter {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            InputStream is = getClass().getClassLoader().getResourceAsStream("mock/inntekt/" + FNR + ".xml");
            Object xmlBean = unmarshaller.unmarshal(is);
            return ((no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeResponse) xmlBean).getResponse();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
