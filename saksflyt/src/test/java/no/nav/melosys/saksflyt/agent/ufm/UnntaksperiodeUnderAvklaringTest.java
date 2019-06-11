package no.nav.melosys.saksflyt.agent.ufm;

import java.time.LocalDate;
import java.util.Optional;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UnntaksperiodeUnderAvklaringTest {

    @Mock
    private OppdaterMedlFelles felles;
    @Mock
    private MedlFasade medlFasade;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private UnntaksperiodeUnderAvklaring unntaksperiodeUnderAvklaring;

    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    
    @Before
    public void setUp() {
        unntaksperiodeUnderAvklaring = new UnntaksperiodeUnderAvklaring(felles, medlFasade, behandlingRepository, behandlingsresultatRepository);
        when(behandlingsresultatRepository.findById(anyLong())).thenReturn(Optional.of(behandlingsresultat));
    }

    @Test
    public void utfør_ingenEksisterendePeriode_opprettPeriodeUnderAvklaring() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setSaksopplysninger(Sets.newHashSet(
            hentSedSaksopplysning(LocalDate.now(), LocalDate.now().plusYears(1L)), hentPersonDokument())
        );
        prosessinstans.setBehandling(behandling);
        when(behandlingRepository.findWithSaksopplysningerById(anyLong())).thenReturn(behandling);

        unntaksperiodeUnderAvklaring.utfør(prosessinstans);

        verify(medlFasade).opprettPeriodeUnderAvklaring(any(), any(Lovvalgsperiode.class), any());
    }

    @Test
    public void utfør_eksisterendePeriode_ingenNyPeriode() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.getSaksopplysninger().add(hentSedSaksopplysning(LocalDate.now(), LocalDate.now().plusYears(1L)));
        prosessinstans.setBehandling(behandling);

        behandlingsresultat.getLovvalgsperioder().add(new Lovvalgsperiode());

        unntaksperiodeUnderAvklaring.utfør(prosessinstans);

        verify(medlFasade, never()).opprettPeriodeUnderAvklaring(any(), any(), any());
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
        return sedDokument;
    }

    private Saksopplysning hentPersonDokument() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "123";
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysning.setDokument(personDokument);
        return saksopplysning;
    }
}