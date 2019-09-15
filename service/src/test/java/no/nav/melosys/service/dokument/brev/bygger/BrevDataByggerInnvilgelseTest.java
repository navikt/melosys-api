package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

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

    private Behandling behandling;

    @Mock
    private BrevbestillingDto brevbestillingDto;

    private String saksbehandler = "saksbehandler";
    private BrevDataByggerInnvilgelse brevDataByggerInnvilgelse;

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
        behandling = new Behandling();
        behandling.setId(1L);
        behandling.getSaksopplysninger().add(lagSoeknadssaksopplysning(new SoeknadDokument()));
        behandling.getSaksopplysninger().add(lagPersonsaksopplysning(new PersonDokument()));

        when(brevDataByggerA1.lag(any(), any())).thenReturn(new BrevDataA1());

        AvklartVirksomhet virksomhet = new AvklartVirksomhet("Bedrift AS", "123456789", null, Yrkesaktivitetstyper.LOENNET_ARBEID);
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Arrays.asList(virksomhet));

        Lovvalgsperiode periode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(periode);

        when(landvelgerService.hentArbeidsland(anyLong())).thenReturn(Landkoder.AT);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(anyLong())).thenReturn(Collections.singletonList(Landkoder.DE));

        brevDataByggerInnvilgelse = new BrevDataByggerInnvilgelse(avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            anmodningsperiodeService,
            brevbestillingDto,
            brevDataByggerA1);
    }

    public DokumentdataGrunnlag lagBrevdataGrunnlag() throws TekniskException {
        return new DokumentdataGrunnlag(behandling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
    }

    @Test
    public void lag_medSokkel_setterMaritimtypeSokkel() throws FunksjonellException, TekniskException {
        Maritimtyper maritimType = Maritimtyper.SOKKEL;
        when(avklartefaktaService.hentMaritimType(anyLong())).thenReturn(Optional.of(maritimType));

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
        assertThat(brevData.avklartMaritimType).isEqualTo(Maritimtyper.SOKKEL);
    }

    @Test
    public void lag_utenMaritimtArbeid_setterMaritimtypeTilNull() throws FunksjonellException, TekniskException {
        when(avklartefaktaService.hentMaritimType(anyLong())).thenReturn(Optional.empty());

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.avklartMaritimType).isNull();
    }

    @Test
    public void lag_innvilgelsesBrev_harBestillingsinformasjon() throws FunksjonellException, TekniskException {
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.mottaker = Aktoersroller.BRUKER;
        brevbestillingDto.begrunnelseKode = "BEGRUNNELSEKODE";
        brevbestillingDto.fritekst = "FRITEKST";

        BrevDataByggerInnvilgelse brevDataByggerInnvilgelse =
            new BrevDataByggerInnvilgelse(avklartefaktaService, landvelgerService, lovvalgsperiodeService, anmodningsperiodeService, brevbestillingDto, brevDataByggerA1);

        BrevData brevData = brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData).isEqualToComparingOnlyGivenFields(brevbestillingDto, "begrunnelseKode", "fritekst");
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
    }

    @Test
    public void lag_medAnmodningsperiode_girAnmodningsperiodeSvar() throws FunksjonellException, TekniskException {
        BrevDataByggerInnvilgelse brevDataByggerInnvilgelse =
            new BrevDataByggerInnvilgelse(avklartefaktaService, landvelgerService, lovvalgsperiodeService, anmodningsperiodeService, brevbestillingDto, brevDataByggerA1);

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        AnmodningsperiodeSvar anmodningsperiodeSvar = lagAnmodningsperiodeSvarInnvilgelse();
        anmodningsperiode.setSendtUtland(true);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);

        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singletonList(anmodningsperiode));
        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.anmodningsperiodesvar.get()).isEqualTo(anmodningsperiodeSvar);
    }

    @Test
    public void lag_utenAnmodningsperiode_erMulig() throws FunksjonellException, TekniskException {
        BrevDataByggerInnvilgelse brevDataByggerInnvilgelse =
            new BrevDataByggerInnvilgelse(avklartefaktaService, landvelgerService, lovvalgsperiodeService, anmodningsperiodeService, brevbestillingDto, brevDataByggerA1);

        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.emptyList());
        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), saksbehandler);
        assertThat(brevData.anmodningsperiodesvar.isPresent()).isFalse();
    }
}