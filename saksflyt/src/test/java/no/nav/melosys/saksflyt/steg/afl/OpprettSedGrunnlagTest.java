package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettSedGrunnlagTest {

    private OpprettSedGrunnlag opprettSedGrunnlag;

    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Mock
    private EessiService eessiService;

    @Before
    public void setup() {
        opprettSedGrunnlag = new OpprettSedGrunnlag(behandlingsgrunnlagService, eessiService);
    }

    @Test
    public void utfør() throws MelosysException {
        final String aktørID = "123";
        final Behandling behandling = new Behandling();
        behandling.setId(123321L);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);
        prosessinstans.setBehandling(behandling);

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setRinaSaksnummer("123");
        melosysEessiMelding.setSedId("abc");
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        when(eessiService.hentSedGrunnlag(anyString(), anyString())).thenReturn(new SedGrunnlag());

        opprettSedGrunnlag.utfør(prosessinstans);

        verify(behandlingsgrunnlagService).opprettSedGrunnlag(eq(behandling.getId()), any(SedGrunnlag.class));
        verify(eessiService).hentSedGrunnlag(eq("123"), eq("abc"));
    }
}