package no.nav.melosys.service.dokument.brev.bygger;

import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.domain.OrganisasjonDokumentTestFactory;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagYrkesaktiv;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.behandling.VilkaarsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.KORT_OPPDRAG_RETUR_NORSK_AG;
import static no.nav.melosys.service.MottatteOpplysningerStub.lagMottatteOpplysninger;
import static no.nav.melosys.service.SaksopplysningStubs.lagArbeidsforholdOpplysninger;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevDataByggerAvslagYrkesaktivTest {
    @Mock
    AvklartefaktaService avklartefaktaService;
    @Mock
    OrganisasjonOppslagService organisasjonOppslagService;
    @Mock
    LandvelgerService landvelgerService;
    @Mock
    AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    KodeverkService kodeverkService;
    @Mock
    VilkaarsresultatService vilkaarsresultatService;

    private BrevDataByggerAvslagYrkesaktiv brevDataByggerAvslagYrkesaktiv;
    private AnmodningsperiodeSvar anmodningsperiodeSvar;

    @BeforeEach
    public void setUp() {
        anmodningsperiodeSvar = lagAnmodningsperiodeSvarAvslag();
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);
        anmodningsperiode.setSendtUtland(true);
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singletonList(anmodningsperiode));

        when(vilkaarsresultatService.finnVilkaarsresultat(anyLong(), eq(Vilkaar.FO_883_2004_ART16_1)))
            .thenReturn(Optional.of(lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, KORT_OPPDRAG_RETUR_NORSK_AG)));

        brevDataByggerAvslagYrkesaktiv = new BrevDataByggerAvslagYrkesaktiv(landvelgerService, anmodningsperiodeService, new BrevbestillingDto(), vilkaarsresultatService);
    }

    @Test
    void lag_annmodningUnntakBrev_avklarVirksomhetSomSelvstendigForetak() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Fagsak fagsak = FagsakTestFactory.builder().medBruker().build();
        behandling.setFagsak(fagsak);

        List<String> selvstendigeForetak = Collections.singletonList("987654321");
        List<String> arbeidsgivereRegister = Collections.singletonList("123456789");

        Set<Saksopplysning> saksopplysninger = lagArbeidsforholdOpplysninger(arbeidsgivereRegister);
        behandling.setSaksopplysninger(saksopplysninger);
        behandling.getSaksopplysninger().add(lagPersonsaksopplysning(new PersonDokument()));
        behandling.setMottatteOpplysninger(lagMottatteOpplysninger(selvstendigeForetak, Collections.emptyList(), Collections.emptyList()));

        Set<String> orgSet = new HashSet<>(Collections.singletonList("987654321"));
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.getId())).thenReturn(orgSet);

        when(landvelgerService.hentArbeidsland(anyLong())).thenReturn(Land_iso2.DE);
        OrganisasjonsDetaljer organisasjonsDetaljer = mock(OrganisasjonsDetaljer.class);
        when(organisasjonsDetaljer.hentStrukturertForretningsadresse()).thenReturn(lagStrukturertAdresse());
        OrganisasjonDokument organisasjonDokument = OrganisasjonDokumentTestFactory.builder()
            .orgnummer("999")
            .organisasjonsDetaljer(organisasjonsDetaljer)
            .build();

        lenient().when(
            organisasjonOppslagService.hentOrganisasjoner(orgSet)).thenReturn(new HashSet<>(Collections.singletonList(organisasjonDokument)));
        when(vilkaarsresultatService.harVilkaarForArtikkel12(anyLong())).thenReturn(false);
        when(vilkaarsresultatService.harVilkaarForArtikkel16(anyLong())).thenReturn(true);

        String saksbehandler = "saksbehandler";
        BrevDataAvslagYrkesaktiv brevData = (BrevDataAvslagYrkesaktiv) brevDataByggerAvslagYrkesaktiv.lag(lagBrevressurser(behandling), saksbehandler);

        assertThat(brevData.getHovedvirksomhet().orgnr).isEqualTo("999");
        assertThat(brevData.getHovedvirksomhet().erSelvstendigForetak()).isTrue();
        assertThat(brevData.getArbeidsland()).isEqualTo(Landkoder.DE.getBeskrivelse());
        assertThat(brevData.getAnmodningsperiodeSvar()).usingRecursiveComparison().isEqualTo(anmodningsperiodeSvar);
        assertThat(brevData.getArt16UtenArt12()).isTrue();
    }

    private AnmodningsperiodeSvar lagAnmodningsperiodeSvarAvslag() {
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setBegrunnelseFritekst("No tiendo");
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        return anmodningsperiodeSvar;
    }

    public BrevDataGrunnlag lagBrevressurser(Behandling behandling) {
        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService,
            organisasjonOppslagService, mock(BehandlingService.class), kodeverkService);
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder().medBehandling(behandling).build();
        Persondata persondata = PersonopplysningerObjectFactory.lagPersonopplysninger();
        return new BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService, persondata);
    }
}
