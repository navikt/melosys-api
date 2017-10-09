package no.nav.melosys.integrasjon.medl2.medlemskap;

import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;

import javax.xml.bind.*;
import java.io.*;

public class MedlemskapMock implements MedlemskapConsumer {

    @Override
    public HentPeriodeListeResponse hentPeriodeListe(HentPeriodeListeRequest request) throws PersonIkkeFunnet, Sikkerhetsbegrensning {
        String ident = request.getIdent().getValue();

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(HentPeriodeListeResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            InputStream is = getClass().getClassLoader().getResourceAsStream("mock/medlemskap/" + ident + ".xml");

            if (is == null) {
                throw new PersonIkkeFunnet("Person med ident " + ident + "ikke funnet.", null);
            }

            Object xmlBean = unmarshaller.unmarshal(is);
            HentPeriodeListeResponse response = (HentPeriodeListeResponse) xmlBean;
            return response;
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
