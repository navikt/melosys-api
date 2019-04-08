package no.nav.melosys.service.avklartefakta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.Yrkesgrupper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklartefaktaServiceTest {

    @InjectMocks
    private AvklartefaktaService avklartefaktaService;

    @Mock
    private AvklarteFaktaRepository avklarteFaktaRepository;

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    AvklartefaktaDtoKonverterer avklartefaktaDtoKonverterer;

    @Before
    public void setUp() {
        avklartefaktaService = new AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, avklartefaktaDtoKonverterer);
    }

    @Test
    public void hentAvklartefakta() {
        String referanse = "Referanse";
        String subjektID = "SubjektID";
        String fakta = "NO";
        Avklartefaktatype type = Avklartefaktatype.ARBEIDSLAND;
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
        avklartefakta.setRegistreringer(new HashSet<>(Arrays.asList(registrering)));
        Set<Avklartefakta> avklartefaktaSet = new HashSet<>(Arrays.asList(avklartefakta));

        when(avklarteFaktaRepository.findByBehandlingsresultatId(anyLong())).thenReturn(avklartefaktaSet);

        AvklartefaktaDto dto = avklartefaktaService.hentAlleAvklarteFakta(1L).stream().findFirst().get();

        assertEquals(referanse, dto.getReferanse());
        assertEquals(subjektID, dto.getSubjektID());
        assertEquals(Arrays.asList(fakta), dto.getFakta());
        assertEquals(type, dto.getAvklartefaktaType());
        assertEquals(Arrays.asList(begrunnelsekode), dto.getBegrunnelseKoder());
        assertEquals(begrunnelsefritekst, dto.getBegrunnelseFritekst());
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
        verify(avklarteFaktaRepository).deleteByBehandlingsresultat(any());
        verify(avklartefaktaDtoKonverterer).opprettAvklartefaktaFraDto(any(), any());
        verify(avklarteFaktaRepository).saveAll(any());

    }

    @Test
    public void hentYrkesgruppe_forventerOrdinær() throws TekniskException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("ORDINAER");
        Optional<Avklartefakta> avklartefaktaSet = Optional.ofNullable(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaSet);

        Yrkesgrupper yrkesgruppeType = avklartefaktaService.hentYrkesGruppe(1L);
        assertThat(yrkesgruppeType).isEqualTo(Yrkesgrupper.ORDINAER);
    }

    @Test
    public void hentYrkesgruppe_forventerFlyende() throws TekniskException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("FLYENDE_PERSONELL");
        Optional<Avklartefakta> avklartefaktaSet = Optional.ofNullable(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaSet);

        Yrkesgrupper yrkesgruppeType = avklartefaktaService.hentYrkesGruppe(1L);
        assertThat(yrkesgruppeType).isEqualTo(Yrkesgrupper.FLYENDE_PERSONELL);
    }

    @Test
    public void hentYrkesgruppe_forventerSokkelSkip() throws TekniskException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("SOKKEL_ELLER_SKIP");
        Optional<Avklartefakta> avklartefaktaFraDb = Optional.ofNullable(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        Yrkesgrupper yrkesgruppeType = avklartefaktaService.hentYrkesGruppe(1L);
        assertThat(yrkesgruppeType).isEqualTo(Yrkesgrupper.SOKKEL_ELLER_SKIP);
    }

    @Test(expected = TekniskException.class)
    public void hentYrkesgruppe_utenYrkesgruppe_forventerFeil() throws TekniskException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("IKKE_YRKESAKTIV");
        Optional<Avklartefakta> avklartefaktaFraDb = Optional.ofNullable(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        avklartefaktaService.hentYrkesGruppe(1L);
    }

    @Test
    public void hentMaritimType_medSokkelTekst_foventerSokkelType() throws TekniskException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("SOKKEL");
        Optional<Avklartefakta> avklartefaktaFraDb = Optional.ofNullable(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        Optional<Maritimtyper> maritimType = avklartefaktaService.hentMaritimType(1L);
        assertThat(maritimType.get()).isEqualTo(Maritimtyper.SOKKEL);
    }

    @Test
    public void hentMaritimType_medSkipTekst_foventerSkipType() throws TekniskException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("SKIP");
        Optional<Avklartefakta> avklartefaktaFraDb = Optional.ofNullable(avklartefakta);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), any())).thenReturn(avklartefaktaFraDb);

        Optional<Maritimtyper> maritimType = avklartefaktaService.hentMaritimType(1L);
        assertThat(maritimType.get()).isEqualTo(Maritimtyper.SKIP);
    }

    @Test
    public void testAvklarteOrganisasjoner() {
        String orgnr1 = "12345678910";
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setType(Avklartefaktatype.VIRKSOMHET);
        avklartefakta.setFakta("TRUE");
        avklartefakta.setSubjekt(orgnr1);

        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(anyLong(), any(), eq("TRUE"))).thenReturn(new HashSet<>(Arrays.asList(avklartefakta)));

        Set<String> avklarteOrgnumre = avklartefaktaService.hentAvklarteOrganisasjoner(1L);
        assertThat(avklarteOrgnumre).containsOnly(orgnr1);
    }
}