package no.nav.melosys.service.dokument.brev.bygger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_vesentlig_virksomhet;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.domain.OrganisasjonDokumentTestFactory;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagArbeidsgiver;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART12_1;
import static no.nav.melosys.service.MottatteOpplysningerStub.lagMottatteOpplysninger;
import static no.nav.melosys.service.SaksopplysningStubs.lagArbeidsforholdOpplysninger;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagPersonsaksopplysning;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagStrukturertAdresse;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrevDataByggerAvslagArbeidsgiverTest {
    @Mock
    AvklartefaktaService avklartefaktaService;
    @Mock
    LandvelgerService landvelgerService;
    @Mock
    OrganisasjonOppslagService organisasjonOppslagService;
    @Mock
    KodeverkService kodeverkService;
    @Mock
    VilkaarsresultatRepository vilkaarsresultatRepository;
    @Mock
    LovvalgsperiodeService lovvalgsperiodeService;

    private BrevDataByggerAvslagArbeidsgiver brevDataByggerAvslagArbeidsgiver;

    @BeforeEach
    void setUp() {
        when(landvelgerService.hentArbeidsland(anyLong())).thenReturn(Land_iso2.AT);

        brevDataByggerAvslagArbeidsgiver = new BrevDataByggerAvslagArbeidsgiver(landvelgerService,
            lovvalgsperiodeService,
            vilkaarsresultatRepository);
    }

    @Test
    void lag_avslagArbeidsgiverBrev_harVilkaarBegrunnelser() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId("ident");

        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Set.of(aktoer));

        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(1L);
        behandling.getSaksopplysninger().add(lagPersonsaksopplysning(new PersonDokument()));

        PersonDokument personDokument = new PersonDokument();
        personDokument.setSammensattNavn("Navn Navnesen");
        Saksopplysning person = new Saksopplysning();
        person.setDokument(personDokument);
        person.setType(SaksopplysningType.PERSOPL);

        Set<Saksopplysning> saksopplysninger = lagArbeidsforholdOpplysninger(Collections.singletonList("123456789"));
        saksopplysninger.add(person);
        behandling.setSaksopplysninger(saksopplysninger);

        behandling.setMottatteOpplysninger(lagMottatteOpplysninger(Collections.emptyList(),
            Collections.emptyList(),
            Collections.singletonList("987654321")));

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Land_iso2.DE);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        when(lovvalgsperiodeService.hentLovvalgsperiode(behandling.getId())).thenReturn(lovvalgsperiode);

        Set<String> orgSet = new HashSet<>(Collections.singletonList("987654321"));
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.getId())).thenReturn(orgSet);

        OrganisasjonsDetaljer organisasjonsDetaljer = mock(OrganisasjonsDetaljer.class);
        when(organisasjonsDetaljer.hentStrukturertForretningsadresse()).thenReturn(lagStrukturertAdresse());
        OrganisasjonDokument organisasjonDokument = OrganisasjonDokumentTestFactory.builder()
            .orgnummer("987654321")
            .organisasjonsDetaljer(organisasjonsDetaljer)
            .build();

        when(organisasjonOppslagService.hentOrganisasjoner(orgSet)).thenReturn(new HashSet<>(Collections.singletonList(organisasjonDokument)));

        Vilkaarsresultat vilkaarsresultatArt121 = lagVilkårresultat(Vilkaar.FO_883_2004_ART12_1, Art12_1_begrunnelser.IKKE_OMFATTET_LENGE_NOK_I_NORGE_FOER.getKode());
        Vilkaarsresultat vesentligVirksomhet = lagVilkårresultat(Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET, Art12_1_vesentlig_virksomhet.FOR_LITE_KONTRAKTER_NORGE.getKode());

        when(vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(anyLong(), eq(FO_883_2004_ART12_1))).thenReturn(Optional.of(vilkaarsresultatArt121));
        when(vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(anyLong(), eq(ART12_1_VESENTLIG_VIRKSOMHET))).thenReturn(Optional.of(vesentligVirksomhet));

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService,
            organisasjonOppslagService, mock(BehandlingService.class), kodeverkService);
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder().medBehandling(behandling).build();
        Persondata persondata = PersonopplysningerObjectFactory.lagPersonopplysninger();
        BrevDataGrunnlag dataGrunnlag = new BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService, persondata);
        String saksbehandler = "saksbehandler";
        BrevDataAvslagArbeidsgiver brevData = (BrevDataAvslagArbeidsgiver) brevDataByggerAvslagArbeidsgiver.lag(dataGrunnlag, saksbehandler);
        assertThat(brevData.hovedvirksomhet.orgnr).isEqualTo("987654321");
    }

    private Vilkaarsresultat lagVilkårresultat(Vilkaar vilkaarType, String vilkårbegrunnelseKode) {
        VilkaarBegrunnelse begrunnelser = new VilkaarBegrunnelse();
        begrunnelser.setKode(vilkårbegrunnelseKode);
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setOppfylt(false);
        vilkaarsresultat.setVilkaar(vilkaarType);
        vilkaarsresultat.setBegrunnelser(Collections.singleton(begrunnelser));
        return vilkaarsresultat;
    }
}
