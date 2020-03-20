package no.nav.melosys.saksflyt.steg.afl;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
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
    @Mock
    private BehandlingService behandlingService;

    private Behandling behandling;
    private Fagsak fagsak;

    @Before
    public void setup() throws MelosysException {
        oppdaterMedl = new OppdaterMedl(medlFasade, medlPeriodeService, lovvalgsperiodeService, saksopplysningerService, behandlingService);

        SedDokument sedDokument = new SedDokument();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(sedDokument);

        fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-1");
        behandling = new Behandling();
        behandling.getSaksopplysninger().add(saksopplysning);
        behandling.setFagsak(fagsak);
        behandling.setId(1122L);

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

        verify(behandlingService).oppdaterStatus(eq(behandling.getId()), eq(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING));
        verify(medlPeriodeService).lagreMedlPeriodeId(anyLong(), any(Lovvalgsperiode.class), anyLong());
        verify(medlFasade).opprettPeriodeUnderAvklaring(eq("123"), any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED));
    }
}