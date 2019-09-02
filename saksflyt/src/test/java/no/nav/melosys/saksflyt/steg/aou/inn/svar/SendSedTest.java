package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import java.util.Arrays;
import java.util.Collections;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SendSedTest {

    @Mock
    private EessiService eessiService;
    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;

    private SendSed sendSed;

    @Before
    public void setup() {
        sendSed = new SendSed(eessiService, anmodningsperiodeService);
    }

    @Test
    public void utfør() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong()))
            .thenReturn(Collections.singletonList(new AnmodningsperiodeSvar()));

        sendSed.utfør(prosessinstans);

        verify(anmodningsperiodeService).hentAnmodningsperiodeSvarForBehandling(anyLong());
        verify(eessiService).sendAnmodningUnntakSvar(any(AnmodningsperiodeSvar.class), anyLong());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_SVAR_OPPDATER_MEDL);
    }

    @Test(expected = FunksjonellException.class)
    public void utfør_medFlereAnmodningsperiodeSvar_forventException() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong()))
            .thenReturn(Arrays.asList(
                new AnmodningsperiodeSvar(),
                new AnmodningsperiodeSvar()
            ));

        sendSed.utfør(prosessinstans);

        verify(anmodningsperiodeService).hentAnmodningsperiodeSvarForBehandling(anyLong());
    }
}