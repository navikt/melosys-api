package no.nav.melosys.saksflyt.steg.ufm;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Periode;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import no.nav.melosys.service.SaksopplysningerService;
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
    private SaksopplysningerService saksopplysningerService;

    private HentMedlemskapsopplysninger hentMedlemskapsopplysninger;

    @Before
    public void setUp() {
        hentMedlemskapsopplysninger = new HentMedlemskapsopplysninger(hentOpplysningerFelles, saksopplysningerService);
    }

    @Test
    public void utfør() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(false);
        when(saksopplysningerService.finnSedOpplysninger(anyLong()))
            .thenReturn(Optional.of(hentSedDokument(LocalDate.now(), LocalDate.now().plusMonths(2))));

        hentMedlemskapsopplysninger.utfør(prosessinstans);
        verify(hentOpplysningerFelles).hentOgLagreMedlemskapsopplysninger(anyLong(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_HENT_YTELSER);
    }

    private Prosessinstans hentProsessinstans(boolean erEndring) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, erEndring);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "123123");

        Behandling behandling = new Behandling();
        behandling.setId(2L);

        prosessinstans.setBehandling(behandling);
        return prosessinstans;
    }

    private SedDokument hentSedDokument(LocalDate fom, LocalDate tom) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new no.nav.melosys.domain.dokument.medlemskap.Periode(fom, tom));
        sedDokument.setFnr("tada");
        return sedDokument;
    }
}