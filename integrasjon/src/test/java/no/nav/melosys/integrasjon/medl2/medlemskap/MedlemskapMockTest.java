package no.nav.melosys.integrasjon.medl2.medlemskap;

import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Foedselsnummer;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MedlemskapMockTest {

    @Test
    public void hentPeriodeListeTest() throws Exception {
        MedlemskapMock medlemskapMock = new MedlemskapMock();
        List<String> numre = Arrays.asList(
                "77777777773", "77777777778","77777777779",
                "77777777774", "77777777775", "66666666661");

        for (String fnr : numre) {
            Foedselsnummer ident = new Foedselsnummer();
            ident.setValue(fnr);

            HentPeriodeListeRequest request = new HentPeriodeListeRequest();
            request.setIdent(ident);

            HentPeriodeListeResponse response = medlemskapMock.hentPeriodeListe(request);

            assertNotNull(response);
        }
    }
}
