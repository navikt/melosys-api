package no.nav.melosys.service.behandling;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BehandlingsresultatServiceTest {
    private static final String AVKLARTEFAKTA_REGISTRERING_BEGRUNNELSE_KODE = "AvklartefaktaRegistrering-begrunnelsekode";

    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatCaptor;

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepo;
    @Mock
    private VilkaarsresultatService vilkaarsresultatService;

    private final FakeUnleash fakeUnleash = new FakeUnleash();


    private BehandlingsresultatService behandlingsresultatService;

    @BeforeEach
    public void setUp() {
        behandlingsresultatService = spy(new BehandlingsresultatService(behandlingsresultatRepo, vilkaarsresultatService, fakeUnleash));
    }

    @Test
    void tømBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAvklartefakta(new HashSet<>(Collections.singletonList(new Avklartefakta())));
        behandlingsresultat.setLovvalgsperioder(new HashSet<>(Collections.singletonList(new Lovvalgsperiode())));
        behandlingsresultat.setVilkaarsresultater(new HashSet<>(Collections.singleton(new Vilkaarsresultat())));

        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.of(behandlingsresultat));

        behandlingsresultatService.tømBehandlingsresultat(1L);

        assertThat(behandlingsresultat.getAvklartefakta()).isEmpty();
        assertThat(behandlingsresultat.getLovvalgsperioder()).isEmpty();
        verify(vilkaarsresultatService).tømVilkårForBehandlingsresultat(behandlingsresultat);
    }

    @Test
    void hentBehandlingsresultat_medTomtResultat_forventerException() {
        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.empty());
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> behandlingsresultatService.hentBehandlingsresultat(4L))
            .withMessageContaining("Kan ikke finne");
    }

    @Test
    void hentBehandlingsresultat_returnererBehandlingsresultat() {
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
    void lagreNyttBehandlingsresultat_lagresKorrekt() {
        var behandling = new Behandling();

        behandlingsresultatService.lagreNyttBehandlingsresultat(behandling);

        verify(behandlingsresultatRepo).save(behandlingsresultatCaptor.capture());
        var behandlingsresultat = behandlingsresultatCaptor.getValue();
        assertThat(behandlingsresultat.getBehandling()).isEqualTo(behandling);
        assertThat(behandlingsresultat.getBehandlingsmåte()).isEqualTo(Behandlingsmaate.MANUELT);
        assertThat(behandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.IKKE_FASTSATT);
    }

    @Test
    void replikerBehandlingOgBehandlingsresultat_replikererBehandlingsresultatObjekterOgCollections()
        throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
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

        BehandlingsresultatBegrunnelse behandlingsresultatBegrunnelse = opprettBehandlingsresultatBegrunnelse();
        behandlingsresultat.getBehandlingsresultatBegrunnelser().add(behandlingsresultatBegrunnelse);

        Kontrollresultat kontrollresultat = opprettKontrollresultat();
        behandlingsresultat.getKontrollresultater().add(kontrollresultat);

        Anmodningsperiode anmodningsperiode = opprettAnmodningsperiode();
        behandlingsresultat.getAnmodningsperioder().add(anmodningsperiode);

        Utpekingsperiode utpekingsperiode = opprettUtpekingsperiode();
        behandlingsresultat.getUtpekingsperioder().add(utpekingsperiode);

        doReturn(behandlingsresultat).when(behandlingsresultatService).hentBehandlingsresultat(1L);

        behandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingsreplika);

        ArgumentCaptor<Behandlingsresultat> captor = ArgumentCaptor.forClass(Behandlingsresultat.class);
        verify(behandlingsresultatRepo).save(captor.capture());
        Behandlingsresultat behandlingsresultatreplika = captor.getValue();

        assertThat(behandlingsresultatreplika)
            .matches(b -> b.getId() == null)
            .matches(b -> b.getBehandling().equals(behandlingsreplika))
            .matches(b -> b.getBehandlingsmåte().equals(behandlingsresultat.getBehandlingsmåte()))
            .matches(b -> b.getType().equals(behandlingsresultat.getType()))
            .matches(b -> b.getVedtakMetadata() == null);

        assertThat(behandlingsresultatreplika.getLovvalgsperioder())
            .singleElement()
            .matches(l -> l.getId() == null)
            .matches(l -> l.getId() == null)
            .matches(l -> l.getFom() != null)
            .matches(l -> l.getTom() != null)
            .matches(l -> l.getMedlPeriodeID() != null)
            .matches(l -> l.getBehandlingsresultat() == behandlingsresultatreplika)
            .matches(l -> l.getDekning().equals(Trygdedekninger.FULL_DEKNING_EOSFO));

        assertThat(behandlingsresultatreplika.getAnmodningsperioder())
            .singleElement()
            .matches(a -> a.getId() == null)
            .matches(a -> a.getFom() != null)
            .matches(a -> a.getTom() != null)
            .matches(a -> a.getLovvalgsland() == Landkoder.SE)
            .matches(a -> a.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1)
            .matches(a -> a.getAnmodningsperiodeSvar() == null)
            .matches(a -> !a.erSendtUtland())
            .matches(a -> a.getBehandlingsresultat() == behandlingsresultatreplika)
            .matches(a -> a.getDekning().equals(Trygdedekninger.FULL_DEKNING_EOSFO));

        assertThat(behandlingsresultatreplika.getUtpekingsperioder())
            .singleElement()
            .matches(u -> u.getId() == null)
            .matches(u -> u.getFom() != null)
            .matches(u -> u.getTom() != null)
            .matches(u -> u.getLovvalgsland() == Landkoder.SE)
            .matches(u -> u.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A)
            .matches(u -> u.getSendtUtland() == null)
            .matches(u -> u.getBehandlingsresultat() == behandlingsresultatreplika);

        assertThat(behandlingsresultatreplika.getAvklartefakta())
            .singleElement()
            .matches(a -> a.getId() == null)
            .matches(a -> a.getBehandlingsresultat() == behandlingsresultatreplika)
            .matches(a -> a.getFakta().equals("fakta"))
            .matches(a -> a.getType().equals(Avklartefaktatyper.ARBEIDSLAND))
            .extracting(Avklartefakta::getRegistreringer)
            .matches(r -> r.size() == 1)
            .extracting(r -> r.iterator().next())
            .matches(ar -> ar.getAvklartefakta().equals(behandlingsresultatreplika.getAvklartefakta().iterator().next()))
            .matches(ar -> ar.getBegrunnelseKode().equals(AVKLARTEFAKTA_REGISTRERING_BEGRUNNELSE_KODE));

        assertThat(behandlingsresultatreplika.getVilkaarsresultater())
            .singleElement()
            .matches(v -> v.getId() == null)
            .matches(v -> v.getBehandlingsresultat() == behandlingsresultatreplika)
            .matches(v -> v.getBegrunnelseFritekst().equals("fritekst"))
            .matches(v -> v.getBegrunnelseFritekstEessi().equals("free text"))
            .extracting(Vilkaarsresultat::getBegrunnelser)
            .matches(vb -> vb.size() == 1)
            .extracting(vb -> vb.iterator().next())
            .matches(vb -> vb.getId() == null)
            .matches(vb -> vb.getKode().equals("kode"));

        assertThat(behandlingsresultatreplika.getBehandlingsresultatBegrunnelser())
            .singleElement()
            .matches(b -> b.getId() == null)
            .matches(b -> b.getBehandlingsresultat() == behandlingsresultatreplika)
            .matches(b -> b.getKode().equals("begrunnelsekode"));

        assertThat(behandlingsresultatreplika.getKontrollresultater())
            .singleElement()
            .matches(k -> k.getId() == null)
            .matches(k -> k.getBehandlingsresultat() == behandlingsresultatreplika)
            .matches(k -> k.getBegrunnelse() == Kontroll_begrunnelser.FEIL_I_PERIODEN);

        assertThat(behandlingsresultatreplika.getUtfallRegistreringUnntak()).isNull();
        assertThat(behandlingsresultatreplika.getUtfallUtpeking()).isNull();
    }

    @Test
    void replikerBehandlingsresultat_toggleEnabled_replikererBehandlingsresultatUtenBehandlingsresultattype()
        throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        fakeUnleash.enable("melosys.ikke_kopier_behandlingsresultattype");

        Behandling tidligsteInaktiveBehandling = new Behandling();
        tidligsteInaktiveBehandling.setId(1L);

        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling);
        doReturn(behandlingsresultat).when(behandlingsresultatService).hentBehandlingsresultat(1L);


        behandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, new Behandling());


        ArgumentCaptor<Behandlingsresultat> captor = ArgumentCaptor.forClass(Behandlingsresultat.class);
        verify(behandlingsresultatRepo).save(captor.capture());
        Behandlingsresultat behandlingsresultatreplika = captor.getValue();

        assertThat(behandlingsresultatreplika.getType()).isEqualTo(Behandlingsresultattyper.IKKE_FASTSATT);
    }

    @Test
    void oppdaterBehandlingsresultattype_idEksisterer_oppdatererBehandlingsresultattype() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        doReturn(Optional.of(behandlingsresultat)).when(behandlingsresultatRepo).findById(1L);

        behandlingsresultatService.oppdaterBehandlingsresultattype(1L, Behandlingsresultattyper.IKKE_FASTSATT);

        assertThat(behandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.IKKE_FASTSATT);
        verify(behandlingsresultatRepo).save(behandlingsresultat);
    }

    @Test
    void oppdaterBehandlingsresultattype_idEksistererIkke_gjørIngenting() {
        behandlingsresultatService.oppdaterBehandlingsresultattype(1L, Behandlingsresultattyper.IKKE_FASTSATT);
        verify(behandlingsresultatRepo).findById(1L);
        verify(behandlingsresultatRepo, never()).save(any());
    }

    @Test
    void oppdaterBehandlingsmaate_bhmåteUdefinert_verifiserOppdatert() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandlingsmåte(Behandlingsmaate.MANUELT);
        when(behandlingsresultatRepo.findById(anyLong())).thenReturn(Optional.of(behandlingsresultat));
        behandlingsresultatService.oppdaterBehandlingsMaate(1L, Behandlingsmaate.AUTOMATISERT);
        verify(behandlingsresultatRepo).save(behandlingsresultat);
        assertThat(behandlingsresultat.getBehandlingsmåte()).isEqualTo(Behandlingsmaate.AUTOMATISERT);
    }

    @Test
    void oppdaterUtfallRegistreringUnntak_ikkeSatt_lagres() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepo.findById(1L)).thenReturn(Optional.of(behandlingsresultat));
        behandlingsresultatService.oppdaterUtfallRegistreringUnntak(1, Utfallregistreringunntak.GODKJENT);
        verify(behandlingsresultatRepo).save(behandlingsresultat);
    }

    @Test
    void oppdaterUtfallRegistreringUnntak_alleredeSatt_kasterException() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setUtfallRegistreringUnntak(Utfallregistreringunntak.GODKJENT);
        when(behandlingsresultatRepo.findById(1L)).thenReturn(Optional.of(behandlingsresultat));
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingsresultatService.oppdaterUtfallRegistreringUnntak(1, Utfallregistreringunntak.GODKJENT))
            .withMessageContaining("Utfall for registrering av unntak er allerede satt for behandlingsresultat");
    }

    @Test
    void oppdaterBegrunnelser_enBegrunnelse_blirLagret() {
        var behandlingsresultatBegrunnelse = new BehandlingsresultatBegrunnelse();
        behandlingsresultatBegrunnelse.setKode("koden");

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepo.findById(1L)).thenReturn(Optional.of(behandlingsresultat));

        behandlingsresultatService.oppdaterBegrunnelser(1L, Set.of(behandlingsresultatBegrunnelse), "fri");

        verify(behandlingsresultatRepo).save(behandlingsresultat);
        assertThat(behandlingsresultatBegrunnelse.getBehandlingsresultat()).isEqualTo(behandlingsresultat);
    }

    @Test
    void oppdaterFritekster_altOk_blirLagret() {
        ArgumentCaptor<Behandlingsresultat> captor = ArgumentCaptor.forClass(Behandlingsresultat.class);
        var behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepo.findById(1L)).thenReturn(Optional.of(behandlingsresultat));

        behandlingsresultatService.oppdaterFritekster(
            1L, "fritekst for begrunnelse", "fritekst for innledning");


        verify(behandlingsresultatRepo).save(captor.capture());
        Behandlingsresultat capturedBehandlingsresultat = captor.getValue();
        assertThat(capturedBehandlingsresultat.getBegrunnelseFritekst()).isEqualTo("fritekst for begrunnelse");
        assertThat(capturedBehandlingsresultat.getInnledningFritekst()).isEqualTo("fritekst for innledning");
    }

    private Lovvalgsperiode opprettLovvalgsperiode() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setId(32L);
        lovvalgsperiode.setBehandlingsresultat(opprettTomtBehandlingsresultatMedId());
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusMonths(2));
        lovvalgsperiode.setMedlPeriodeID(777L);
        return lovvalgsperiode;
    }

    private Anmodningsperiode opprettAnmodningsperiode() {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setId(32L);
        anmodningsperiode.setFom(LocalDate.now());
        anmodningsperiode.setTom(LocalDate.now().plusYears(1L));
        anmodningsperiode.setLovvalgsland(Landkoder.SE);
        anmodningsperiode.setUnntakFraLovvalgsland(Landkoder.NO);
        anmodningsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);
        anmodningsperiode.setUnntakFraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        anmodningsperiode.setTilleggsbestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1);
        anmodningsperiode.setBehandlingsresultat(opprettTomtBehandlingsresultatMedId());
        anmodningsperiode.setSendtUtland(true);
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());
        anmodningsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
        return anmodningsperiode;
    }

    private Utpekingsperiode opprettUtpekingsperiode() {
        Utpekingsperiode utpekingsperiode = new Utpekingsperiode(
            LocalDate.now(), LocalDate.now().plusYears(1), Landkoder.SE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);
        utpekingsperiode.setId(11111L);
        utpekingsperiode.setMedlPeriodeID(1242L);
        utpekingsperiode.setSendtUtland(LocalDate.now());
        return utpekingsperiode;
    }

    private Avklartefakta opprettAvklartefakta() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setId(32L);
        avklartefakta.setBehandlingsresultat(opprettTomtBehandlingsresultatMedId());
        avklartefakta.setFakta("fakta");
        avklartefakta.setType(Avklartefaktatyper.ARBEIDSLAND);
        AvklartefaktaRegistrering avklartefaktaRegistrering = new AvklartefaktaRegistrering();
        avklartefaktaRegistrering.setBegrunnelseKode(AVKLARTEFAKTA_REGISTRERING_BEGRUNNELSE_KODE);
        avklartefakta.getRegistreringer().add(avklartefaktaRegistrering);
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
        vilkaarsresultat.setBegrunnelseFritekstEessi("free text");

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

        behandlingsresultat.setUtfallUtpeking(Utfallregistreringunntak.IKKE_GODKJENT);
        behandlingsresultat.setUtfallRegistreringUnntak(Utfallregistreringunntak.IKKE_GODKJENT);

        return behandlingsresultat;
    }
}
