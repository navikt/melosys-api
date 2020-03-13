package no.nav.melosys.service.eessi;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
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
    public void finnSakOgBestemRuting_gsakSaksnummerErNull_NySak() throws FunksjonellException {
        RutingResultat resultat = anmodningUnntakMottakInitialiserer.finnSakOgBestemRuting(new Prosessinstans(), null);
        assertThat(resultat).isEqualTo(RutingResultat.NY_SAK);
    }

    @Test
    public void finnSakOgBestemRuting_sakEksistererStatusOpprettet_nyBehandling() throws FunksjonellException {
        final Long gsakSaksnummer = 123L;
        Fagsak fagsak = new Fagsak();
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.setBehandlinger(List.of(new Behandling()));
        when(fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer)).thenReturn(Optional.of(fagsak));
        RutingResultat resultat = anmodningUnntakMottakInitialiserer.finnSakOgBestemRuting(new Prosessinstans(), gsakSaksnummer);
        assertThat(resultat).isEqualTo(RutingResultat.NY_BEHANDLING);
    }

    @Test
    public void finnSakOgBestemRuting_sakEksistererStatusLovvalgAvklart_nyBehandling() throws FunksjonellException {
        final Long gsakSaksnummer = 123L;
        Fagsak fagsak = new Fagsak();
        fagsak.setBehandlinger(List.of(new Behandling()));
        fagsak.setStatus(Saksstatuser.LOVVALG_AVKLART);
        when(fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer)).thenReturn(Optional.of(fagsak));
        RutingResultat resultat = anmodningUnntakMottakInitialiserer.finnSakOgBestemRuting(new Prosessinstans(), gsakSaksnummer);
        assertThat(resultat).isEqualTo(RutingResultat.INGEN_BEHANDLING);
    }

    @Test
    public void finnSakOgBestemRuting_sakEksistererIkke_nySak() throws FunksjonellException {
        final Long gsakSaksnummer = 123L;
        when(fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer)).thenReturn(Optional.empty());
        RutingResultat resultat = anmodningUnntakMottakInitialiserer.finnSakOgBestemRuting(new Prosessinstans(), gsakSaksnummer);
        assertThat(resultat).isEqualTo(RutingResultat.NY_SAK);
    }
}