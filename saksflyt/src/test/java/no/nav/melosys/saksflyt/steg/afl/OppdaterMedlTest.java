package no.nav.melosys.saksflyt.steg.afl;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterMedlTest {

    private OppdaterMedl oppdaterMedl;

    @Mock
    private MedlPeriodeService medlPeriodeService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private MedlFasade medlFasade;
    @Mock
    private SaksopplysningerService saksopplysningerService;

    private Behandling behandling;

    @Before
    public void setup() throws MelosysException {
        oppdaterMedl = new OppdaterMedl(medlFasade, medlPeriodeService, lovvalgsperiodeService, saksopplysningerService);

        SedDokument sedDokument = new SedDokument();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(sedDokument);
        behandling = new Behandling();
        behandling.getSaksopplysninger().add(saksopplysning);

        when(medlFasade.opprettPeriodeUnderAvklaring(any(), any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED))).thenReturn(234L);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(lovvalgsperiode));
    }

    @Test
    public void utfør() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.getBehandling().setId(123321L);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "123");
        oppdaterMedl.utfør(prosessinstans);

        verify(medlPeriodeService).lagreMedlPeriodeId(anyLong(), any(Lovvalgsperiode.class), anyLong());
        verify(medlFasade).opprettPeriodeUnderAvklaring(eq("123"), any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED));
    }
}