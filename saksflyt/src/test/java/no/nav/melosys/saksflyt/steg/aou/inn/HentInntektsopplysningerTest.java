package no.nav.melosys.saksflyt.steg.aou.inn;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import no.nav.melosys.service.BehandlingService;
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
public class HentInntektsopplysningerTest {

    @Mock
    private HentOpplysningerFelles hentOpplysningerFelles;
    @Mock
    private BehandlingService behandlingService;

    private HentInntektsopplysninger hentInntektsopplysninger;

    @Before
    public void setUp() {
        hentInntektsopplysninger = new HentInntektsopplysninger(hentOpplysningerFelles, behandlingService);
    }

    @Test
    public void utførSteg() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "123123");

        Behandling behandling = new Behandling();
        behandling.setId(2L);
        behandling.setSaksopplysninger(Collections.singleton(hentSedSaksopplysning(LocalDate.now(), LocalDate.now().plusMonths(2))));

        prosessinstans.setBehandling(behandling);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        hentInntektsopplysninger.utfør(prosessinstans);

        verify(hentOpplysningerFelles).hentOgLagreInntektsopplysninger(eq(2L), eq("123123"));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_REGISTERKONTROLL);
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