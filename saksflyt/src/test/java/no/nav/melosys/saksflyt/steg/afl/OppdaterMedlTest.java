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
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.LovvalgsperiodeService;
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
    private BehandlingService behandlingService;
    @Mock
    private OppdaterMedlFelles oppdaterMedlFelles;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private MedlFasade medlFasade;

    @Before
    public void setup() throws MelosysException {
        oppdaterMedl = new OppdaterMedl(medlFasade, oppdaterMedlFelles, behandlingService, lovvalgsperiodeService);

        SedDokument sedDokument = new SedDokument();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(sedDokument);
        Behandling behandling = new Behandling();
        behandling.getSaksopplysninger().add(saksopplysning);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(medlFasade.opprettPeriodeUnderAvklaring(any(), any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED))).thenReturn(234L);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(lovvalgsperiode));
    }

    @Test
    public void utfør() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        prosessinstans.getBehandling().setId(123321L);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "123");
        oppdaterMedl.utfør(prosessinstans);

        verify(oppdaterMedlFelles).lagreMedlPeriodeId(anyLong(), any(Lovvalgsperiode.class), anyLong());
        verify(medlFasade).opprettPeriodeUnderAvklaring(eq("123"), any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED));
    }
}