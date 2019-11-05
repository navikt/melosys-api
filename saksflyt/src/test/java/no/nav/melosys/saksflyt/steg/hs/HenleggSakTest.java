package no.nav.melosys.saksflyt.steg.hs;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.OppdaterFagsakOgBehandling;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HenleggSakTest {
    @Mock
    OppdaterFagsakOgBehandling felles;

    @InjectMocks
    HenleggSak henleggSak;

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.HENLEGG_SAK);
        Fagsak fagsak = new Fagsak();
        behandling.setFagsak(fagsak);

        henleggSak.utfør(prosessinstans);

        verify(felles).oppdaterFagsakOgBehandlingStatuser(eq(behandling), eq(Saksstatuser.HENLAGT), eq(Behandlingsstatus.AVSLUTTET));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.HS_SEND_BREV);
    }
}