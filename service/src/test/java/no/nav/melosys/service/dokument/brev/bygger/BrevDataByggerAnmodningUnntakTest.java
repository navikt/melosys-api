package no.nav.melosys.service.dokument.brev.bygger;

import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntak;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static no.nav.melosys.domain.kodeverk.begrunnelser.Art12_2_begrunnelser.UTSENDELSE_OVER_24_MN;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning.KORT_OPPDRAG_RETUR_NORSK_AG;
import static no.nav.melosys.service.BehandlingsgrunnlagStub.lagBehandlingsgrunnlag;
import static no.nav.melosys.service.SaksopplysningStubs.lagArbeidsforholdOpplysninger;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrevDataByggerAnmodningUnntakTest {
    @Mock
    AvklartefaktaService avklartefaktaService;
    @Mock
    RegisterOppslagService registerOppslagService;
    @Mock
    VilkaarsresultatService vilkaarsresultatService;
    @Mock
    LandvelgerService landvelgerService;
    @Mock
    KodeverkService kodeverkService;

    private BrevDataByggerAnmodningUnntak brevDataByggerAnmodningUnntak;

    private final String saksbehandler = "saksbehandler";

    @BeforeEach
    public void setUp() {
        brevDataByggerAnmodningUnntak = new BrevDataByggerAnmodningUnntak(landvelgerService, vilkaarsresultatService);

        when(vilkaarsresultatService.harVilkaarForArtikkel12(anyLong())).thenCallRealMethod();
        when(vilkaarsresultatService.finnVilkaarsresultat(anyLong(), eq(Vilkaar.FO_883_2004_ART16_1)))
            .thenReturn(Optional.of(lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, KORT_OPPDRAG_RETUR_NORSK_AG)));
    }

    @Test
    void lag_annmodningUnntakBrev_avklarVirksomhetSomSelvstendigForetak() {
        Behandling behandling = lagBehandling();

        BrevDataAnmodningUnntak brevData = (BrevDataAnmodningUnntak) brevDataByggerAnmodningUnntak.lag(lagBrevressurser(behandling), saksbehandler);
        assertThat(brevData.hovedvirksomhet.orgnr).isEqualTo("999");
        assertThat(brevData.hovedvirksomhet.erSelvstendigForetak()).isTrue();
        assertThat(brevData.arbeidsland).isEqualTo(Landkoder.DE.getBeskrivelse());
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        behandling.setFagsak(fagsak);

        List<String> selvstendigeForetak = Collections.singletonList("987654321");
        List<String> arbeidsgivereRegister = Collections.singletonList("123456789");

        behandling.setBehandlingsgrunnlag(lagBehandlingsgrunnlag(selvstendigeForetak, Collections.emptyList(), Collections.emptyList()));

        Set<Saksopplysning> saksopplysninger = lagArbeidsforholdOpplysninger(arbeidsgivereRegister);
        behandling.setSaksopplysninger(saksopplysninger);
        behandling.getSaksopplysninger().add(lagPersonsaksopplysning(new PersonDokument()));

        return behandling;
    }

    public BrevDataGrunnlag lagBrevressurser(Behandling behandling) {
        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService, mock(BehandlingService.class), kodeverkService);

        Set<String> orgSet = new HashSet<>(Collections.singletonList("987654321"));
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.getId())).thenReturn(orgSet);

        when(landvelgerService.hentArbeidsland(anyLong())).thenReturn(Landkoder.DE);
        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.setOrgnummer("999");
        OrganisasjonsDetaljer organisasjonsDetaljer = mock(OrganisasjonsDetaljer.class);
        when(organisasjonsDetaljer.hentStrukturertForretningsadresse()).thenReturn(lagStrukturertAdresse());
        organisasjonDokument.organisasjonDetaljer = organisasjonsDetaljer;

        when(registerOppslagService.hentOrganisasjoner(orgSet)).thenReturn(new HashSet<>(Collections.singletonList(organisasjonDokument)));
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder().medBehandling(behandling).build();
        return new BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
    }

    @Test
    void lag_brevDataUtenArt12_girAnmodningUtenArt12Begrunnelser() {
        Behandling behandling = lagBehandling();
        BrevDataAnmodningUnntak brevData = (BrevDataAnmodningUnntak) brevDataByggerAnmodningUnntak.lag(lagBrevressurser(behandling), saksbehandler);
        assertThat(brevData.anmodningBegrunnelser).isEmpty();
        assertThat(brevData.anmodningUtenArt12Begrunnelser).isNotEmpty();
    }

    @Test
    void lag_brevDataMedArt121_girAnmodningBegrunnelser() {
        when(vilkaarsresultatService.finnVilkaarsresultat(anyLong(), eq(Vilkaar.FO_883_2004_ART12_2)))
            .thenReturn(Optional.of(lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_2, false, UTSENDELSE_OVER_24_MN)));

        Behandling behandling = lagBehandling();
        BrevDataAnmodningUnntak brevData = (BrevDataAnmodningUnntak) brevDataByggerAnmodningUnntak.lag(lagBrevressurser(behandling), saksbehandler);
        assertThat(brevData.anmodningBegrunnelser).isNotEmpty();
        assertThat(brevData.anmodningUtenArt12Begrunnelser).isEmpty();
    }

    @Test
    void lag_brevDataMedArt122_girAnmodningBegrunnelser() {
        when(vilkaarsresultatService.finnVilkaarsresultat(anyLong(), eq(Vilkaar.FO_883_2004_ART12_2)))
            .thenReturn(Optional.of(lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_2, false, UTSENDELSE_OVER_24_MN)));

        Behandling behandling = lagBehandling();
        BrevDataAnmodningUnntak brevData = (BrevDataAnmodningUnntak) brevDataByggerAnmodningUnntak.lag(lagBrevressurser(behandling), saksbehandler);
        assertThat(brevData.anmodningBegrunnelser).isNotEmpty();
        assertThat(brevData.anmodningUtenArt12Begrunnelser).isEmpty();
    }

    @Test
    void lag_brevDataMedOppfyltArt121_girAnmodningBegrunnelser() {
        when(vilkaarsresultatService.finnVilkaarsresultat(anyLong(), eq(Vilkaar.FO_883_2004_ART12_2)))
            .thenReturn(Optional.of(lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_2, true)));

        Behandling behandling = lagBehandling();
        BrevDataAnmodningUnntak brevData = (BrevDataAnmodningUnntak) brevDataByggerAnmodningUnntak.lag(lagBrevressurser(behandling), saksbehandler);
        assertThat(brevData.anmodningBegrunnelser).isNotEmpty();
        assertThat(brevData.anmodningUtenArt12Begrunnelser).isEmpty();
    }

    @Test
    void lag_brevDataMedFritekst() {
        Vilkaarsresultat vilkaar = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, KORT_OPPDRAG_RETUR_NORSK_AG);
        vilkaar.setBegrunnelseFritekst("FRITEKST");
        when(vilkaarsresultatService.finnVilkaarsresultat(anyLong(), eq(Vilkaar.FO_883_2004_ART16_1))).thenReturn(Optional.of(vilkaar));

        Behandling behandling = lagBehandling();
        BrevDataAnmodningUnntak brevData = (BrevDataAnmodningUnntak) brevDataByggerAnmodningUnntak.lag(lagBrevressurser(behandling), saksbehandler);
        assertThat(brevData.anmodningFritekst).isEqualTo("FRITEKST");
    }
}
