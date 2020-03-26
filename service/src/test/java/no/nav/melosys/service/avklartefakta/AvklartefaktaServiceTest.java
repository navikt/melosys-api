package no.nav.melosys.service.avklartefakta;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
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
            .thenReturn(Set.of(lagAvklartefakta(Avklartefaktatyper.ARBEIDSLAND, "NO", "TRUE"),
                lagAvklartefakta(Avklartefaktatyper.ARBEIDSLAND, "SE", "TRUE")));

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
        Optional<Avklartefakta> avklartefaktaFraDb = Optional.of(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        Optional<Maritimtyper> maritimType = avklartefaktaService.hentMaritimType(1L);
        assertThat(maritimType).isPresent().get().isEqualTo(Maritimtyper.SOKKEL);
    }

    @Test
    public void hentMaritimType_medSkipTekst_foventerSkipType() {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("SKIP");
        Optional<Avklartefakta> avklartefaktaFraDb = Optional.of(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        Optional<Maritimtyper> maritimType = avklartefaktaService.hentMaritimType(1L);
        assertThat(maritimType).isPresent().get().isEqualTo(Maritimtyper.SKIP);
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
        Map<String, AvklartMaritimtArbeid> avklarteMaritimeArbeid = avklartefaktaService.hentAlleMaritimeAvklartfakta(1L);
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

        Map<String, AvklartMaritimtArbeid> avklarteMaritimeArbeid = avklartefaktaService.hentAlleMaritimeAvklartfakta(1L);
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