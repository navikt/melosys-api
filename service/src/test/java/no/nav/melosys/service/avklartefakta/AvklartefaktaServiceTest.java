package no.nav.melosys.service.avklartefakta;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.familie.AvklarteMedfolgendeBarn;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.familie.OmfattetFamilie;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklartefaktaServiceTest {
    @Mock
    private AvklarteFaktaRepository avklarteFaktaRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private AvklartefaktaDtoKonverterer avklartefaktaDtoKonverterer;

    private AvklartefaktaService avklartefaktaService;

    @Captor
    private ArgumentCaptor<Avklartefakta> captor;

    @Before
    public void setUp() {
        avklartefaktaService = new AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, avklartefaktaDtoKonverterer);
    }

    @Test
    public void hentAvklartefakta() {
        Avklartefakta avklartefakta = lagAvklartefakta(Avklartefaktatyper.ARBEIDSLAND, "NO", "TRUE");
        Set<Avklartefakta> avklartefaktaSet = new HashSet<>(List.of(avklartefakta));

        when(avklarteFaktaRepository.findByBehandlingsresultatId(anyLong())).thenReturn(avklartefaktaSet);

        Optional<AvklartefaktaDto> dtoOpt = avklartefaktaService.hentAlleAvklarteFakta(1L).stream()
            .findFirst();
        assertThat(dtoOpt).isPresent();

        AvklartefaktaDto dto = dtoOpt.get();
        assertEquals(avklartefakta.getReferanse(), dto.getReferanse());
        assertEquals(avklartefakta.getSubjekt(), dto.getSubjektID());
        assertEquals(List.of(avklartefakta.getFakta()), dto.getFakta());
        assertEquals(avklartefakta.getType(), dto.getAvklartefaktaType());
        assertEquals(avklartefakta.getRegistreringer().stream().map(AvklartefaktaRegistrering::getBegrunnelseKode)
            .collect(Collectors.toList()), dto.getBegrunnelseKoder());
        assertEquals(avklartefakta.getBegrunnelseFritekst(), dto.getBegrunnelseFritekst());
    }

    @Test
    public void lagreAvklarteFakta() throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepository.findById(anyLong())).thenReturn(Optional.of(behandlingsresultat));
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("test fakta");
        HashSet<AvklartefaktaDto> avklartefaktaDtoer = new HashSet<>();
        avklartefaktaDtoer.add(new AvklartefaktaDto(avklartefakta));
        avklartefaktaService.lagreAvklarteFakta(123L, avklartefaktaDtoer);
        verify(avklarteFaktaRepository).deleteByBehandlingsresultatId(anyLong());
        verify(avklarteFaktaRepository).flush();
        verify(avklartefaktaDtoKonverterer).opprettAvklartefaktaFraDto(any(), any());
        verify(avklarteFaktaRepository).saveAll(any());
    }

    @Test
    public void hentAlleAvklarteArbeidsland() {
        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(anyLong(), eq(Avklartefaktatyper.ARBEIDSLAND)))
            .thenReturn(Set.of(lagAvklartefakta(Avklartefaktatyper.ARBEIDSLAND, null, "NO"),
                lagAvklartefakta(Avklartefaktatyper.ARBEIDSLAND, null, "SE")));

        Set<Landkoder> landkoder = avklartefaktaService.hentAlleAvklarteArbeidsland(1L);
        assertThat(landkoder).containsExactlyInAnyOrder(Landkoder.NO, Landkoder.SE);
    }

    @Test
    public void hentBostedsland() {
        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(anyLong(), eq(Avklartefaktatyper.BOSTEDSLAND)))
            .thenReturn(Set.of(lagAvklartefakta(Avklartefaktatyper.BOSTEDSLAND, null, "NO")));

        Optional<Landkoder> landkoder = avklartefaktaService.hentBostedland(1L);
        assertThat(landkoder).isPresent().get().isEqualTo(Landkoder.NO);
    }

    @Test
    public void hentYrkesgruppe_forventerOrdinær() throws TekniskException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("ORDINAER");
        Optional<Avklartefakta> avklartefaktaSet = Optional.of(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaSet);

        Optional<Yrkesgrupper> yrkesgruppeType = avklartefaktaService.finnYrkesGruppe(1L);
        assertThat(yrkesgruppeType).isPresent().get().isEqualTo(Yrkesgrupper.ORDINAER);
    }

    @Test
    public void hentYrkesgruppe_forventerFlyende() throws TekniskException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("YRKESAKTIV_FLYVENDE");
        Optional<Avklartefakta> avklartefaktaSet = Optional.of(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaSet);

        Optional<Yrkesgrupper> yrkesgruppeType = avklartefaktaService.finnYrkesGruppe(1L);
        assertThat(yrkesgruppeType).isPresent().get().isEqualTo(Yrkesgrupper.FLYENDE_PERSONELL);
    }

    @Test
    public void hentYrkesgruppe_forventerSokkelSkip() throws TekniskException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("SOKKEL_ELLER_SKIP");
        Optional<Avklartefakta> avklartefaktaFraDb = Optional.of(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        Optional<Yrkesgrupper> yrkesgruppeType = avklartefaktaService.finnYrkesGruppe(1L);
        assertThat(yrkesgruppeType).isPresent().get().isEqualTo(Yrkesgrupper.SOKKEL_ELLER_SKIP);
    }

    @Test(expected = TekniskException.class)
    public void hentYrkesgruppe_utenYrkesgruppe_forventerFeil() throws TekniskException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("IKKE_YRKESAKTIV");
        Optional<Avklartefakta> avklartefaktaFraDb = Optional.of(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        avklartefaktaService.finnYrkesGruppe(1L);
    }

    @Test
    public void hentMarginaltArbeid_medEttLandMedMarginaltArbeid_girMarginaltArbeid() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("MARGINALT_ARBEID");
        Set<Avklartefakta> avklartefaktaFraDb = Collections.singleton(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(anyLong(), eq(Avklartefaktatyper.MARGINALT_ARBEID), eq("TRUE"))).thenReturn(avklartefaktaFraDb);

        boolean harMarginaltArbeid = avklartefaktaService.harMarginaltArbeid(1L);
        assertThat(harMarginaltArbeid).isTrue();
    }

    @Test
    public void hentMarginaltArbeid_ingenLandMedMarginaltArbeid_girIkkeMarginaltArbeid() {
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(anyLong(), any(), any())).thenReturn(Collections.emptySet());

        boolean harMarginaltArbeid = avklartefaktaService.harMarginaltArbeid(1L);
        assertThat(harMarginaltArbeid).isFalse();
    }

    @Test
    public void hentMaritimType_medSokkelTekst_foventerSokkelType() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("SOKKEL");
        Set<Avklartefakta> avklartefaktaFraDb = Set.of(avklartefakta);
        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        Set<Maritimtyper> maritimTyper = avklartefaktaService.hentMaritimTyper(1L);
        assertThat(maritimTyper).isNotEmpty();
        assertThat(maritimTyper.iterator().next()).isEqualTo(Maritimtyper.SOKKEL);
    }

    @Test
    public void hentMaritimType_medSkipTekst_foventerSkipType() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("SKIP");
        Set<Avklartefakta> avklartefaktaFraDb = Set.of(avklartefakta);
        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        Set<Maritimtyper> maritimTyper = avklartefaktaService.hentMaritimTyper(1L);
        assertThat(maritimTyper).isNotEmpty();
        assertThat(maritimTyper.iterator().next()).isEqualTo(Maritimtyper.SKIP);
    }

    @Test
    public void hentInformertMyndighet_avklartFaktaErSverige_forventSverige() {
        Avklartefakta valgtMyndighetFakta = new Avklartefakta();
        valgtMyndighetFakta.setSubjekt(Landkoder.SE.getKode());
        valgtMyndighetFakta.setType(Avklartefaktatyper.INFORMERT_MYNDIGHET);

        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(anyLong(), eq(Avklartefaktatyper.INFORMERT_MYNDIGHET), eq("TRUE")))
            .thenReturn(Set.of(valgtMyndighetFakta));

        assertThat(avklartefaktaService.hentInformertMyndighet(1L)).isPresent().hasValue(Landkoder.SE);
    }

    public static Set<Avklartefakta> lagAlleMaritimeAvklartefakta(String navn, String maritimType, String landkode) {
        Avklartefakta avklartSokkel = AvklartMaritimtArbeidTest.lagAvklartefaktaSokkelSkip(navn, maritimType);
        Avklartefakta avklartArbeidsland = AvklartMaritimtArbeidTest.lagAvklartefaktaArbeidsland(navn, landkode);
        return new HashSet<>(Arrays.asList(avklartSokkel, avklartArbeidsland));
    }

    @Test
    public void hentAvklartMaritimeAvklartfakta_medAvklartSokkel_girAvklartMaritimtArbeid() {
        Set<Avklartefakta> alleMaritimeFakta = lagAlleMaritimeAvklartefakta("Stena Don", "SOKKEL", "GB");
        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndTypeIn(anyLong(), anySet())).thenReturn(alleMaritimeFakta);
        Map<String, AvklartMaritimtArbeid> avklarteMaritimeArbeid = avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(1L);
        assertThat(avklarteMaritimeArbeid).hasSize(1);

        avklarteMaritimeArbeid.values().forEach(maritimtArbeid -> {
            assertThat(maritimtArbeid.getNavn()).isEqualTo("Stena Don");
            assertThat(maritimtArbeid.getMaritimtype()).isEqualTo(Maritimtyper.SOKKEL);
            assertThat(maritimtArbeid.getLand()).isEqualTo("GB");
        });
    }

    @Test
    public void hentAvklartMaritimeAvklartfakta_medAvklartSkip_girAvklartMaritimtArbeid() {
        Set<Avklartefakta> alleMaritimeFakta = lagAlleMaritimeAvklartefakta("Stena Don", "SOKKEL", "SE");
        alleMaritimeFakta.addAll(lagAlleMaritimeAvklartefakta("Seven Kestrel", "SKIP", "GB"));
        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndTypeIn(anyLong(), anySet())).thenReturn(alleMaritimeFakta);

        Map<String, AvklartMaritimtArbeid> avklarteMaritimeArbeid = avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(1L);
        assertThat(avklarteMaritimeArbeid).hasSize(2);
    }

    @Test
    public void testAvklarteOrganisasjoner() {
        String orgnr1 = "12345678910";
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setType(Avklartefaktatyper.VIRKSOMHET);
        avklartefakta.setFakta("TRUE");
        avklartefakta.setSubjekt(orgnr1);

        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(anyLong(), any(), eq("TRUE")))
            .thenReturn(new HashSet<>(List.of(avklartefakta)));

        Set<String> avklarteOrgnumre = avklartefaktaService.hentAvklarteOrgnrOgUuid(1L);
        assertThat(avklarteOrgnumre).containsOnly(orgnr1);
    }

    @Test
    public void leggTilRegistrering_forventLagret() throws Exception {

        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any(Avklartefaktatyper.class)))
            .thenReturn(Optional.of(new Avklartefakta()));

        avklartefaktaService.leggTilRegistrering(1, Avklartefaktatyper.VURDERING_UNNTAK_PERIODE, "kode");

        verify(avklarteFaktaRepository).save(captor.capture());

        Avklartefakta capturedAvklarteFakta = captor.getValue();

        assertThat(capturedAvklarteFakta.getRegistreringer()).hasSize(1);

        AvklartefaktaRegistrering registrering = capturedAvklarteFakta.getRegistreringer().iterator().next();
        assertThat(registrering.getBegrunnelseKode()).isEqualTo("kode");
    }

    @Test
    public void hentAvklarteMedfølgendeBarn_medOgUtenMedfølgendeBarn_girForventedeVerdier() {
        Avklartefakta barnOmfattet = lagAvklartefakta(Avklartefaktatyper.VURDERING_LOVVALG_BARN,
            "omfattet", "TRUE");
        Avklartefakta barnIkkeOmfattet1 = lagAvklartIkkeOmfattetBarn(
            "ikkeOmfattet1", Medfolgende_barn_begrunnelser.OVER_18_AR, "begrunnelseFritekst");
        Avklartefakta barnIkkeOmfattet2 = lagAvklartIkkeOmfattetBarn(
            "ikkeOmfattet2", Medfolgende_barn_begrunnelser.MANGLER_OPPLYSNINGER, null);

         when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(anyLong(), any(Avklartefaktatyper.class)))
            .thenReturn(Set.of(barnOmfattet, barnIkkeOmfattet1, barnIkkeOmfattet2));

        AvklarteMedfolgendeBarn avklarteMedfølgendeBarn
            = avklartefaktaService.hentAvklarteMedfølgendeBarn(1L);

        assertThat(avklarteMedfølgendeBarn.barnOmfattetAvNorskTrygd)
            .extracting("uuid").containsExactly("omfattet");
        assertThat(avklarteMedfølgendeBarn.barnIkkeOmfattetAvNorskTrygd)
            .extracting("uuid")
            .containsExactlyInAnyOrder("ikkeOmfattet1", "ikkeOmfattet2");
        assertThat(avklarteMedfølgendeBarn.barnIkkeOmfattetAvNorskTrygd)
            .filteredOn(barnIkkeOmfattet -> barnIkkeOmfattet.uuid.equals("ikkeOmfattet1"))
            .extracting("begrunnelse")
            .isEqualTo(List.of(Medfolgende_barn_begrunnelser.OVER_18_AR));
        assertThat(avklarteMedfølgendeBarn.barnIkkeOmfattetAvNorskTrygd)
            .filteredOn(barnIkkeOmfattet -> barnIkkeOmfattet.uuid.equals("ikkeOmfattet2"))
            .extracting("begrunnelse")
            .isEqualTo(List.of(Medfolgende_barn_begrunnelser.MANGLER_OPPLYSNINGER));
        assertThat(avklarteMedfølgendeBarn.hentBegrunnelseFritekst().orElse("")).isEqualTo("begrunnelseFritekst");
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklartefakta_ikkeOmfattetFamilie_lagresKorrekt() throws FunksjonellException {
        Set<IkkeOmfattetFamilie> ikkeOmfattetEktefelleSamboers = Set.of(
            new IkkeOmfattetFamilie("uuid1", "SAMBOER_UTEN_FELLES_BARN", "fritekstForUuid7"));
        AvklarteMedfolgendeFamilie avklarteMedfolgendeEktefelleSamboer =
            new AvklarteMedfolgendeFamilie(Set.of(), ikkeOmfattetEktefelleSamboers);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);

        Avklartefakta forventetAvklartefakta = new Avklartefakta();
        forventetAvklartefakta.setSubjekt("uuid1");
        forventetAvklartefakta.setType(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER);
        forventetAvklartefakta.setReferanse(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.getKode());
        forventetAvklartefakta.setFakta("FALSE");
        forventetAvklartefakta.setBegrunnelseFritekst("fritekstForUuid7");
        forventetAvklartefakta.setBehandlingsresultat(behandlingsresultat);

        AvklartefaktaRegistrering forventetRegistrering = new AvklartefaktaRegistrering();
        forventetRegistrering.setAvklartefakta(forventetAvklartefakta);
        forventetRegistrering.setBegrunnelseKode("SAMBOER_UTEN_FELLES_BARN");

        when(behandlingsresultatRepository.findById(1L)).thenReturn(Optional.of(behandlingsresultat));

        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), eq(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER)))
            .thenReturn(Optional.of(forventetAvklartefakta));

        avklartefaktaService.lagreMedfolgendeFamilieSomAvklartefakta(
            new AvklarteMedfolgendeFamilie(Set.of(), Set.of()), avklarteMedfolgendeEktefelleSamboer, 1L);

        verify(avklarteFaktaRepository, times(2)).save(captor.capture());

        List<Avklartefakta> capturedAvklarteFakta = captor.getAllValues();
        assertEquals(capturedAvklarteFakta.size(), 2);
        Avklartefakta utenRegistrering = capturedAvklarteFakta.get(0);
        Avklartefakta medRegistrering = capturedAvklarteFakta.get(1);

        assertEquals(utenRegistrering.getSubjekt(), forventetAvklartefakta.getSubjekt());
        assertEquals(utenRegistrering.getType(), forventetAvklartefakta.getType());
        assertEquals(utenRegistrering.getReferanse(), forventetAvklartefakta.getReferanse());
        assertEquals(utenRegistrering.getFakta(), forventetAvklartefakta.getFakta());
        assertEquals(utenRegistrering.getBegrunnelseFritekst(), forventetAvklartefakta.getBegrunnelseFritekst());
        assertEquals(utenRegistrering.getBehandlingsresultat(), forventetAvklartefakta.getBehandlingsresultat());

        assertEquals(medRegistrering.getSubjekt(), forventetAvklartefakta.getSubjekt());
        assertEquals(medRegistrering.getType(), forventetAvklartefakta.getType());
        assertEquals(medRegistrering.getReferanse(), forventetAvklartefakta.getReferanse());
        assertEquals(medRegistrering.getFakta(), forventetAvklartefakta.getFakta());
        assertEquals(medRegistrering.getBegrunnelseFritekst(), forventetAvklartefakta.getBegrunnelseFritekst());
        assertEquals(medRegistrering.getBehandlingsresultat(), forventetAvklartefakta.getBehandlingsresultat());
        assertEquals(medRegistrering.getRegistreringer().size(), 1);

        AvklartefaktaRegistrering registreringFraCaptured = medRegistrering.getRegistreringer().iterator().next();
        assertEquals(registreringFraCaptured.getAvklartefakta(), forventetRegistrering.getAvklartefakta());
        assertEquals(registreringFraCaptured.getBegrunnelseKode(), forventetRegistrering.getBegrunnelseKode());
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklartefakta_omfattetFamilie_lagresKorrekt() throws FunksjonellException {
        Set<OmfattetFamilie> omfattetBarn = Set.of(new OmfattetFamilie("uuid1"));
        AvklarteMedfolgendeFamilie avklarteMedfolgendeBarn =
            new AvklarteMedfolgendeFamilie(omfattetBarn, Set.of());
        Set<OmfattetFamilie> omfattetEktefelleSamboer = Set.of(new OmfattetFamilie("uuid2"));
        AvklarteMedfolgendeFamilie avklarteMedfolgendeEktefelleSamboer =
            new AvklarteMedfolgendeFamilie(omfattetEktefelleSamboer, Set.of());

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        when(behandlingsresultatRepository.findById(1L)).thenReturn(Optional.of(behandlingsresultat));

        avklartefaktaService.lagreMedfolgendeFamilieSomAvklartefakta(
            avklarteMedfolgendeBarn, avklarteMedfolgendeEktefelleSamboer, 1L);

        verify(avklarteFaktaRepository, times(2)).save(captor.capture());

        List<Avklartefakta> capturedAvklarteFakta = captor.getAllValues();
        assertEquals(capturedAvklarteFakta.size(), 2);
        Avklartefakta avklartBarn = capturedAvklarteFakta.get(0);
        Avklartefakta avklartEktefelleSamboer = capturedAvklarteFakta.get(1);

        assertEquals(avklartBarn.getSubjekt(), "uuid1");
        assertEquals(avklartBarn.getType(), Avklartefaktatyper.VURDERING_LOVVALG_BARN);
        assertEquals(avklartBarn.getReferanse(), Avklartefaktatyper.VURDERING_LOVVALG_BARN.getKode());
        assertEquals(avklartBarn.getFakta(), "TRUE");
        assertNull(avklartBarn.getBegrunnelseFritekst());
        assertEquals(avklartBarn.getBehandlingsresultat(), behandlingsresultat);
        assertTrue(avklartBarn.getRegistreringer().isEmpty());

        assertEquals(avklartEktefelleSamboer.getSubjekt(), "uuid2");
        assertEquals(avklartEktefelleSamboer.getType(), Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER);
        assertEquals(avklartEktefelleSamboer.getReferanse(), Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.getKode());
        assertEquals(avklartEktefelleSamboer.getFakta(), "TRUE");
        assertNull(avklartEktefelleSamboer.getBegrunnelseFritekst());
        assertEquals(avklartEktefelleSamboer.getBehandlingsresultat(), behandlingsresultat);
        assertTrue(avklartEktefelleSamboer.getRegistreringer().isEmpty());
    }

    private static Avklartefakta lagAvklartIkkeOmfattetBarn(String subjectID, Medfolgende_barn_begrunnelser begrunnelse, String begrunnelseFritekst) {
        Avklartefakta avklartefakta = lagAvklartefakta(Avklartefaktatyper.VURDERING_LOVVALG_BARN, subjectID, "FALSE");
        avklartefakta.setBegrunnelseFritekst(begrunnelseFritekst);

        AvklartefaktaRegistrering avklartefaktaRegistrering = new AvklartefaktaRegistrering();
        avklartefaktaRegistrering.setBegrunnelseKode(begrunnelse.getKode());
        avklartefakta.setRegistreringer(Set.of(avklartefaktaRegistrering));
        return avklartefakta;
    }

    private static Avklartefakta lagAvklartefakta(Avklartefaktatyper type, String subjektID, String fakta) {
        String referanse = "Referanse";
        String begrunnelsekode = "Begrunnelse";
        String begrunnelsefritekst = "Fritekst";

        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setReferanse(referanse);
        avklartefakta.setSubjekt(subjektID);
        avklartefakta.setFakta(fakta);
        avklartefakta.setType(type);
        avklartefakta.setBegrunnelseFritekst(begrunnelsefritekst);

        AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
        registrering.setAvklartefakta(avklartefakta);
        registrering.setBegrunnelseKode(begrunnelsekode);
        avklartefakta.setRegistreringer(new HashSet<>(List.of(registrering)));
        return avklartefakta;
    }
}