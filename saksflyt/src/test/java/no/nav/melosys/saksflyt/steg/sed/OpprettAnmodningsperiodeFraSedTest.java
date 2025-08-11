package no.nav.melosys.saksflyt.steg.sed;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettAnmodningsperiodeFraSedTest {
    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private BehandlingService behandlingService;

    private OpprettAnmodningsperiodeFraSed opprettAnmodningsperiodeFraSed;

    @Captor
    private ArgumentCaptor<Collection<Anmodningsperiode>> argumentCaptor;

    @BeforeEach
    public void setup() {
        opprettAnmodningsperiodeFraSed = new OpprettAnmodningsperiodeFraSed(anmodningsperiodeService,
            behandlingService);
    }

    @Test
    void utfør_medSedHvorLovvalgslandErNorge_lagrerAnmodningsperiodeMedFullDekning() {
        Prosessinstans prosessinstans = Prosessinstans.builder().medType(ProsessType.OPPRETT_SAK).medStatus(ProsessStatus.KLAR).build();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        SedDokument sedDokument = lagSedDokument(Landkoder.NO);
        saksopplysning.setDokument(sedDokument);
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medSaksopplysninger(Collections.singleton(saksopplysning))
            .build();
        prosessinstans.setBehandling(behandling);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);

        opprettAnmodningsperiodeFraSed.utfør(prosessinstans);

        verify(anmodningsperiodeService).lagreAnmodningsperioder(eq(1L), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).contains(
            lagForventetAnmodningsperiode(sedDokument, Trygdedekninger.FULL_DEKNING_EOSFO));
    }

    @Test
    void utfør_medSedHvorLovvalgslandIkkeErNorge_lagrerAnmodningsperiodeUtenDekning() {
        Prosessinstans prosessinstans = Prosessinstans.builder().medType(ProsessType.OPPRETT_SAK).medStatus(ProsessStatus.KLAR).build();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        SedDokument sedDokument = lagSedDokument(Landkoder.DE);
        saksopplysning.setDokument(sedDokument);
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medSaksopplysninger(Collections.singleton(saksopplysning))
            .build();
        prosessinstans.setBehandling(behandling);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);

        opprettAnmodningsperiodeFraSed.utfør(prosessinstans);

        verify(anmodningsperiodeService).lagreAnmodningsperioder(eq(1L), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).contains(
            lagForventetAnmodningsperiode(sedDokument, Trygdedekninger.UTEN_DEKNING));
    }

    private SedDokument lagSedDokument(Landkoder lovvalgslandKode) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now().plusYears(1)));
        sedDokument.setLovvalgBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);
        sedDokument.setLovvalgslandKode(lovvalgslandKode);
        sedDokument.setUnntakFraLovvalgBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        sedDokument.setUnntakFraLovvalgslandKode(lovvalgslandKode == Landkoder.NO ? Landkoder.SE : Landkoder.NO);

        return sedDokument;
    }

    private static Anmodningsperiode lagForventetAnmodningsperiode(SedDokument sedDokument,
                                                                   Trygdedekninger trygdedekning) {
        return new Anmodningsperiode(sedDokument.getLovvalgsperiode().getFom(),
            sedDokument.getLovvalgsperiode().getTom(), Land_iso2.valueOf(sedDokument.getLovvalgslandKode().name()),
            sedDokument.getLovvalgBestemmelse(), null,
            Land_iso2.valueOf(sedDokument.getUnntakFraLovvalgslandKode().name()),
            sedDokument.getUnntakFraLovvalgBestemmelse(), trygdedekning);
    }
}
