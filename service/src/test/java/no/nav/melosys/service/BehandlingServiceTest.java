package no.nav.melosys.service;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BehandlingServiceTest {

    @Mock
    private BehandlingRepository behandlingRepo;

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    private TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepo;

    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private BehandlingService behandlingService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        behandlingService = new BehandlingService(behandlingRepo, behandlingsresultatRepository, tidligereMedlemsperiodeRepo, behandlingsresultatService);
    }

    @Test
    public void oppdaterStatus() throws FunksjonellException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVVENT_DOK_PART);
    }

    @Test(expected = IkkeFunnetException.class)
    public void oppdaterStatus_behIkkeFunnet() throws FunksjonellException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.empty());
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVVENT_DOK_PART);
    }

    @Test(expected = FunksjonellException.class)
    public void oppdaterStatus_ugyldig() throws FunksjonellException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVSLUTTET);
    }

    @Test
    public void knyttMedlemsperioder_ingenBehandling() throws FunksjonellException {
        long behandlingID = 11L;
        List<Long> periodeIder = Arrays.asList(2L, 3L);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.empty());

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Behandling " + behandlingID + " finnes ikke.");

        behandlingService.knyttMedlemsperioder(behandlingID, periodeIder);
    }

    @Test
    public void knyttMedlemsperioder_avsluttetBehandling() throws FunksjonellException {
        long behandlingID = 11L;
        Behandlingsstatus behandlingsstatus = Behandlingsstatus.AVSLUTTET;
        Behandling behandling = new Behandling();
        behandling.setStatus(behandlingsstatus);
        List<Long> periodeIder = Arrays.asList(2L, 3L);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Medlemsperioder kan ikke lagres på behandling med status " + behandlingsstatus);

        behandlingService.knyttMedlemsperioder(behandlingID, periodeIder);
    }

    @Test
    public void knyttMedlemsperioder() throws FunksjonellException {
        long behandlingID = 11L;
        Behandlingsstatus behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING;
        Behandling behandling = new Behandling();
        behandling.setStatus(behandlingsstatus);
        List<Long> periodeIder = Arrays.asList(2L, 3L);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));

        behandlingService.knyttMedlemsperioder(behandlingID, periodeIder);
        verify(tidligereMedlemsperiodeRepo, times(1)).deleteById_BehandlingId(behandlingID);
        verify(tidligereMedlemsperiodeRepo).saveAll(anyList());
    }

    @Test
    public void finnMedlemsperioder_ingenTidligereMedlemsperioder() {
        long behandlingID = 11L;
        when(tidligereMedlemsperiodeRepo.findById_BehandlingId(anyLong())).thenReturn(null);

        List<Long> periodeIder = behandlingService.hentMedlemsperioder(behandlingID);
        assertThat(periodeIder).isNotNull();
        assertThat(periodeIder).isEmpty();
    }

    @Test
    public void hentMedlemsperioder() {
        long behandlingID = 11L;
        List<TidligereMedlemsperiode> tidligereMedlemsperioder = Arrays.asList(
            new TidligereMedlemsperiode(behandlingID, 2L),
            new TidligereMedlemsperiode(behandlingID, 3L));
        when(tidligereMedlemsperiodeRepo.findById_BehandlingId(anyLong())).thenReturn(tidligereMedlemsperioder);

        List<Long> periodeIder = behandlingService.hentMedlemsperioder(behandlingID);
        assertThat(periodeIder).isNotNull();
        assertThat(periodeIder).containsExactly(2L, 3L);
    }

    @Test
    public void nyBehandling() {
        String initierendeJournalpostId = "234";
        String initierendeDokumentId = "221234";
        Behandling behandling = behandlingService.nyBehandling(new Fagsak(), Behandlingsstatus.OPPRETTET, Behandlingstyper.SOEKNAD, initierendeJournalpostId, initierendeDokumentId);
        verify(behandlingRepo).save(any(Behandling.class));
        assertThat(behandling.getType()).isEqualTo(Behandlingstyper.SOEKNAD);
        assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.OPPRETTET);
        assertThat(behandling.getInitierendeJournalpostId()).isEqualTo(initierendeJournalpostId);
        assertThat(behandling.getInitierendeDokumentId()).isEqualTo(initierendeDokumentId);
    }

    @Test
    public void replikerBehandling_replikererObjekterOgCollections() throws NoSuchMethodException, TekniskException, InstantiationException, IkkeFunnetException, IllegalAccessException, InvocationTargetException {
        Behandling tidligsteInaktiveBehandling = opprettBehandlingMedData();
        Behandling replikertBehandling = behandlingService.replikerBehandling(tidligsteInaktiveBehandling, Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.ENDRET_PERIODE);

        assertThat(replikertBehandling.getId()).isNull();
        assertThat(replikertBehandling.getStatus()).isEqualTo(Behandlingsstatus.UNDER_BEHANDLING);
        assertThat(replikertBehandling.getDokumentasjonSvarfristDato()).isEqualTo(tidligsteInaktiveBehandling.getDokumentasjonSvarfristDato());
        assertThat(replikertBehandling.getInitierendeJournalpostId()).isEqualTo(tidligsteInaktiveBehandling.getInitierendeJournalpostId());

        assertThat(replikertBehandling.getSaksopplysninger().size()).isEqualTo(1);
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getId() == null);
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getBehandling().equals(replikertBehandling));
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getDokumentXml().equals("dokxml"));
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getType().equals(SaksopplysningType.INNTEKT));
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getEndretDato().toString().equals("2020-02-11T09:37:30Z"));
    }

    private Behandling opprettBehandlingMedData() {
        Behandling behandling = opprettTomBehandlingMedId();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setInitierendeJournalpostId("initierendeJournalpostId");
        behandling.setDokumentasjonSvarfristDato(Instant.parse("2017-12-11T09:37:30.00Z"));
        behandling.setSaksopplysninger(new LinkedHashSet<>());

        behandling.getSaksopplysninger().add(opprettSaksopplysning("dokxml", SaksopplysningType.INNTEKT, "2020-02-11T09:37:30Z"));
        return behandling;
    }

    private Saksopplysning opprettSaksopplysning(String dokxml, SaksopplysningType saksopplysningType, String endretDato) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setBehandling(opprettTomBehandlingMedId());
        saksopplysning.setDokumentXml(dokxml);
        saksopplysning.setType(saksopplysningType);
        saksopplysning.setEndretDato(Instant.parse(endretDato));
        return saksopplysning;
    }

    private Behandling opprettTomBehandlingMedId() {
        Behandling behandling = new Behandling();
        behandling.setId(665L);
        return behandling;
    }

    private Behandlingsresultat opprettTomtBehandlingsresultatMedId() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(667L);
        return behandlingsresultat;
    }

    @Test
    public void replikerBehandlingOgBehandlingsresultat_replikererBehandlingsresultatObjekterOgCollections() throws NoSuchMethodException, TekniskException, InstantiationException, IkkeFunnetException, IllegalAccessException, InvocationTargetException {
        Behandling tidligsteInaktiveBehandling = opprettBehandlingMedData();
        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultatMedData();

        Avklartefakta avklartefakta = opprettAvklartefakta();
        behandlingsresultat.getAvklartefakta().add(avklartefakta);

        Vilkaarsresultat vilkaarsresultat = opprettVilkaarsresultat();
        behandlingsresultat.getVilkaarsresultater().add(vilkaarsresultat);

        Lovvalgsperiode lovvalgsperiode = opprettLovvalgsperiode();
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        doReturn(behandlingsresultat).when(behandlingsresultatService).hentBehandlingsresultat(tidligsteInaktiveBehandling.getId());

        Behandling behandlingsreplika = behandlingService.replikerBehandlingOgBehandlingsresultat(tidligsteInaktiveBehandling, Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.ENDRET_PERIODE);

        ArgumentCaptor<Behandlingsresultat> captor = ArgumentCaptor.forClass(Behandlingsresultat.class);
        verify(behandlingsresultatRepository).save(captor.capture());
        Behandlingsresultat behandlingsresultatreplika = captor.getValue();

        assertThat(behandlingsresultatreplika.getId()).isNull();
        assertThat(behandlingsresultatreplika.getBehandling()).isEqualTo(behandlingsreplika);
        assertThat(behandlingsresultatreplika.getBehandlingsmåte()).isEqualTo(behandlingsresultat.getBehandlingsmåte());
        assertThat(behandlingsresultatreplika.getType()).isEqualTo(behandlingsresultat.getType());
        assertThat(behandlingsresultatreplika.getVedtaksdato()).isEqualTo(behandlingsresultat.getVedtaksdato());

        assertThat(behandlingsresultatreplika.getLovvalgsperioder()).allMatch(l -> l.getId() == null);
        assertThat(behandlingsresultatreplika.getLovvalgsperioder()).allMatch(l -> l.getBehandlingsresultat() == behandlingsresultatreplika);
        assertThat(behandlingsresultatreplika.getLovvalgsperioder()).allMatch(l -> l.getDekning().equals(Trygdedekninger.FULL_DEKNING_EOSFO));
        assertThat(behandlingsresultatreplika.getLovvalgsperioder()).allMatch(l -> l.getUnntakFraLovvalgsland().equals(Landkoder.BE));

        assertThat(behandlingsresultatreplika.getAvklartefakta()).allMatch(a -> a.getId() == null);
        assertThat(behandlingsresultatreplika.getAvklartefakta()).allMatch(a -> a.getBehandlingsresultat() == behandlingsresultatreplika);
        assertThat(behandlingsresultatreplika.getAvklartefakta()).allMatch(a -> a.getFakta().equals("fakta"));
        assertThat(behandlingsresultatreplika.getAvklartefakta()).allMatch(a -> a.getType().equals(Avklartefaktatype.ARBEIDSLAND));

        assertThat(behandlingsresultatreplika.getVilkaarsresultater()).allMatch(v -> v.getId() == null);
        assertThat(behandlingsresultatreplika.getVilkaarsresultater()).allMatch(v -> v.getBehandlingsresultat() == behandlingsresultatreplika);
        assertThat(behandlingsresultatreplika.getVilkaarsresultater()).allMatch(v -> v.getBegrunnelseFritekst().equals("fritekst"));
        VilkaarBegrunnelse vilkaarBegrunnelse = behandlingsresultatreplika.getVilkaarsresultater().stream().findFirst().get().getBegrunnelser().stream().findFirst().get();
        assertThat(vilkaarBegrunnelse.getId()).isNull();
        assertThat(vilkaarBegrunnelse.getKode()).isEqualTo("kode");
    }

    private Lovvalgsperiode opprettLovvalgsperiode() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setId(32L);
        lovvalgsperiode.setBehandlingsresultat(opprettTomtBehandlingsresultatMedId());
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
        lovvalgsperiode.setUnntakFraLovvalgsland(Landkoder.BE);
        return lovvalgsperiode;
    }

    private Avklartefakta opprettAvklartefakta() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setId(32L);
        avklartefakta.setBehandlingsresultat(opprettTomtBehandlingsresultatMedId());
        avklartefakta.setFakta("fakta");
        avklartefakta.setType(Avklartefaktatype.ARBEIDSLAND);
        return avklartefakta;
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

    private Behandlingsresultat opprettBehandlingsresultatMedData() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(30L);
        behandlingsresultat.setBehandling(opprettTomBehandlingMedId());
        behandlingsresultat.setBehandlingsmåte(Behandlingsmaate.MANUELT);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        behandlingsresultat.setVedtaksdato(Instant.parse("2002-02-11T09:37:30Z"));

        behandlingsresultat.setAvklartefakta(new LinkedHashSet<>());
        behandlingsresultat.setLovvalgsperioder(new LinkedHashSet<>());
        behandlingsresultat.setVilkaarsresultater(new LinkedHashSet<>());

        return behandlingsresultat;
    }
}