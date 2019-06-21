package no.nav.melosys.saksflyt.steg.ufm;

import java.time.LocalDate;
import java.util.Collections;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterMedlTest {

    @Mock
    private MedlFasade medlFasade;
    @Mock
    private OppdaterMedlFelles oppdaterMedlFelles;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private BehandlingService behandlingService;

    private OppdaterMedl oppdaterMedl;

    private final Behandling behandling = new Behandling();

    @Before
    public void setUp() throws Exception {
        oppdaterMedl = new OppdaterMedl(medlFasade, oppdaterMedlFelles, lovvalgsperiodeService, behandlingService);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
    }

    @Test
    public void utfør_ingenLovvalgsperiode_oppretNyLovvalgsperiode() throws Exception {
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "123";
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(personDokument);
        saksopplysning.setType(SaksopplysningType.PERSOPL);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.lagreLovvalgsperioder(anyLong(), any())).thenReturn(Collections.singleton(lovvalgsperiode));

        behandling.setId(12L);
        behandling.getSaksopplysninger().addAll(Sets.newHashSet(saksopplysning, hentSedSaksopplysning()));
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        oppdaterMedl.utfør(prosessinstans);

        verify(medlFasade).opprettPeriodeEndelig(any(), any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED));
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(eq(12L), any());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING);
    }

    @Test
    public void utfør_erEksisterendeLovvalgsperiode_oppdaterPeriodeEndeligMedl() throws Exception {
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "123";
        Saksopplysning personSaksopplysning = new Saksopplysning();
        personSaksopplysning.setDokument(personDokument);
        personSaksopplysning.setType(SaksopplysningType.PERSOPL);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singleton(lovvalgsperiode));

        behandling.setId(12L);
        behandling.getSaksopplysninger().addAll(Sets.newHashSet(personSaksopplysning, hentSedSaksopplysning()));
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        oppdaterMedl.utfør(prosessinstans);

        verify(medlFasade).oppdaterPeriodeEndelig(any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED));
        verify(lovvalgsperiodeService).hentLovvalgsperioder(eq(12L));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING);
    }

    private Saksopplysning hentSedSaksopplysning() {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now().plusYears(1L)));
        sedDokument.setLovvalgslandKode(Landkoder.DE);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(sedDokument);
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        return saksopplysning;
    }
}