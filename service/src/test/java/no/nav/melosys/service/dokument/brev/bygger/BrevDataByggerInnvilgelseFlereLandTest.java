package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.*;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagPersonsaksopplysning;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerInnvilgelseFlereLandTest {
    @Mock
    AvklartefaktaService avklartefaktaService;
    @Mock
    AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    LandvelgerService landvelgerService;
    @Mock
    LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    SaksopplysningerService saksopplysningerService;
    @Mock
    BrevDataByggerA1 brevDataByggerA1;

    private Behandling behandling;
    private BrevbestillingDto brevbestillingDto;
    private final String saksbehandler = "saksbehandler";
    private BrevDataBygger brevDataByggerInnvilgelse;

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
        behandling = new Behandling();
        behandling.setId(1L);
        behandling.getSaksopplysninger().add(lagPersonsopplysning());
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new Soeknad());

        brevbestillingDto = new BrevbestillingDto.Builder()
            .medMottaker(Aktoersroller.BRUKER)
            .medBegrunnelseKode("BEGRUNNELSEKODE")
            .medFritekst("FRITEKST")
            .build();

        when(brevDataByggerA1.lag(any(), any())).thenReturn(new BrevDataA1());

        Lovvalgsperiode periode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.hentValidertLovvalgsperiode(anyLong())).thenReturn(periode);

        when(landvelgerService.hentAlleArbeidsland(anyLong())).thenReturn(Collections.singleton(Landkoder.AT));
        when(landvelgerService.hentBostedsland(anyLong(), any())).thenReturn(Landkoder.DE);

        brevDataByggerInnvilgelse = new BrevDataByggerInnvilgelseFlereLand(avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            saksopplysningerService,
            brevbestillingDto,
            brevDataByggerA1);
    }

    private BrevDataGrunnlag lagBrevressurser() throws TekniskException {
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder().medBehandling(behandling).build();
        return new BrevDataGrunnlag(brevbestilling, null, avklarteVirksomheterService, avklartefaktaService);
    }

    private static Saksopplysning lagPersonsopplysning() {
        PersonDokument person = new PersonDokument();
        return lagPersonsaksopplysning(person);
    }

    @Test
    public void lag_medSokkel_setterMaritimtypeSokkel() throws FunksjonellException, TekniskException {
        Maritimtyper maritimType = Maritimtyper.SOKKEL;
        when(avklartefaktaService.hentMaritimTyper(anyLong())).thenReturn(Set.of(maritimType));

        BrevDataGrunnlag brevdataressurser = lagBrevressurser();
        BrevDataInnvilgelseFlereLand brevData = (BrevDataInnvilgelseFlereLand) brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler);
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
        assertThat(brevData.harAvklartMaritimTypeSokkel).isTrue();
        assertThat(brevData.harAvklartMaritimTypeSkip).isFalse();
    }

    @Test
    public void lag_utenMaritimtArbeid_setterMaritimtypeTilNull() throws FunksjonellException, TekniskException {
        when(avklartefaktaService.hentMaritimTyper(anyLong())).thenReturn(Collections.emptySet());

        BrevDataGrunnlag brevdataressurser = lagBrevressurser();
        BrevDataInnvilgelseFlereLand brevData = (BrevDataInnvilgelseFlereLand) brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler);
        assertThat(brevData.harAvklartMaritimTypeSokkel).isFalse();
        assertThat(brevData.harAvklartMaritimTypeSkip).isFalse();
        assertThat(brevData.trydemyndighetsland).isNull();
    }

    @Test
    public void lag_utpekingAnnetLand_setterTrydemyndighetsland() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setAvsenderLandkode(Landkoder.DE);
        when(saksopplysningerService.hentSedOpplysninger(behandling.getId())).thenReturn(sedDokument);

        BrevDataGrunnlag brevdataressurser = lagBrevressurser();
        BrevDataInnvilgelseFlereLand brevData = (BrevDataInnvilgelseFlereLand) brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler);
        assertThat(brevData.trydemyndighetsland).isEqualTo(Landkoder.DE);
    }

    @Test
    public void lag_innvilgelsesBrev_harBestillingsinformasjon() throws FunksjonellException, TekniskException {
        BrevData brevData = brevDataByggerInnvilgelse.lag(lagBrevressurser(), saksbehandler);

        assertThat(brevData).isEqualToComparingOnlyGivenFields(brevbestillingDto, "begrunnelseKode", "fritekst");
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
    }
}