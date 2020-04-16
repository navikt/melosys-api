package no.nav.melosys.saksflyt.steg.ufm;

import java.time.LocalDate;
import java.util.Collections;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterMedlTest {

    @Mock
    private MedlPeriodeService medlPeriodeService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private BehandlingService behandlingService;
    @Captor
    private ArgumentCaptor<Behandling> behandlingCaptor;

    private OppdaterMedl oppdaterMedl;

    private final Behandling behandling = new Behandling();

    @Before
    public void setUp() throws Exception {
        oppdaterMedl = new OppdaterMedl(medlPeriodeService, lovvalgsperiodeService, behandlingService);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
    }

    @Test
    public void utfør_ingenLovvalgsperiode_opprettNyLovvalgsperiode() throws Exception {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.lagreLovvalgsperioder(anyLong(), any())).thenReturn(Collections.singleton(lovvalgsperiode));

        behandling.setId(12L);
        behandling.getSaksopplysninger().addAll(Sets.newHashSet(lagPersonSaksopplysning(), hentSedSaksopplysning()));
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        oppdaterMedl.utfør(prosessinstans);

        verify(medlPeriodeService).opprettPeriodeEndelig(any(Lovvalgsperiode.class), eq(12L), eq(true), eq("123"));
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(eq(12L), any());
        verify(behandlingService, never()).lagre(any(Behandling.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VARSLE_UTLAND);
    }

    @Test
    public void utfør_ingenLovvalgsperiodeArtikkel13_opprettNyLovvalgsperiode() throws Exception {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        when(lovvalgsperiodeService.lagreLovvalgsperioder(anyLong(), any())).thenReturn(Collections.singleton(lovvalgsperiode));

        behandling.setId(12L);
        behandling.getSaksopplysninger().addAll(Sets.newHashSet(lagPersonSaksopplysning(), hentSedSaksopplysning()));
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        oppdaterMedl.utfør(prosessinstans);

        verify(medlPeriodeService).opprettPeriodeForeløpig(any(Lovvalgsperiode.class), eq(12L), eq(true), eq("123"));
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(eq(12L), any());
        verify(behandlingService).lagre(behandlingCaptor.capture());
        assertThat(behandlingCaptor.getValue().getStatus()).isEqualTo(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VARSLE_UTLAND);
    }

    @Test
    public void utfør_erEksisterendeLovvalgsperiodeMedMedlId_oppdaterPeriodeEndeligMedl() throws Exception {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setMedlPeriodeID(123456L);
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singleton(lovvalgsperiode));

        behandling.setId(12L);
        behandling.getSaksopplysninger().addAll(Sets.newHashSet(lagPersonSaksopplysning(), hentSedSaksopplysning()));
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        oppdaterMedl.utfør(prosessinstans);

        verify(medlPeriodeService).oppdaterPeriodeEndelig(any(Lovvalgsperiode.class), eq(true));
        verify(lovvalgsperiodeService).hentLovvalgsperioder(eq(12L));
        verify(behandlingService, never()).lagre(any(Behandling.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VARSLE_UTLAND);
    }

    @Test
    public void utfør_erEksisterendeLovvalgsperiodeUtenMedlId_opprettOgLagrePeriodeEndelig() throws Exception {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singleton(lovvalgsperiode));

        behandling.setId(12L);
        behandling.getSaksopplysninger().addAll(Sets.newHashSet(lagPersonSaksopplysning(), hentSedSaksopplysning()));
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        oppdaterMedl.utfør(prosessinstans);

        verify(medlPeriodeService).opprettPeriodeEndelig(any(Lovvalgsperiode.class), eq(12L), eq(true), eq("123"));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VARSLE_UTLAND);
    }

    private Saksopplysning lagPersonSaksopplysning() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "123";
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(personDokument);
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        return saksopplysning;
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