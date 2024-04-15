package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.behandling.VilkaarsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagAnmodningsperiodeSvarInnvilgelse;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagPersonsaksopplysning;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevDataByggerInnvilgelseTest {
    @Mock
    AvklartefaktaService avklartefaktaService;
    @Mock
    AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    KodeverkService kodeverkService;
    @Mock
    LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    LandvelgerService landvelgerService;
    @Mock
    BrevDataByggerA1 brevDataByggerA1;
    @Mock
    AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    VilkaarsresultatService vilkaarsresultatService;
    @Mock
    PersondataFasade persondataFasade;
    @Mock
    MottatteOpplysningerService mottatteOpplysningerService;

    private Behandling behandling;
    private BrevbestillingDto brevbestillingDto;

    private final String saksbehandler = "saksbehandler";
    private BrevDataByggerInnvilgelse brevDataByggerInnvilgelse;

    @BeforeEach
    void setUp() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId("ident");

        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Set.of(aktoer));

        behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(new Soeknad());

        brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setMottaker(Mottakerroller.BRUKER);
        brevbestillingDto.setBegrunnelseKode("BEGRUNNELSEKODE");
        brevbestillingDto.setFritekst("FRITEKST");

        PersonDokument person = new PersonDokument();
        person.setSammensattNavn("Tom Mestokk");
        behandling.getSaksopplysninger().add(lagPersonsaksopplysning(person));

        when(brevDataByggerA1.lag(any(), any())).thenReturn(new BrevDataA1());

        AvklartVirksomhet virksomhet = new AvklartVirksomhet("Bedrift AS", "123456789", null, Yrkesaktivitetstyper.LOENNET_ARBEID);
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any())).thenReturn(Collections.singletonList(virksomhet));

        Lovvalgsperiode periode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(periode);

        when(landvelgerService.hentArbeidsland(anyLong())).thenReturn(Land_iso2.AT);
        when(landvelgerService.hentBostedsland(anyLong(), any(MottatteOpplysningerData.class))).thenReturn(new Bostedsland(Landkoder.NO));
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(anyLong())).thenReturn(Collections.singletonList(Land_iso2.DE));
        when(avklartefaktaService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(new AvklarteMedfolgendeFamilie(Collections.emptySet(), Collections.emptySet()));

        brevDataByggerInnvilgelse = new BrevDataByggerInnvilgelse(avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            anmodningsperiodeService,
            brevbestillingDto,
            brevDataByggerA1,
            vilkaarsresultatService,
            persondataFasade,
            mottatteOpplysningerService);
    }

    BrevDataGrunnlag lagBrevdataGrunnlag() {
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder().medBehandling(behandling).build();
        Persondata persondata = PersonopplysningerObjectFactory.lagPersonopplysninger();
        return new BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService, persondata);
    }

    @Test
    void lag_medSokkel_setterMaritimtypeSokkel() {
        Maritimtyper maritimType = Maritimtyper.SOKKEL;
        when(avklartefaktaService.hentMaritimTyper(anyLong())).thenReturn(Set.of(maritimType));

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.getSaksbehandler()).isEqualTo(saksbehandler);
        assertThat(brevData.getAvklartMaritimType()).isEqualTo(Maritimtyper.SOKKEL);
    }

    @Test
    void lag_utenMaritimtArbeid_setterMaritimtypeTilNull() {
        when(avklartefaktaService.hentMaritimTyper(anyLong())).thenReturn(Collections.emptySet());

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.getAvklartMaritimType()).isNull();
    }

    @Test
    void lag_medFtrl2_12_setterTuristSkipTrue() {
        when(vilkaarsresultatService.oppfyllerVilkaar(behandling.getId(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP))
            .thenReturn(true);

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.getTuristskip()).isTrue();
    }

    @Test
    void lag_innvilgelsesBrev_harBestillingsinformasjon() {
        BrevData brevData = brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.getBegrunnelseKode()).isEqualTo(brevbestillingDto.getBegrunnelseKode());
        assertThat(brevData.getFritekst()).isEqualTo(brevbestillingDto.getFritekst());
        assertThat(brevData.getSaksbehandler()).isEqualTo(saksbehandler);
    }

    @Test
    void lag_medAnmodningsperiode_girAnmodningsperiodeSvar() {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        AnmodningsperiodeSvar anmodningsperiodeSvar = lagAnmodningsperiodeSvarInnvilgelse();
        anmodningsperiode.setSendtUtland(true);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);

        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singletonList(anmodningsperiode));
        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.getAnmodningsperiodesvar()).isPresent().get().isEqualTo(anmodningsperiodeSvar);
    }

    @Test
    void lag_utenAnmodningsperiode_erMulig() {
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.emptyList());
        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.getAnmodningsperiodesvar()).isNotPresent();
    }

    @Test
    void lag_erArt12_art16UtenArt12False() {
        when(vilkaarsresultatService.harVilkaarForArtikkel12(anyLong())).thenReturn(true);
        when(vilkaarsresultatService.harVilkaarForArtikkel16(anyLong())).thenReturn(true);

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.getArt16UtenArt12()).isFalse();
    }

    @Test
    void lag_erArt16UtenArt12_art16UtenArt12True() {
        when(vilkaarsresultatService.harVilkaarForArtikkel12(anyLong())).thenReturn(false);
        when(vilkaarsresultatService.harVilkaarForArtikkel16(anyLong())).thenReturn(true);

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.getArt16UtenArt12()).isTrue();
    }

    @Test
    void lag_medfølgendeBarnHarFnr_henterNavnFraTps() {
        MedfolgendeFamilie barn1 = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID.randomUUID().toString(), "fnr1", null, MedfolgendeFamilie.Relasjonsrolle.BARN);
        MedfolgendeFamilie barn2 = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID.randomUUID().toString(), "fnr2", null, MedfolgendeFamilie.Relasjonsrolle.BARN);
        MottatteOpplysningerData mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.personOpplysninger.getMedfolgendeFamilie().addAll(List.of(barn1, barn2));
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(mottatteOpplysningerData);

        when(avklartefaktaService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(new AvklarteMedfolgendeFamilie(
            Set.of(new OmfattetFamilie(barn1.getUuid())),
            Set.of(new IkkeOmfattetFamilie(barn2.getUuid(), null, null))));
        when(mottatteOpplysningerService.hentMottatteOpplysninger(anyLong())).thenReturn(mottatteOpplysninger);
        when(persondataFasade.hentSammensattNavn(barn1.getFnr())).thenReturn("Navn1");
        when(persondataFasade.hentSammensattNavn(barn2.getFnr())).thenReturn("Navn2");

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.getAvklarteMedfolgendeBarn().getFamilieOmfattetAvNorskTrygd())
            .extracting("sammensattNavn", "ident")
            .containsExactly(tuple("Navn1", barn1.getFnr()));
        assertThat(brevData.getAvklarteMedfolgendeBarn().getFamilieIkkeOmfattetAvNorskTrygd())
            .extracting("sammensattNavn")
            .containsExactly("Navn2");

        verify(persondataFasade, times(2)).hentSammensattNavn(anyString());
    }

    @Test
    void lag_medfølgendeBarnHarUuid_henterNavnFraMottatteOpplysninger() {
        MedfolgendeFamilie barn1 = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID.randomUUID().toString(), null, "Navn1", MedfolgendeFamilie.Relasjonsrolle.BARN);
        MedfolgendeFamilie barn2 = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID.randomUUID().toString(), null, "Navn2", MedfolgendeFamilie.Relasjonsrolle.BARN);
        MottatteOpplysningerData mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.personOpplysninger.getMedfolgendeFamilie().addAll(List.of(barn1, barn2));
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(mottatteOpplysningerData);

        when(avklartefaktaService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(new AvklarteMedfolgendeFamilie(
            Set.of(new OmfattetFamilie(barn1.getUuid())),
            Set.of(new IkkeOmfattetFamilie(barn2.getUuid(), null, null))));
        when(mottatteOpplysningerService.hentMottatteOpplysninger(anyLong())).thenReturn(mottatteOpplysninger);

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.getAvklarteMedfolgendeBarn().getFamilieOmfattetAvNorskTrygd())
            .extracting("sammensattNavn").containsExactly(barn1.getNavn());
        assertThat(brevData.getAvklarteMedfolgendeBarn().getFamilieIkkeOmfattetAvNorskTrygd())
            .extracting("sammensattNavn").containsExactly(barn2.getNavn());

        verify(persondataFasade, never()).hentSammensattNavn(anyString());
    }

    @Test
    void lag_omfattetBarnIkkeIMottatteOpplysninger_kasterException() {
        MedfolgendeFamilie barn = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID.randomUUID().toString(), null, "Navn", MedfolgendeFamilie.Relasjonsrolle.BARN);
        final BrevDataGrunnlag brevDataGrunnlag = lagBrevdataGrunnlag();

        when(avklartefaktaService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(new AvklarteMedfolgendeFamilie(
            Set.of(new OmfattetFamilie(barn.getUuid())), Collections.emptySet()));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> brevDataByggerInnvilgelse.lag(brevDataGrunnlag, saksbehandler))
            .withMessageContaining("finnes ikke i mottatteOpplysningeret");
    }

    @Test
    void lag_ikkeOmfattetBarnIkkeIMottatteOpplysninger_kasterException() {
        MedfolgendeFamilie barn = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID.randomUUID().toString(), null, "Navn", MedfolgendeFamilie.Relasjonsrolle.BARN);
        final BrevDataGrunnlag brevDataGrunnlag = lagBrevdataGrunnlag();

        when(avklartefaktaService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(new AvklarteMedfolgendeFamilie(
            Collections.emptySet(), Set.of(new IkkeOmfattetFamilie(barn.getUuid(), null, null))));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> brevDataByggerInnvilgelse.lag(brevDataGrunnlag, saksbehandler))
            .withMessageContaining("finnes ikke i mottatteOpplysningeret");
    }
}
