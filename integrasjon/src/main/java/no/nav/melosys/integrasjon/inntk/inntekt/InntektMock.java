package no.nav.melosys.integrasjon.inntk.inntekt;

import java.io.IOException;
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
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("mock/inntekt/" + FNR + ".xml")) {
            JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Object xmlBean = unmarshaller.unmarshal(is);
            return ((no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeResponse) xmlBean).getResponse();
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
