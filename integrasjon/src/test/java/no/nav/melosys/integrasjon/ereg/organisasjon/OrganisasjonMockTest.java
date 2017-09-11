package no.nav.melosys.integrasjon.ereg.organisasjon;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonRequest;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse;

public class OrganisasjonMockTest {
    @Test
    public void hentOrganisasjon() throws Exception {
        OrganisasjonMock organisasjonMock = new OrganisasjonMock();
        List<String> numre = Arrays.asList("971432063", "973271334", "973162152");

        for (String orgNummer : numre) {
            HentOrganisasjonRequest request = new HentOrganisasjonRequest();
            request.setOrgnummer(orgNummer);
            HentOrganisasjonResponse response = organisasjonMock.hentOrganisasjon(request);
            assertThat(response.getOrganisasjon().getOrgnummer()).isEqualTo(orgNummer);
        }
    }

}