package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelseFlereLand;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagPersonsaksopplysning;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrevDataByggerInnvilgelseFlereLandTest {
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
    private BrevbestillingRequest brevbestillingRequest;
    private final String saksbehandler = "saksbehandler";
    private BrevDataBygger brevDataByggerInnvilgelse;

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
        behandling.getSaksopplysninger().add(lagPersonsopplysning());
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new Soeknad());

        brevbestillingRequest = new BrevbestillingRequest.Builder()
            .medMottaker(Aktoersroller.BRUKER)
            .medBegrunnelseKode("BEGRUNNELSEKODE")
            .medFritekst("FRITEKST")
            .build();

        when(brevDataByggerA1.lag(any(), any())).thenReturn(new BrevDataA1());

        Lovvalgsperiode periode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(periode);

        when(landvelgerService.hentAlleArbeidsland(anyLong())).thenReturn(Collections.singleton(Landkoder.AT));
        when(landvelgerService.hentBostedsland(anyLong(), any())).thenReturn(new Bostedsland(Landkoder.DE));

        brevDataByggerInnvilgelse = new BrevDataByggerInnvilgelseFlereLand(avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            saksopplysningerService,
            brevbestillingRequest,
            brevDataByggerA1);
    }

    private BrevDataGrunnlag lagBrevressurser() {
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder().medBehandling(behandling).build();
        Persondata persondata = PersonopplysningerObjectFactory.lagPersonopplysninger();
        return new BrevDataGrunnlag(brevbestilling, null, avklarteVirksomheterService, avklartefaktaService, persondata);
    }

    private static Saksopplysning lagPersonsopplysning() {
        PersonDokument person = new PersonDokument();
        return lagPersonsaksopplysning(person);
    }

    @Test
    void lag_medSokkel_setterMaritimtypeSokkel() {
        Maritimtyper maritimType = Maritimtyper.SOKKEL;
        when(avklartefaktaService.hentMaritimTyper(anyLong())).thenReturn(Set.of(maritimType));

        BrevDataGrunnlag brevdataressurser = lagBrevressurser();
        BrevDataInnvilgelseFlereLand brevData = (BrevDataInnvilgelseFlereLand) brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler);
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
        assertThat(brevData.harAvklartMaritimTypeSokkel).isTrue();
        assertThat(brevData.harAvklartMaritimTypeSkip).isFalse();
    }

    @Test
    void lag_utenMaritimtArbeid_setterMaritimtypeTilNull() {
        when(avklartefaktaService.hentMaritimTyper(anyLong())).thenReturn(Collections.emptySet());

        BrevDataGrunnlag brevdataressurser = lagBrevressurser();
        BrevDataInnvilgelseFlereLand brevData = (BrevDataInnvilgelseFlereLand) brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler);
        assertThat(brevData.harAvklartMaritimTypeSokkel).isFalse();
        assertThat(brevData.harAvklartMaritimTypeSkip).isFalse();
        assertThat(brevData.trydemyndighetsland).isNull();
    }

    @Test
    void lag_utpekingAnnetLand_setterTrydemyndighetsland() {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setAvsenderLandkode(Landkoder.DE);
        when(saksopplysningerService.hentSedOpplysninger(behandling.getId())).thenReturn(sedDokument);

        BrevDataGrunnlag brevdataressurser = lagBrevressurser();
        BrevDataInnvilgelseFlereLand brevData = (BrevDataInnvilgelseFlereLand) brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler);
        assertThat(brevData.trydemyndighetsland).isEqualTo(Landkoder.DE);
    }

    @Test
    void lag_innvilgelsesBrev_harBestillingsinformasjon() {
        BrevData brevData = brevDataByggerInnvilgelse.lag(lagBrevressurser(), saksbehandler);

        assertThat(brevData.begrunnelseKode).isEqualTo(brevbestillingRequest.getBegrunnelseKode());
        assertThat(brevData.fritekst).isEqualTo(brevbestillingRequest.getFritekst());
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
    }
}
