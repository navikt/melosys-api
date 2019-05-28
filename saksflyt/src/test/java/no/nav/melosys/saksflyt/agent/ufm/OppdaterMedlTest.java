package no.nav.melosys.saksflyt.agent.ufm;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
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
    private SaksopplysningRepository saksopplysningRepository;

    private OppdaterMedl oppdaterMedl;

    @Before
    public void setUp() {
        oppdaterMedl = new OppdaterMedl(medlFasade, oppdaterMedlFelles, lovvalgsperiodeService, saksopplysningRepository);
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SEDOPPL)))
            .thenReturn(Optional.of(hentSaksopplysning()));
    }

    @Test
    public void utfør() throws Exception {
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "123";
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(personDokument);
        saksopplysning.setType(SaksopplysningType.PERSOPL);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.lagreLovvalgsperioder(anyLong(), any())).thenReturn(Collections.singleton(lovvalgsperiode));

        Behandling behandling = new Behandling();
        behandling.setId(12L);
        behandling.getSaksopplysninger().add(saksopplysning);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        oppdaterMedl.utfør(prosessinstans);

        verify(medlFasade).opprettPeriodeEndelig(any(), any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED));
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(eq(12L), any());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING);
    }

    private Saksopplysning hentSaksopplysning() {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now().plusYears(1L)));
        sedDokument.setLovvalgslandKode(Landkoder.DE);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(sedDokument);
        return saksopplysning;
    }
}