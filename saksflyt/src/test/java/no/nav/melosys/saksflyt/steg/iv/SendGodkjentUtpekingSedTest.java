package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SendGodkjentUtpekingSedTest {

    @Mock
    private EessiService eessiService;
    @Mock
    private SaksopplysningerService saksopplysningerService;

    private SendGodkjentUtpekingSed sendGodkjentUtpekingSed;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        sendGodkjentUtpekingSed = new SendGodkjentUtpekingSed(eessiService, saksopplysningerService);
    }

    @Test
    public void utfør_sedErElektronisk_sendSvarSedElektronisk() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();

        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setType(Behandlingstyper.BESLUTNING_LOVVALG_NORGE);
        prosessinstans.setBehandling(behandling);

        SedDokument sedDokument = new SedDokument();
        sedDokument.setErElektronisk(true);
        when(saksopplysningerService.hentSedOpplysninger(eq(behandling.getId()))).thenReturn(sedDokument);

        sendGodkjentUtpekingSed.utfør(prosessinstans);

        verify(eessiService).sendGodkjenningArbeidFlereLand(eq(behandling.getId()));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE);
    }

    @Test
    public void utfør_sedErIkkeElektronisk_ikkeImplementertKastException() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();

        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setType(Behandlingstyper.BESLUTNING_LOVVALG_NORGE);
        prosessinstans.setBehandling(behandling);

        SedDokument sedDokument = new SedDokument();
        sedDokument.setErElektronisk(false);
        when(saksopplysningerService.hentSedOpplysninger(eq(behandling.getId()))).thenReturn(sedDokument);

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Sending av brev-A012 er ikke implementert");

        sendGodkjentUtpekingSed.utfør(prosessinstans);
    }

    @Test
    public void utfør_behandlingstypeSøknad_kasterExceptionFeilBehandlingstype() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();

        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setType(Behandlingstyper.SOEKNAD);
        prosessinstans.setBehandling(behandling);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Kan ikke sende A012 på en behandling av type");

        sendGodkjentUtpekingSed.utfør(prosessinstans);
    }


}