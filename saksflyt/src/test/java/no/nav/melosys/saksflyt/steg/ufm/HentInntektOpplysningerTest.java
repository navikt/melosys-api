package no.nav.melosys.saksflyt.steg.ufm;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import no.nav.melosys.service.SaksopplysningerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HentInntektOpplysningerTest {
    @Mock
    private HentOpplysningerFelles hentOpplysningerFelles;
    @Mock
    private SaksopplysningerService saksopplysningerService;

    private HentInntektOpplysninger hentInntektOpplysninger;

    @Before
    public void setUp() {
        hentInntektOpplysninger = new HentInntektOpplysninger(hentOpplysningerFelles, saksopplysningerService);
    }

    @Test
    public void utførSteg() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "123123");

        Periode periode = new Periode(LocalDate.now(), LocalDate.now().plusMonths(1));
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(periode);
        when(saksopplysningerService.hentSedOpplysninger(anyLong())).thenReturn(sedDokument);

        Behandling behandling = new Behandling();
        behandling.setId(2L);

        prosessinstans.setBehandling(behandling);

        hentInntektOpplysninger.utfør(prosessinstans);

        verify(hentOpplysningerFelles).hentOgLagreInntektsopplysninger(eq(2L), eq("123123"));
    }
}