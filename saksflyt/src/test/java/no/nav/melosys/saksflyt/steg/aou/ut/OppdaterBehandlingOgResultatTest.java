package no.nav.melosys.saksflyt.steg.aou.ut;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.AOU_AVKLAR_MYNDIGHET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterBehandlingOgResultatTest {
    @Mock
    BehandlingService behandlingService;
    @Mock
    BehandlingsresultatService behandlingsresultatService;

    private OppdaterBehandlingOgResultat oppdaterBehandlingOgResultat;

    @Before
    public void setUp() {
        oppdaterBehandlingOgResultat = new OppdaterBehandlingOgResultat(behandlingService, behandlingsresultatService);
    }

    @Test
    public void utfør() throws IkkeFunnetException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.ANMODNING_OM_UNNTAK);
        String testbruker = "Z097";
        p.setData(ProsessDataKey.SAKSBEHANDLER, testbruker);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        oppdaterBehandlingOgResultat.utfør(p);

        assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        assertThat(behandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        assertThat(behandlingsresultat.getEndretAv()).isEqualTo(testbruker);
        assertThat(p.getSteg()).isEqualTo(AOU_AVKLAR_MYNDIGHET);
    }
}