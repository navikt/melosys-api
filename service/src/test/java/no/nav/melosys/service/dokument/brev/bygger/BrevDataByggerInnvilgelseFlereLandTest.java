package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelseFlereLand;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
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
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new SoeknadDokument());

        brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.mottaker = Aktoersroller.BRUKER;
        brevbestillingDto.begrunnelseKode = "BEGRUNNELSEKODE";
        brevbestillingDto.fritekst = "FRITEKST";

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
        return new BrevDataGrunnlag(behandling, null, avklarteVirksomheterService, avklartefaktaService);
    }

    private static Saksopplysning lagPersonsopplysning() {
        PersonDokument person = new PersonDokument();
        return lagPersonsaksopplysning(person);
    }

    @Test
    public void lag_medSokkel_setterMaritimtypeSokkel() throws FunksjonellException, TekniskException {
        Maritimtyper maritimType = Maritimtyper.SOKKEL;
        when(avklartefaktaService.hentMaritimType(anyLong())).thenReturn(Optional.of(maritimType));

        BrevDataGrunnlag brevdataressurser = lagBrevressurser();
        BrevDataInnvilgelseFlereLand brevData = (BrevDataInnvilgelseFlereLand) brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler);
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
        assertThat(brevData.avklartMaritimType).isEqualTo(Maritimtyper.SOKKEL);
    }

    @Test
    public void lag_utenMaritimtArbeid_setterMaritimtypeTilNull() throws FunksjonellException, TekniskException {
        when(avklartefaktaService.hentMaritimType(anyLong())).thenReturn(Optional.empty());

        BrevDataGrunnlag brevdataressurser = lagBrevressurser();
        BrevDataInnvilgelseFlereLand brevData = (BrevDataInnvilgelseFlereLand) brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler);
        assertThat(brevData.avklartMaritimType).isNull();
        assertThat(brevData.trydemyndighetsland).isNull();
    }

    @Test
    public void lag_utpekingAnnetLand_setterTrydemyndighetsland() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setAvsenderID("NO" + ":" + "xopjaf");
        when(saksopplysningerService.hentSedOpplysninger(behandling.getId())).thenReturn(sedDokument);

        BrevDataGrunnlag brevdataressurser = lagBrevressurser();
        BrevDataInnvilgelseFlereLand brevData = (BrevDataInnvilgelseFlereLand) brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler);
        assertThat(brevData.trydemyndighetsland).isEqualTo(Landkoder.NO);
    }

    @Test
    public void lag_innvilgelsesBrev_harBestillingsinformasjon() throws FunksjonellException, TekniskException {
        BrevData brevData = brevDataByggerInnvilgelse.lag(lagBrevressurser(), saksbehandler);

        assertThat(brevData).isEqualToComparingOnlyGivenFields(brevbestillingDto, "begrunnelseKode", "fritekst");
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
    }
}