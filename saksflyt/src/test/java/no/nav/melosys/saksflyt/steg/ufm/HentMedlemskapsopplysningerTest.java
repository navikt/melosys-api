package no.nav.melosys.saksflyt.steg.ufm;

import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.BehandlingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HentMedlemskapsopplysningerTest {

    @Mock
    private SaksopplysningRepository saksopplysningRepository;
    @Mock
    private MedlFasade medlFasade;
    @Mock
    private BehandlingService behandlingService;

    private HentMedlemskapsopplysninger hentMedlemskapsopplysninger;

    @Before
    public void setUp() throws Exception {
        hentMedlemskapsopplysninger = new HentMedlemskapsopplysninger(saksopplysningRepository,medlFasade, behandlingService);
        when(medlFasade.hentPeriodeListe(anyString(), any(LocalDate.class), any()))
            .thenReturn(new Saksopplysning());
    }

    @Test
    public void utfør() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(hentSedSaksopplysning(LocalDate.now(), LocalDate.now()),false);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(prosessinstans.getBehandling());
        hentMedlemskapsopplysninger.utfør(prosessinstans);
        verify(medlFasade).hentPeriodeListe(anyString(), any(), any());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_HENT_YTELSER);
    }

    private Prosessinstans hentProsessinstans(Saksopplysning saksopplysning, boolean erEndring) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.ER_ENDRING, erEndring);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "123123");

        Behandling behandling = new Behandling();
        behandling.setId(2L);
        behandling.getSaksopplysninger().add(saksopplysning);

        prosessinstans.setBehandling(behandling);
        return prosessinstans;
    }

    private Saksopplysning hentSedSaksopplysning(LocalDate fom, LocalDate tom) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(hentSedDokument(fom, tom));
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        return saksopplysning;
    }

    private SedDokument hentSedDokument(LocalDate fom, LocalDate tom) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new no.nav.melosys.domain.dokument.medlemskap.Periode(fom, tom));
        sedDokument.setFnr("tada");
        return sedDokument;

    }
}