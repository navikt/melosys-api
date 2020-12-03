package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.MedfolgendeFamilie;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.familie.AvklarteMedfolgendeBarn;
import no.nav.melosys.domain.familie.IkkeOmfattetBarn;
import no.nav.melosys.domain.familie.OmfattetBarn;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagAnmodningsperiodeSvarInnvilgelse;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagPersonsaksopplysning;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerInnvilgelseTest {
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
    TpsFasade tpsFasade;
    @Mock
    BehandlingsgrunnlagService behandlingsgrunnlagService;

    private Behandling behandling;
    private BrevbestillingDto brevbestillingDto;

    private final String saksbehandler = "saksbehandler";
    private BrevDataByggerInnvilgelse brevDataByggerInnvilgelse;

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
        behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(new Fagsak());
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new Soeknad());

        brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.mottaker = Aktoersroller.BRUKER;
        brevbestillingDto.begrunnelseKode = "BEGRUNNELSEKODE";
        brevbestillingDto.fritekst = "FRITEKST";

        PersonDokument person = new PersonDokument();
        person.sammensattNavn = "Tom Mestokk";
        behandling.getSaksopplysninger().add(lagPersonsaksopplysning(person));

        when(brevDataByggerA1.lag(any(), any())).thenReturn(new BrevDataA1());

        AvklartVirksomhet virksomhet = new AvklartVirksomhet("Bedrift AS", "123456789", null, Yrkesaktivitetstyper.LOENNET_ARBEID);
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(virksomhet));

        Lovvalgsperiode periode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.hentValidertLovvalgsperiode(anyLong())).thenReturn(periode);

        when(landvelgerService.hentArbeidsland(anyLong())).thenReturn(Landkoder.AT);
        when(landvelgerService.hentBostedsland(anyLong(), any(BehandlingsgrunnlagData.class))).thenReturn(Landkoder.NO);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(anyLong())).thenReturn(Collections.singletonList(Landkoder.DE));
        when(avklartefaktaService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(new AvklarteMedfolgendeBarn(Collections.emptySet(), Collections.emptySet()));

        brevDataByggerInnvilgelse = new BrevDataByggerInnvilgelse(avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            anmodningsperiodeService,
            brevbestillingDto,
            brevDataByggerA1,
            vilkaarsresultatService,
            tpsFasade,
            behandlingsgrunnlagService);
    }

    public BrevDataGrunnlag lagBrevdataGrunnlag() throws TekniskException {
        Brevbestilling brevbestilling = new Brevbestilling.Builder().medBehandling(behandling).build();
        return new BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
    }

    @Test
    public void lag_medSokkel_setterMaritimtypeSokkel() throws FunksjonellException, TekniskException {
        Maritimtyper maritimType = Maritimtyper.SOKKEL;
        when(avklartefaktaService.hentMaritimTyper(anyLong())).thenReturn(Set.of(maritimType));

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
        assertThat(brevData.avklartMaritimType).isEqualTo(Maritimtyper.SOKKEL);
    }

    @Test
    public void lag_utenMaritimtArbeid_setterMaritimtypeTilNull() throws FunksjonellException, TekniskException {
        when(avklartefaktaService.hentMaritimTyper(anyLong())).thenReturn(Collections.emptySet());

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.avklartMaritimType).isNull();
    }

    @Test
    public void lag_medFtrl2_12_setterTuristSkipTrue() throws FunksjonellException, TekniskException {
        when(vilkaarsresultatService.oppfyllerVilkaar(eq(behandling.getId()), eq(Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP)))
            .thenReturn(true);

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.erTuristskip).isTrue();
    }

    @Test
    public void lag_innvilgelsesBrev_harBestillingsinformasjon() throws FunksjonellException, TekniskException {
        BrevData brevData = brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData).isEqualToComparingOnlyGivenFields(brevbestillingDto, "begrunnelseKode", "fritekst");
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
    }

    @Test
    public void lag_medAnmodningsperiode_girAnmodningsperiodeSvar() throws FunksjonellException, TekniskException {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        AnmodningsperiodeSvar anmodningsperiodeSvar = lagAnmodningsperiodeSvarInnvilgelse();
        anmodningsperiode.setSendtUtland(true);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);

        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singletonList(anmodningsperiode));
        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.getAnmodningsperiodesvar()).isPresent().get().isEqualTo(anmodningsperiodeSvar);
    }

    @Test
    public void lag_utenAnmodningsperiode_erMulig() throws FunksjonellException, TekniskException {
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.emptyList());
        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.getAnmodningsperiodesvar()).isNotPresent();
    }

    @Test
    public void lag_erArt12_art16UtenArt12False() throws FunksjonellException, TekniskException {
        when(vilkaarsresultatService.harVilkaarForArtikkel12(anyLong())).thenReturn(true);
        when(vilkaarsresultatService.harVilkaarForArtikkel16(anyLong())).thenReturn(true);

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.erArt16UtenArt12).isFalse();
    }

    @Test
    public void lag_erArt16UtenArt12_art16UtenArt12True() throws FunksjonellException, TekniskException {
        when(vilkaarsresultatService.harVilkaarForArtikkel12(anyLong())).thenReturn(false);
        when(vilkaarsresultatService.harVilkaarForArtikkel16(anyLong())).thenReturn(true);

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.erArt16UtenArt12).isTrue();
    }

    @Test
    public void lag_medfølgendeBarnHarFnr_henterNavnFraTps() throws TekniskException, FunksjonellException {
        when(avklartefaktaService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(new AvklarteMedfolgendeBarn(
            Set.of(new OmfattetBarn("fnr1")),
            Set.of(new IkkeOmfattetBarn("fnr2", null, null))));
        when(tpsFasade.hentSammensattNavn(eq("fnr1"))).thenReturn("Navn1");
        when(tpsFasade.hentSammensattNavn(eq("fnr2"))).thenReturn("Navn2");

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.avklarteMedfolgendeBarn.barnOmfattetAvNorskTrygd)
            .extracting("sammensattNavn").containsExactly("Navn1");
        assertThat(brevData.avklarteMedfolgendeBarn.barnIkkeOmfattetAvNorskTrygd)
            .extracting("sammensattNavn").containsExactly("Navn2");

        verify(tpsFasade, times(2)).hentSammensattNavn(anyString());
    }

    @Test
    public void lag_medfølgendeBarnHarUuid_henterNavnFraBehandlingsgrunnlag() throws TekniskException, FunksjonellException {
        when(avklartefaktaService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(new AvklarteMedfolgendeBarn(
            Set.of(new OmfattetBarn("uuid1")),
            Set.of(new IkkeOmfattetBarn("uuid2", null, null))));
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(anyLong())).thenReturn(lagBehandlingsgrunnlagMedMedfølgendeBarn());

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.avklarteMedfolgendeBarn.barnOmfattetAvNorskTrygd)
            .extracting("sammensattNavn").containsExactly("Navn1");
        assertThat(brevData.avklarteMedfolgendeBarn.barnIkkeOmfattetAvNorskTrygd)
            .extracting("sammensattNavn").containsExactly("Navn2");

        verify(tpsFasade, never()).hentSammensattNavn(anyString());
    }

    private Behandlingsgrunnlag lagBehandlingsgrunnlagMedMedfølgendeBarn() {
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.personOpplysninger.medfolgendeFamilie.addAll(List.of(
            lagMedfølgendeBarnMedUuidOgNavn("uuid1", "Navn1"),
            lagMedfølgendeBarnMedUuidOgNavn("uuid2", "Navn2")
        ));
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagData);
        return behandlingsgrunnlag;
    }

    private MedfolgendeFamilie lagMedfølgendeBarnMedUuidOgNavn(String uuid, String navn) {
        MedfolgendeFamilie barn = new MedfolgendeFamilie();
        barn.uuid = uuid;
        barn.navn = navn;
        barn.relasjonsrolle = MedfolgendeFamilie.Relasjonsrolle.BARN;
        return barn;
    }
}