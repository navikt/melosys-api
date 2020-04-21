package no.nav.melosys.service.behandling;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BehandlingsresultatServiceTest {
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepo;

    private BehandlingsresultatService behandlingsresultatService;

    @Before
    public void setUp() {
        behandlingsresultatService = spy(new BehandlingsresultatService(behandlingsresultatRepo));
    }

    @Test
    public void tømBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAvklartefakta(new HashSet<>(Collections.singletonList(new Avklartefakta())));
        behandlingsresultat.setLovvalgsperioder(new HashSet<>(Collections.singletonList(new Lovvalgsperiode())));
        behandlingsresultat.setVilkaarsresultater(new HashSet<>(Collections.singleton(new Vilkaarsresultat())));

        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.of(behandlingsresultat));

        behandlingsresultatService.tømBehandlingsresultat(1L);

        assertThat(behandlingsresultat.getAvklartefakta()).isEmpty();
        assertThat(behandlingsresultat.getLovvalgsperioder()).isEmpty();
        assertThat(behandlingsresultat.getVilkaarsresultater()).isEmpty();
    }

    @Test(expected = IkkeFunnetException.class)
    public void hentBehandlingsresultat_medTomtResultat_forventerException() throws IkkeFunnetException {
        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.empty());
        behandlingsresultatService.hentBehandlingsresultat(4L);
    }

    @Test
    public void hentBehandlingsresultat_returnererBehandlingsresultat() throws IkkeFunnetException {
        Behandlingsresultat resultat = new Behandlingsresultat();
        BehandlingsresultatBegrunnelse begrunnelse = new BehandlingsresultatBegrunnelse();
        begrunnelse.setKode(Henleggelsesgrunner.ANNET.getKode());
        resultat.getBehandlingsresultatBegrunnelser().add(begrunnelse);
        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.of(resultat));

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(4L);
        begrunnelse = behandlingsresultat.getBehandlingsresultatBegrunnelser().iterator().next();
        assertThat(begrunnelse.getKode()).isEqualTo(Henleggelsesgrunner.ANNET.getKode());
    }

    private Behandlingsresultat opprettTomtBehandlingsresultatMedId() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(667L);
        return behandlingsresultat;
    }

    @Test
    public void replikerBehandlingOgBehandlingsresultat_replikererBehandlingsresultatObjekterOgCollections()
        throws NoSuchMethodException, InstantiationException, IkkeFunnetException, IllegalAccessException, InvocationTargetException {
        Behandling tidligsteInaktiveBehandling = new Behandling();
        tidligsteInaktiveBehandling.setId(1L);
        Behandling behandlingsreplika = new Behandling();
        behandlingsreplika.setId(2L);

        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling);

        Avklartefakta avklartefakta = opprettAvklartefakta();
        behandlingsresultat.getAvklartefakta().add(avklartefakta);

        Vilkaarsresultat vilkaarsresultat = opprettVilkaarsresultat();
        behandlingsresultat.getVilkaarsresultater().add(vilkaarsresultat);

        Lovvalgsperiode lovvalgsperiode = opprettLovvalgsperiode();
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        Anmodningsperiode anmodningsperiode = opprettAnmodningsperiode();
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));

        BehandlingsresultatBegrunnelse behandlingsresultatBegrunnelse = opprettBehandlingsresultatBegrunnelse();
        behandlingsresultat.getBehandlingsresultatBegrunnelser().add(behandlingsresultatBegrunnelse);

        Kontrollresultat kontrollresultat = opprettKontrollresultat();
        behandlingsresultat.getKontrollresultater().add(kontrollresultat);

        doReturn(behandlingsresultat).when(behandlingsresultatService).hentBehandlingsresultat(1L);

        behandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingsreplika);

        ArgumentCaptor<Behandlingsresultat> captor = ArgumentCaptor.forClass(Behandlingsresultat.class);
        verify(behandlingsresultatRepo).save(captor.capture());
        Behandlingsresultat behandlingsresultatreplika = captor.getValue();

        assertThat(behandlingsresultatreplika.getId()).isNull();
        assertThat(behandlingsresultatreplika.getBehandling()).isEqualTo(behandlingsreplika);
        assertThat(behandlingsresultatreplika.getBehandlingsmåte()).isEqualTo(behandlingsresultat.getBehandlingsmåte());
        assertThat(behandlingsresultatreplika.getType()).isEqualTo(behandlingsresultat.getType());
        assertThat(behandlingsresultatreplika.getVedtakMetadata()).isNull();

        assertThat(behandlingsresultatreplika.getLovvalgsperioder()).allMatch(l -> l.getId() == null);
        assertThat(behandlingsresultatreplika.getLovvalgsperioder()).allMatch(l -> l.getBehandlingsresultat() == behandlingsresultatreplika);
        assertThat(behandlingsresultatreplika.getLovvalgsperioder()).allMatch(l -> l.getDekning().equals(Trygdedekninger.FULL_DEKNING_EOSFO));

        assertThat(behandlingsresultatreplika.getAnmodningsperioder()).allMatch(a -> a.getId() == null);
        assertThat(behandlingsresultatreplika.getAnmodningsperioder()).allMatch(a -> a.getAnmodningsperiodeSvar() == null);
        assertThat(behandlingsresultatreplika.getAnmodningsperioder()).allMatch(a -> !a.erSendtUtland());
        assertThat(behandlingsresultatreplika.getAnmodningsperioder()).allMatch(a -> a.getBehandlingsresultat() == behandlingsresultatreplika);
        assertThat(behandlingsresultatreplika.getAnmodningsperioder()).allMatch(a -> a.getDekning().equals(Trygdedekninger.FULL_DEKNING_EOSFO));

        assertThat(behandlingsresultatreplika.getAvklartefakta()).allMatch(a -> a.getId() == null);
        assertThat(behandlingsresultatreplika.getAvklartefakta()).allMatch(a -> a.getBehandlingsresultat() == behandlingsresultatreplika);
        assertThat(behandlingsresultatreplika.getAvklartefakta()).allMatch(a -> a.getFakta().equals("fakta"));
        assertThat(behandlingsresultatreplika.getAvklartefakta()).allMatch(a -> a.getType().equals(Avklartefaktatyper.ARBEIDSLAND));
        assertThat(behandlingsresultatreplika.getVilkaarsresultater()).allMatch(v -> v.getId() == null);
        assertThat(behandlingsresultatreplika.getVilkaarsresultater()).allMatch(v -> v.getBehandlingsresultat() == behandlingsresultatreplika);
        assertThat(behandlingsresultatreplika.getVilkaarsresultater()).allMatch(v -> v.getBegrunnelseFritekst().equals("fritekst"));
        VilkaarBegrunnelse vilkaarBegrunnelse = behandlingsresultatreplika.getVilkaarsresultater().stream().findFirst().get().getBegrunnelser().stream().findFirst().get();
        assertThat(vilkaarBegrunnelse.getId()).isNull();
        assertThat(vilkaarBegrunnelse.getKode()).isEqualTo("kode");

        assertThat(behandlingsresultatreplika.getBehandlingsresultatBegrunnelser()).allMatch(a -> a.getId() == null);
        assertThat(behandlingsresultatreplika.getBehandlingsresultatBegrunnelser()).allMatch(a -> a.getBehandlingsresultat() == behandlingsresultatreplika);
        assertThat(behandlingsresultatreplika.getBehandlingsresultatBegrunnelser()).allMatch(a -> a.getKode().equals("begrunnelsekode"));

        assertThat(behandlingsresultatreplika.getKontrollresultater()).allMatch(a -> a.getId() == null);
        assertThat(behandlingsresultatreplika.getKontrollresultater()).allMatch(a -> a.getBehandlingsresultat() == behandlingsresultatreplika);
        assertThat(behandlingsresultatreplika.getKontrollresultater()).allMatch(a -> a.getBegrunnelse() == Kontroll_begrunnelser.FEIL_I_PERIODEN);
    }

    @Test
    public void oppdaterBehandlingsresultattype_idEksisterer_oppdatererBehandlingsresultattype() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        doReturn(Optional.of(behandlingsresultat)).when(behandlingsresultatRepo).findById(1L);

        behandlingsresultatService.oppdaterBehandlingsresultattype(1L, Behandlingsresultattyper.IKKE_FASTSATT);

        assertThat(behandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.IKKE_FASTSATT);
        verify(behandlingsresultatRepo).save(behandlingsresultat);
    }

    @Test
    public void oppdaterBehandlingsresultattype_idEksistererIkke_gjørIngenting() {
        behandlingsresultatService.oppdaterBehandlingsresultattype(1L, Behandlingsresultattyper.IKKE_FASTSATT);
        verify(behandlingsresultatRepo).findById(1L);
        verify(behandlingsresultatRepo, never()).save(any());
    }

    @Test
    public void oppdaterBehandlingsmaate_bhmåteUdefinert_verifiserOppdatert() throws FunksjonellException {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandlingsmåte(Behandlingsmaate.UDEFINERT);
        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.of(behandlingsresultat));
        behandlingsresultatService.oppdaterBehandlingsMaate(1L, Behandlingsmaate.AUTOMATISERT);
        verify(behandlingsresultatRepo).save(behandlingsresultat);
        assertThat(behandlingsresultat.getBehandlingsmåte()).isEqualTo(Behandlingsmaate.AUTOMATISERT);
    }

    private Lovvalgsperiode opprettLovvalgsperiode() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setId(32L);
        lovvalgsperiode.setBehandlingsresultat(opprettTomtBehandlingsresultatMedId());
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
        return lovvalgsperiode;
    }

    private Anmodningsperiode opprettAnmodningsperiode() {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setId(32L);
        anmodningsperiode.setBehandlingsresultat(opprettTomtBehandlingsresultatMedId());
        anmodningsperiode.setSendtUtland(true);
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());
        anmodningsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
        return anmodningsperiode;
    }

    private Avklartefakta opprettAvklartefakta() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setId(32L);
        avklartefakta.setBehandlingsresultat(opprettTomtBehandlingsresultatMedId());
        avklartefakta.setFakta("fakta");
        avklartefakta.setType(Avklartefaktatyper.ARBEIDSLAND);
        return avklartefakta;
    }

    private BehandlingsresultatBegrunnelse opprettBehandlingsresultatBegrunnelse() {
        BehandlingsresultatBegrunnelse behandlingsresultatBegrunnelse = new BehandlingsresultatBegrunnelse();
        behandlingsresultatBegrunnelse.setId(32L);
        behandlingsresultatBegrunnelse.setBehandlingsresultat(opprettTomtBehandlingsresultatMedId());
        behandlingsresultatBegrunnelse.setKode("begrunnelsekode");
        return behandlingsresultatBegrunnelse;
    }

    private Vilkaarsresultat opprettVilkaarsresultat() {
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBehandlingsresultat(opprettTomtBehandlingsresultatMedId());
        vilkaarsresultat.setId(32L);
        vilkaarsresultat.setBegrunnelseFritekst("fritekst");

        HashSet<VilkaarBegrunnelse> begrunnelser = new HashSet<>();
        VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelse.setId(2222L);
        vilkaarBegrunnelse.setKode("kode");
        begrunnelser.add(vilkaarBegrunnelse);
        vilkaarsresultat.setBegrunnelser(begrunnelser);
        return vilkaarsresultat;
    }

    private Kontrollresultat opprettKontrollresultat() {
        Kontrollresultat kontrollresultat = new Kontrollresultat();
        kontrollresultat.setId(123L);
        kontrollresultat.setBehandlingsresultat(opprettTomtBehandlingsresultatMedId());
        kontrollresultat.setBegrunnelse(Kontroll_begrunnelser.FEIL_I_PERIODEN);
        return kontrollresultat;
    }

    private Behandlingsresultat opprettBehandlingsresultatMedData(Behandling tidligsteInaktiveBehandling) {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(30L);
        behandlingsresultat.setBehandling(tidligsteInaktiveBehandling);
        behandlingsresultat.setBehandlingsmåte(Behandlingsmaate.MANUELT);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        VedtakMetadata vedtakMetadata = new VedtakMetadata();
        vedtakMetadata.setVedtaksdato(Instant.parse("2002-02-11T09:37:30Z"));
        behandlingsresultat.setVedtakMetadata(vedtakMetadata);

        behandlingsresultat.setAvklartefakta(new LinkedHashSet<>());
        behandlingsresultat.setLovvalgsperioder(new LinkedHashSet<>());
        behandlingsresultat.setVilkaarsresultater(new LinkedHashSet<>());

        return behandlingsresultat;
    }
}