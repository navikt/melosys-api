package no.nav.melosys.service.eessi;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class SvarAnmodningUnntakInitialisererTest {

    private SvarAnmodningUnntakInitialiserer svarAnmodningUnntakInitialiserer;

    @Mock
    private FagsakService fagsakService;

    @Before
    public void setUp() {
        svarAnmodningUnntakInitialiserer = new SvarAnmodningUnntakInitialiserer(fagsakService);
    }

    @Test
    public void finnSakOgBestemRuting_korrektBehandlingsstatus_verifiserKorrektResultat() throws Exception {

        when(fagsakService.finnFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak(Behandlingsstatus.ANMODNING_UNNTAK_SENDT)));
        Prosessinstans prosessinstans = hentProsessinstans();
        RutingResultat resultat = svarAnmodningUnntakInitialiserer.finnSakOgBestemRuting(prosessinstans, 1L);

        assertThat(resultat).isEqualTo(RutingResultat.OPPDATER_BEHANDLING);
        assertThat(prosessinstans.getBehandling()).isNotNull();
    }

    @Test(expected = FunksjonellException.class)
    public void finnSakOgBestemRuting_feilBehandlingsstatus_forventException() throws Exception {

        when(fagsakService.finnFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak(Behandlingsstatus.FORELOEPIG_LOVVALG)));
        Prosessinstans prosessinstans = hentProsessinstans();
        svarAnmodningUnntakInitialiserer.finnSakOgBestemRuting(prosessinstans, 1L);
    }

    @Test(expected = TekniskException.class)
    public void finnSakOgBestemRuting_korrektBehandlingsstatusIngenFagsak_forventException() throws Exception {

        when(fagsakService.finnFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.empty());
        Prosessinstans prosessinstans = hentProsessinstans();
        svarAnmodningUnntakInitialiserer.finnSakOgBestemRuting(prosessinstans, 1L);
    }

    @Test
    public void hentAktuellProsessType() {
        assertThat(svarAnmodningUnntakInitialiserer.hentAktuellProsessType()).isEqualTo(ProsessType.ANMODNING_OM_UNNTAK_SVAR);
    }

    private Fagsak hentFagsak(Behandlingsstatus behandlingsstatus) {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setStatus(behandlingsstatus);
        behandling.setId(123L);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        return fagsak;
    }

    private Prosessinstans hentProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, new MelosysEessiMelding());
        return prosessinstans;
    }
}