package no.nav.melosys.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;

public class LovvalgsperiodeServiceTest {

    public final LovvalgsperiodeService instanse;
    private static final Collection<Lovvalgsperiode> LOVVALGSPERIODER = Collections.singletonList(lagLovvalgsperiode());

    public LovvalgsperiodeServiceTest() {
        this.instanse = new LovvalgsperiodeService(mockBehandlingsresultatRepo(), mockLovvalgsperiodeRepo());
    }

    private static LovvalgsperiodeRepository mockLovvalgsperiodeRepo() {
        LovvalgsperiodeRepository mock = mock(LovvalgsperiodeRepository.class);
        @SuppressWarnings("unchecked")
        Collection<Lovvalgsperiode> anyCollection = any(Collection.class);
        when(mock.save(anyCollection)).thenAnswer(i -> i.getArgument(0));
        return mock;
    }

    private static BehandlingsresultatRepository mockBehandlingsresultatRepo() {
        BehandlingsresultatRepository mock = mock(BehandlingsresultatRepository.class);
        when(mock.findOne(eq(13L))).thenReturn(lagBehandlingsresultat(13L));
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
    public void lagreLovvalgsperioderGirKopiMedBehandlingsresultat() {
        assertThat(LOVVALGSPERIODER.iterator().next().getBehandlingsresultat()).isNull();
        Collection<Lovvalgsperiode> resultat = instanse.lagreLovvalgsperioder(13L, LOVVALGSPERIODER);
        assertThat(resultat).size().isEqualTo(LOVVALGSPERIODER.size());
        assertThat(resultat.iterator().next().getBehandlingsresultat()).isNotNull();
    }

    @Test
    public void lagreLovvalgsperioderUtenBehandlingsresultatGitTomListe() {
        Collection<Lovvalgsperiode> resultat = instanse.lagreLovvalgsperioder(42L, LOVVALGSPERIODER);
        assertThat(resultat).isEmpty();
    }

    private static Lovvalgsperiode lagLovvalgsperiode() {
        Lovvalgsperiode resultat = new Lovvalgsperiode();
        return resultat;
    }

}
