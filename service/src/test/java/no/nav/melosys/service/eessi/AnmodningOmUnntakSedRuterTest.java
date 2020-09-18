package no.nav.melosys.service.eessi;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.eessi.ruting.AnmodningOmUnntakSedRuter;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningOmUnntakSedRuterTest {

    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private FagsakService fagsakService;

    private AnmodningOmUnntakSedRuter anmodningOmUnntakSedRuter;

    @Before
    public void setup() {
        anmodningOmUnntakSedRuter = new AnmodningOmUnntakSedRuter(prosessinstansService, fagsakService);
    }

    @Test
    public void finnSakOgBestemRuting_gsakSaksnummerErNull_NySak() throws FunksjonellException {
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setAktoerId("13412");
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, null);
        verify(prosessinstansService).opprettProsessinstansNySakMottattAnmodningOmUnntak(eq(melosysEessiMelding), eq(melosysEessiMelding.getAktoerId()));
    }

    @Test
    public void finnSakOgBestemRuting_sakEksistererStatusOpprettet_nyBehandling() throws FunksjonellException {
        final Long gsakSaksnummer = 123L;

        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Fagsak fagsak = new Fagsak();
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.setBehandlinger(List.of(new Behandling()));
        when(fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer)).thenReturn(Optional.of(fagsak));
        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer);
        verify(prosessinstansService).opprettProsessinstansNyBehandlingMottattAnmodningUnntak(eq(melosysEessiMelding), eq(gsakSaksnummer));
    }

    @Test
    public void finnSakOgBestemRuting_sakEksistererStatusLovvalgAvklart_nyBehandling() throws FunksjonellException {
        final Long gsakSaksnummer = 123L;

        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Behandling behandling = new Behandling();

        Fagsak fagsak = new Fagsak();
        fagsak.setBehandlinger(List.of(behandling));
        fagsak.setStatus(Saksstatuser.LOVVALG_AVKLART);
        when(fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer)).thenReturn(Optional.of(fagsak));
        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer);
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(eq(behandling), eq(melosysEessiMelding));
    }

    @Test
    public void finnSakOgBestemRuting_sakEksistererIkke_nySak() throws FunksjonellException {
        final Long gsakSaksnummer = 123L;
        final String aktørID = "1352";
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);

        when(fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer)).thenReturn(Optional.empty());
        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer);
        verify(prosessinstansService).opprettProsessinstansNySakMottattAnmodningOmUnntak(eq(melosysEessiMelding), eq(aktørID));
    }
}