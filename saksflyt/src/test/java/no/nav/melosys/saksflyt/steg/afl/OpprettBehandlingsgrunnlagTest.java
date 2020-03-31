package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.felles.OpprettSedDokumentFelles;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettBehandlingsgrunnlagTest {

    private OpprettBehandlingsgrunnlag opprettBehandlingsgrunnlag;

    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Mock
    private OpprettSedDokumentFelles opprettSedDokumentFelles;

    @Before
    public void setup() {
        opprettBehandlingsgrunnlag = new OpprettBehandlingsgrunnlag(behandlingsgrunnlagService, opprettSedDokumentFelles);
    }

    @Test
    public void utfør() throws MelosysException {
        final String aktørID = "123";
        final Behandling behandling = new Behandling();
        behandling.setId(123321L);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, new MelosysEessiMelding());
        prosessinstans.setBehandling(behandling);

        opprettBehandlingsgrunnlag.utfør(prosessinstans);

        verify(behandlingsgrunnlagService).opprettBehandlingsgrunnlag(eq(behandling.getId()), any(BehandlingsgrunnlagData.class));
        verify(opprettSedDokumentFelles).opprettSedSaksopplysning(any(MelosysEessiMelding.class), eq(behandling));

    }
}