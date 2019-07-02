package no.nav.melosys.saksflyt.steg.aou.svar;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.AnmodningsperiodeSvarType;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterBehandlingTest {

    private OppdaterBehandling oppdaterBehandling;

    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private BehandlingService behandlingService;

    private Anmodningsperiode anmodningsperiode = new Anmodningsperiode();

    @Before
    public void setUp() throws Exception {
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());
        oppdaterBehandling = new OppdaterBehandling(anmodningsperiodeService, behandlingService);
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singleton(anmodningsperiode));
    }

    @Test
    public void utfør_anmodningsperiodeIkkeInnvilget_statusVurderDokument() throws Exception {
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.AVSLAG);
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setYtterligereInformasjon("hei");

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Behandling behandling = new Behandling();
        behandling.setId(123L);

        prosessinstans.setBehandling(behandling);

        oppdaterBehandling.utfør(prosessinstans);

        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.VURDER_DOKUMENT));
    }

    @Test
    public void utfør_anmodningsperiodeInnvilget_statusIverksettVedtak() throws Exception {
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.INNVILGELSE);
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Behandling behandling = new Behandling();
        behandling.setId(123L);

        prosessinstans.setBehandling(behandling);

        oppdaterBehandling.utfør(prosessinstans);

        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.IVERKSETTER_VEDTAK));
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_VALIDERING);
    }

    @Test
    public void utfør_anmodningsperiodeInnvilgetMedYtteligereInfo_statusVurderDokument() throws Exception {
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.INNVILGELSE);
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setYtterligereInformasjon("hei");

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Behandling behandling = new Behandling();
        behandling.setId(123L);

        prosessinstans.setBehandling(behandling);

        oppdaterBehandling.utfør(prosessinstans);

        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.VURDER_DOKUMENT));
    }
}