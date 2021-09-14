package no.nav.melosys.service.avklartefakta;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Bostedsland;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeBarn;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl.SAMBOER_UTEN_FELLES_BARN;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvklartefaktaServiceTest {
    @Mock
    private AvklarteFaktaRepository avklarteFaktaRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private AvklartefaktaDtoKonverterer avklartefaktaDtoKonverterer;

    private AvklartefaktaService avklartefaktaService;

    @Captor
    private ArgumentCaptor<Avklartefakta> captor;

    @BeforeEach
    public void setUp() {
        avklartefaktaService = new AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, avklartefaktaDtoKonverterer);
    }

    @Test
    void hentAvklartefakta() {
        Avklartefakta avklartefakta = lagAvklartefakta(Avklartefaktatyper.ARBEIDSLAND, "NO", "TRUE");
        Set<Avklartefakta> avklartefaktaSet = new HashSet<>(List.of(avklartefakta));

        when(avklarteFaktaRepository.findByBehandlingsresultatId(anyLong())).thenReturn(avklartefaktaSet);

        Optional<AvklartefaktaDto> dtoOpt = avklartefaktaService.hentAlleAvklarteFakta(1L).stream()
            .findFirst();
        assertThat(dtoOpt).isPresent();

        AvklartefaktaDto dto = dtoOpt.get();
        assertThat(avklartefakta.getReferanse()).isEqualTo(dto.getReferanse());
        assertThat(avklartefakta.getSubjekt()).isEqualTo(dto.getSubjektID());
        assertThat(List.of(avklartefakta.getFakta())).isEqualTo(dto.getFakta());
        assertThat(avklartefakta.getType()).isEqualTo(dto.getAvklartefaktaType());
        assertThat(avklartefakta.getRegistreringer().stream().map(AvklartefaktaRegistrering::getBegrunnelseKode).collect(Collectors.toList()))
            .isEqualTo(dto.getBegrunnelseKoder());
        assertThat(avklartefakta.getBegrunnelseFritekst()).isEqualTo(dto.getBegrunnelseFritekst());
    }

    @Test
    void lagreAvklarteFakta() {
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
    void hentAlleAvklarteArbeidsland() {
        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(anyLong(), eq(Avklartefaktatyper.ARBEIDSLAND)))
            .thenReturn(Set.of(lagAvklartefakta(Avklartefaktatyper.ARBEIDSLAND, null, "NO"),
                lagAvklartefakta(Avklartefaktatyper.ARBEIDSLAND, null, "SE")));

        Set<Landkoder> landkoder = avklartefaktaService.hentAlleAvklarteArbeidsland(1L);
        assertThat(landkoder).containsExactlyInAnyOrder(Landkoder.NO, Landkoder.SE);
    }

    @Test
    void hentBostedsland() {
        Bostedsland bostedsland = new Bostedsland(Landkoder.NO);
        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(anyLong(), eq(Avklartefaktatyper.BOSTEDSLAND)))
            .thenReturn(Set.of(lagAvklartefakta(Avklartefaktatyper.BOSTEDSLAND, null, bostedsland.getLandkode())));

        Optional<Bostedsland> landkoder = avklartefaktaService.hentBostedland(1L);
        assertThat(landkoder).isPresent().get().isEqualTo(bostedsland);
    }

    @Test
    void hentYrkesgruppe_forventerOrdinær() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("ORDINAER");
        Optional<Avklartefakta> avklartefaktaSet = Optional.of(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaSet);

        Optional<Yrkesgrupper> yrkesgruppeType = avklartefaktaService.finnYrkesGruppe(1L);
        assertThat(yrkesgruppeType).isPresent().get().isEqualTo(Yrkesgrupper.ORDINAER);
    }

    @Test
    void hentYrkesgruppe_forventerFlyende() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("YRKESAKTIV_FLYVENDE");
        Optional<Avklartefakta> avklartefaktaSet = Optional.of(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaSet);

        Optional<Yrkesgrupper> yrkesgruppeType = avklartefaktaService.finnYrkesGruppe(1L);
        assertThat(yrkesgruppeType).isPresent().get().isEqualTo(Yrkesgrupper.FLYENDE_PERSONELL);
    }

    @Test
    void hentYrkesgruppe_forventerSokkelSkip() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("SOKKEL_ELLER_SKIP");
        Optional<Avklartefakta> avklartefaktaFraDb = Optional.of(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        Optional<Yrkesgrupper> yrkesgruppeType = avklartefaktaService.finnYrkesGruppe(1L);
        assertThat(yrkesgruppeType).isPresent().get().isEqualTo(Yrkesgrupper.SOKKEL_ELLER_SKIP);
    }

    @Test
    void hentYrkesgruppe_utenYrkesgruppe_forventerFeil() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("IKKE_YRKESAKTIV");
        Optional<Avklartefakta> avklartefaktaFraDb = Optional.of(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> avklartefaktaService.finnYrkesGruppe(1L))
            .withMessageContaining("Finner ingen yrkesgruppe");
    }

    @Test
    void hentMarginaltArbeid_medEttLandMedMarginaltArbeid_girMarginaltArbeid() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("MARGINALT_ARBEID");
        Set<Avklartefakta> avklartefaktaFraDb = Collections.singleton(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(anyLong(), eq(Avklartefaktatyper.MARGINALT_ARBEID), eq("TRUE"))).thenReturn(avklartefaktaFraDb);

        boolean harMarginaltArbeid = avklartefaktaService.harMarginaltArbeid(1L);
        assertThat(harMarginaltArbeid).isTrue();
    }

    @Test
    void hentMarginaltArbeid_ingenLandMedMarginaltArbeid_girIkkeMarginaltArbeid() {
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(anyLong(), any(), any())).thenReturn(Collections.emptySet());

        boolean harMarginaltArbeid = avklartefaktaService.harMarginaltArbeid(1L);
        assertThat(harMarginaltArbeid).isFalse();
    }

    @Test
    void hentMaritimType_medSokkelTekst_foventerSokkelType() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("SOKKEL");
        Set<Avklartefakta> avklartefaktaFraDb = Set.of(avklartefakta);
        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        Set<Maritimtyper> maritimTyper = avklartefaktaService.hentMaritimTyper(1L);
        assertThat(maritimTyper).isNotEmpty();
        assertThat(maritimTyper.iterator().next()).isEqualTo(Maritimtyper.SOKKEL);
    }

    @Test
    void hentMaritimType_medSkipTekst_foventerSkipType() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("SKIP");
        Set<Avklartefakta> avklartefaktaFraDb = Set.of(avklartefakta);
        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        Set<Maritimtyper> maritimTyper = avklartefaktaService.hentMaritimTyper(1L);
        assertThat(maritimTyper).isNotEmpty();
        assertThat(maritimTyper.iterator().next()).isEqualTo(Maritimtyper.SKIP);
    }

    @Test
    void hentInformertMyndighet_avklartFaktaErSverige_forventSverige() {
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
    void hentAvklartMaritimeAvklartfakta_medAvklartSokkel_girAvklartMaritimtArbeid() {
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
    void hentAvklartMaritimeAvklartfakta_medAvklartSkip_girAvklartMaritimtArbeid() {
        Set<Avklartefakta> alleMaritimeFakta = lagAlleMaritimeAvklartefakta("Stena Don", "SOKKEL", "SE");
        alleMaritimeFakta.addAll(lagAlleMaritimeAvklartefakta("Seven Kestrel", "SKIP", "GB"));
        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndTypeIn(anyLong(), anySet())).thenReturn(alleMaritimeFakta);

        Map<String, AvklartMaritimtArbeid> avklarteMaritimeArbeid = avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(1L);
        assertThat(avklarteMaritimeArbeid).hasSize(2);
    }

    @Test
    void testAvklarteOrganisasjoner() {
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
    void leggTilRegistrering_forventLagret() throws Exception {

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
    void hentAvklarteMedfølgendeBarn_medOgUtenMedfølgendeBarn_girForventedeVerdier() {
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
    void hentAvklartMedfølgendeEktefelle_medMedfølgendeEktefelle() {
        Avklartefakta ektefelleOmfattet = lagAvklartefakta(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER,
            "omfattet", "TRUE");

        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(anyLong(), any(Avklartefaktatyper.class)))
            .thenReturn(Set.of(ektefelleOmfattet));

        AvklarteMedfolgendeFamilie avklarteMedfølgendeEktefelle = avklartefaktaService.hentAvklarteMedfølgendeEktefelle(1L);

        assertThat(avklarteMedfølgendeEktefelle.getFamilieOmfattetAvNorskTrygd())
            .extracting("uuid").containsExactly("omfattet");
    }

    @Test
    void hentAvklartMedfølgendeEktefelle_utenMedfølgendeEktefelle() {
        Avklartefakta ektefelleOmfattet = lagAvklartIkkeOmfattetEktefelle("ikkeOmfattet",
            SAMBOER_UTEN_FELLES_BARN, "TRUE");

        when(avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(anyLong(), any(Avklartefaktatyper.class)))
            .thenReturn(Set.of(ektefelleOmfattet));

        AvklarteMedfolgendeFamilie avklarteMedfølgendeEktefelle = avklartefaktaService.hentAvklarteMedfølgendeEktefelle(1L);

        assertThat(avklarteMedfølgendeEktefelle.getFamilieIkkeOmfattetAvNorskTrygd().iterator().next())
            .extracting("uuid", "begrunnelse")
            .containsExactlyInAnyOrder("ikkeOmfattet", SAMBOER_UTEN_FELLES_BARN.name());
    }

    private static Avklartefakta lagAvklartIkkeOmfattetBarn(String subjectID, Medfolgende_barn_begrunnelser begrunnelse, String begrunnelseFritekst) {
        Avklartefakta avklartefakta = lagAvklartefakta(Avklartefaktatyper.VURDERING_LOVVALG_BARN, subjectID, "FALSE");
        avklartefakta.setBegrunnelseFritekst(begrunnelseFritekst);

        AvklartefaktaRegistrering avklartefaktaRegistrering = new AvklartefaktaRegistrering();
        avklartefaktaRegistrering.setBegrunnelseKode(begrunnelse.getKode());
        avklartefakta.setRegistreringer(Set.of(avklartefaktaRegistrering));
        return avklartefakta;
    }

    private static Avklartefakta lagAvklartIkkeOmfattetEktefelle(String subjectID, Medfolgende_ektefelle_samboer_begrunnelser_ftrl begrunnelse, String begrunnelseFritekst) {
        Avklartefakta avklartefakta = lagAvklartefakta(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, subjectID, "FALSE");
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
