package no.nav.melosys.saksflyt.steg.iv;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.saksflyt.felles.OppdaterFagsakOgBehandling;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.IV_STATUS_BEH_AVSL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttFagsakOgBehandlingTest {

    private AvsluttFagsakOgBehandling agent;

    @Mock
    private OppdaterFagsakOgBehandling felles;


    @Before
    public void setUp() {
        agent = new AvsluttFagsakOgBehandling(felles);
    }

    @Test
    public void utfoerSteg() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        Behandling behandling = new Behandling();

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-112");
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        behandling.setFagsak(fagsak);
        p.setBehandling(behandling);

        agent.utførSteg(p);

        verify(felles).oppdaterFagsakOgBehandlingStatuser(eq(behandling), eq(Saksstatuser.LOVVALG_AVKLART), eq(Behandlingsstatus.AVSLUTTET));
        assertThat(p.getSteg()).isEqualTo(IV_STATUS_BEH_AVSL);

    }
} 