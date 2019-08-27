package no.nav.melosys.service.eessi;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedType;
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
    public void initialiserProsessinstans_korrektBehandlingsstatusFagsakFinner_verifiserNesteSteg() throws Exception {

        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak(Behandlingsstatus.ANMODNING_UNNTAK_SENDT)));
        Prosessinstans prosessinstans = hentProsessinstans(SedType.A011);
        svarAnmodningUnntakInitialiserer.initialiserProsessinstans(prosessinstans);

        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.ANMODNING_OM_UNNTAK_SVAR);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_SVAR_OPPRETT_ANMODNINGSPERIODESVAR);
    }

    @Test(expected = FunksjonellException.class)
    public void initialiserProsessinstans_feilBehandlingsstatus_forventException() throws Exception {

        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak(Behandlingsstatus.FORELOEPIG_LOVVALG)));
        Prosessinstans prosessinstans = hentProsessinstans(SedType.A011);
        svarAnmodningUnntakInitialiserer.initialiserProsessinstans(prosessinstans);
    }

    @Test(expected = TekniskException.class)
    public void initialiserProsessinstans_korrektBehandlingsstatusIngeFagsak_forventException() throws Exception {

        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.empty());
        Prosessinstans prosessinstans = hentProsessinstans(SedType.A002);
        svarAnmodningUnntakInitialiserer.initialiserProsessinstans(prosessinstans);
    }

    private Fagsak hentFagsak(Behandlingsstatus behandlingsstatus) {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setStatus(behandlingsstatus);
        behandling.setId(123L);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        return fagsak;
    }

    private Prosessinstans hentProsessinstans(SedType sedType) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding(sedType));
        return prosessinstans;
    }

    private MelosysEessiMelding hentMelosysEessiMelding(SedType sedType) {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setGsakSaksnummer(123L);
        melosysEessiMelding.setSedType(sedType.name());

        return melosysEessiMelding;
    }
}