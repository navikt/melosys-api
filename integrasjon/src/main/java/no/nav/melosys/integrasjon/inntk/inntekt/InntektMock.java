package no.nav.melosys.integrasjon.inntk.inntekt;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import no.nav.tjeneste.virksomhet.inntekt.v3.binding.*;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse;

// FIXME: flyttes til src/test
public class InntektMock implements InntektConsumer {

    private static final String FNR = "99999999992";

    @Override
    public HentInntektListeBolkResponse hentInntektListeBolk(HentInntektListeBolkRequest request) throws HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter, HentInntektListeBolkUgyldigInput {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("mock/inntekt/" + FNR + "_bolk.xml")) {
            JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeBolkResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Object xmlBean = unmarshaller.unmarshal(is);
            return ((no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeBolkResponse) xmlBean).getResponse();
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
