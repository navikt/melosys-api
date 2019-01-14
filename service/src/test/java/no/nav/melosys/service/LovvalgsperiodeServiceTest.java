package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.GrunnlagMedl;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LovvalgsperiodeServiceTest {

    public final LovvalgsperiodeService instanse;
    private static final Collection<Lovvalgsperiode> LOVVALGSPERIODER = Collections.singletonList(lagLovvalgsperiode());

    public LovvalgsperiodeServiceTest() {
        TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository = lagTidligerePerioderRepo();

        this.instanse = new LovvalgsperiodeService(mockBehandlingsresultatRepo(), mockLovvalgsperiodeRepo(), tidligereMedlemsperiodeRepository);
    }

    public static TidligereMedlemsperiodeRepository lagTidligerePerioderRepo() {
        TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository = mock(TidligereMedlemsperiodeRepository.class);
        TidligereMedlemsperiodeId medlemsperiodeId = new TidligereMedlemsperiodeId();
        medlemsperiodeId.setPeriodeId(23L);

        TidligereMedlemsperiode tidligerePeriode = new TidligereMedlemsperiode();
        tidligerePeriode.setId(medlemsperiodeId);

        when(tidligereMedlemsperiodeRepository.findById_BehandlingId(anyLong())).thenReturn(Arrays.asList(tidligerePeriode));
        return tidligereMedlemsperiodeRepository;
    }

    private static LovvalgsperiodeRepository mockLovvalgsperiodeRepo() {
        LovvalgsperiodeRepository mock = mock(LovvalgsperiodeRepository.class);
        @SuppressWarnings("unchecked")
        Collection<Lovvalgsperiode> anyCollection = any(Collection.class);
        when(mock.saveAll(anyCollection)).thenAnswer(i -> i.getArgument(0));
        return mock;
    }

    private static BehandlingsresultatRepository mockBehandlingsresultatRepo() {
        BehandlingsresultatRepository mock = mock(BehandlingsresultatRepository.class);
        when(mock.findById(eq(13L))).thenReturn(Optional.of(lagBehandlingsresultat(13L)));
        return mock;
    }

    private static Behandlingsresultat lagBehandlingsresultat(long id) {
        Behandlingsresultat resultat = new Behandlingsresultat();
        return resultat;
    }

    @Test
    public void hentIngenLovvalgsperioderGirTomListe() {
        Collection<Lovvalgsperiode> resultat = instanse.hentLovvalgsperioder(42L);
        assertThat(resultat).isEmpty();
    }

    @Test
    public void lagreLovvalgsperioderGirKopiMedBehandlingsresultat() throws Throwable {
        assertThat(LOVVALGSPERIODER.iterator().next().getBehandlingsresultat()).isNull();
        Collection<Lovvalgsperiode> resultat = instanse.lagreLovvalgsperioder(13L, LOVVALGSPERIODER);
        assertThat(resultat).size().isEqualTo(LOVVALGSPERIODER.size());
        assertThat(resultat.iterator().next().getBehandlingsresultat()).isNotNull();
    }

    @Test
    public void lagreLovvalgsperioderUtenBehandlingsresultatKasterException() throws Throwable {
        Throwable thrown = catchThrowable(() -> 
            instanse.lagreLovvalgsperioder(42L, LOVVALGSPERIODER)       
        );
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessageEndingWith("fins ikke.");
    }

    private static Lovvalgsperiode lagLovvalgsperiode() {
        Lovvalgsperiode resultat = new Lovvalgsperiode();
        return resultat;
    }

    @Test
    public void testTidligereLovvalgsperioder() throws TekniskException {
        Medlemsperiode medlemsperiode = lagMedlemsperiode(23L, GrunnlagMedl.FO_12_2.getKode());
        Medlemsperiode medlemsperiodeFeilId = lagMedlemsperiode(46L, GrunnlagMedl.FO_12_2.getKode());

        MedlemskapDokument medlDokument = new MedlemskapDokument();
        medlDokument.getMedlemsperiode().add(medlemsperiode);
        medlDokument.getMedlemsperiode().add(medlemsperiodeFeilId);

        Behandling behandling = lagBehandlingMedMedlOpplysning(medlDokument);

        Collection<Lovvalgsperiode> lovvalgsperioder = instanse.hentTidligereLovvalgsperioder(behandling);
        AssertionsForInterfaceTypes.assertThat(lovvalgsperioder.stream().map(lp -> lp.getMedlPeriodeID())).containsOnly(medlemsperiode.id);
        AssertionsForInterfaceTypes.assertThat(lovvalgsperioder.stream().map(lp -> lp.getFom())).isNotNull();
        AssertionsForInterfaceTypes.assertThat(lovvalgsperioder.stream().map(lp -> lp.getTom())).isNotNull();
        AssertionsForInterfaceTypes.assertThat(lovvalgsperioder.stream().map(lp -> lp.getBestemmelse())).isNotNull();
    }

    @Test
    public void testTidligerePerioderUkjentMapping() throws TekniskException {
        Medlemsperiode medlemsperiode = lagMedlemsperiode(23L, "AV_ANNET"); // Eksempel på mapping som ikke melosys kjenner til

        MedlemskapDokument medlDokument = new MedlemskapDokument();
        medlDokument.getMedlemsperiode().add(medlemsperiode);

        Behandling behandling = lagBehandlingMedMedlOpplysning(medlDokument);

        Collection<Lovvalgsperiode> lovvalgsperioder = instanse.hentTidligereLovvalgsperioder(behandling);
        AssertionsForInterfaceTypes.assertThat(lovvalgsperioder.stream().map(lp -> lp.getMedlPeriodeID())).containsOnly(medlemsperiode.id);
        AssertionsForInterfaceTypes.assertThat(lovvalgsperioder.stream()
                .map(lp -> lp.getBestemmelse())).containsOnly(LovvalgBestemmelse_883_2004.FO_883_2004_ANNET);
    }

    private Behandling lagBehandlingMedMedlOpplysning(MedlemskapDokument medlDokument) {
        Saksopplysning medl = new Saksopplysning();
        medl.setDokument(medlDokument);
        medl.setType(SaksopplysningType.MEDLEMSKAP);

        Behandling behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(1L);
        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Arrays.asList(medl)));
        return behandling;
    }

    private Medlemsperiode lagMedlemsperiode(long id, String grunnlagMedlKode) {
        Periode periode = new Periode(LocalDate.now(), LocalDate.now());
        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.id = id;
        medlemsperiode.periode = periode;
        medlemsperiode.grunnlagstype = grunnlagMedlKode;
        return medlemsperiode;
    }
}
