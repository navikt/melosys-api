package no.nav.melosys.saksflyt.steg.aou.inn;

import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import no.nav.melosys.service.BehandlingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HentMedlemskapsopplysningerTest {

    @Mock
    private HentOpplysningerFelles hentOpplysningerFelles;
    @Mock
    private BehandlingService behandlingService;

    private HentMedlemskapsopplysninger hentMedlemskapsopplysninger;

    @Before
    public void setUp() {
        hentMedlemskapsopplysninger = new HentMedlemskapsopplysninger(hentOpplysningerFelles, behandlingService);
    }

    @Test
    public void utfør() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(LocalDate.now(), LocalDate.now().plusMonths(2), false);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(prosessinstans.getBehandling());
        hentMedlemskapsopplysninger.utfør(prosessinstans);
        verify(hentOpplysningerFelles).hentOgLagreMedlemskapsopplysninger(anyLong(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_HENT_YTELSER);
    }

    private Prosessinstans hentProsessinstans(LocalDate fom, LocalDate tom, boolean erEndring) {
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom ,tom);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, erEndring);
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