package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningUnntakMottakInitialisererTest {

    @Mock
    private FagsakService fagsakService;

    private AnmodningUnntakMottakInitialiserer anmodningUnntakMottakInitialiserer;

    @Before
    public void setup() {
        anmodningUnntakMottakInitialiserer = new AnmodningUnntakMottakInitialiserer(fagsakService);
    }

    @Test
    public void finnSakOgBestemRuting_gsakSaksnummerErNull_NySak() {
        RutingResultat resultat = anmodningUnntakMottakInitialiserer.finnSakOgBestemRuting(new Prosessinstans(), null);
        assertThat(resultat).isEqualTo(RutingResultat.NY_SAK);
    }

    @Test
    public void finnSakOgBestemRuting_sakEksistererStatusOpprettet_nyBehandling() {
        final Long gsakSaksnummer = 123L;
        Fagsak fagsak = new Fagsak();
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        when(fagsakService.hentFagsakFraGsakSaksnummer(gsakSaksnummer)).thenReturn(Optional.of(fagsak));
        RutingResultat resultat = anmodningUnntakMottakInitialiserer.finnSakOgBestemRuting(new Prosessinstans(), gsakSaksnummer);
        assertThat(resultat).isEqualTo(RutingResultat.NY_BEHANDLING);
    }

    @Test
    public void finnSakOgBestemRuting_sakEksistererStatusAvsluttet_nyBehandling() {
        final Long gsakSaksnummer = 123L;
        Fagsak fagsak = new Fagsak();
        fagsak.setStatus(Saksstatuser.AVSLUTTET);
        when(fagsakService.hentFagsakFraGsakSaksnummer(gsakSaksnummer)).thenReturn(Optional.of(fagsak));
        RutingResultat resultat = anmodningUnntakMottakInitialiserer.finnSakOgBestemRuting(new Prosessinstans(), gsakSaksnummer);
        assertThat(resultat).isEqualTo(RutingResultat.INGEN_BEHANDLING);
    }

    @Test
    public void finnSakOgBestemRuting_sakEksistererIkke_nySak() {
        final Long gsakSaksnummer = 123L;
        when(fagsakService.hentFagsakFraGsakSaksnummer(gsakSaksnummer)).thenReturn(Optional.empty());
        RutingResultat resultat = anmodningUnntakMottakInitialiserer.finnSakOgBestemRuting(new Prosessinstans(), gsakSaksnummer);
        assertThat(resultat).isEqualTo(RutingResultat.NY_SAK);
    }
}