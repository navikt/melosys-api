package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelseFlereLand;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.ressurser.Brevressurser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagPersonsaksopplysning;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagSoeknadssaksopplysning;
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
    LovvalgsperiodeService lovvalgsperiodeService;

    @Mock
    LandvelgerService landVelgerService;

    @Mock
    BrevDataByggerA1 brevDataByggerA1;

    private Behandling behandling;

    @Mock
    private BrevbestillingDto brevbestillingDto;

    private String saksbehandler = "saksbehandler";
    private BrevDataBygger brevDataByggerInnvilgelse;

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
        behandling = new Behandling();
        behandling.setId(1L);
        behandling.getSaksopplysninger().add(lagSøknadsopplysning());
        behandling.getSaksopplysninger().add(lagPersonsopplysning());

        when(brevDataByggerA1.lag(any())).thenReturn(new BrevDataA1());

        Lovvalgsperiode periode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(periode));

        when(landVelgerService.hentAlleArbeidsland(any())).thenReturn(Collections.singleton(Landkoder.AT));
        when(landVelgerService.hentBostedsland(any(), any())).thenReturn(Landkoder.DE);

        Brevressurser brevdataressurser = lagBrevressurser();
        brevDataByggerInnvilgelse = new BrevDataByggerInnvilgelseFlereLand(brevdataressurser,
            avklartefaktaService,
            brevbestillingDto,
            brevDataByggerA1);
    }

    private Brevressurser lagBrevressurser() throws TekniskException {
        return new Brevressurser(behandling, null, landVelgerService, avklarteVirksomheterService, avklartefaktaService, lovvalgsperiodeService);
    }

    private static Saksopplysning lagSøknadsopplysning() {
        SoeknadDokument søknad = new SoeknadDokument();
        return lagSoeknadssaksopplysning(søknad);
    }

    private static Saksopplysning lagPersonsopplysning() {
        PersonDokument person = new PersonDokument();
        return lagPersonsaksopplysning(person);
    }

    @Test
    public void lag_medSokkel_setterMaritimtypeSokkel() throws FunksjonellException, TekniskException {
        Maritimtyper maritimType = Maritimtyper.SOKKEL;
        when(avklartefaktaService.hentMaritimType(anyLong())).thenReturn(Optional.of(maritimType));

        BrevDataInnvilgelseFlereLand brevData = (BrevDataInnvilgelseFlereLand) brevDataByggerInnvilgelse.lag(saksbehandler);
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
        assertThat(brevData.avklartMaritimType).isEqualTo(Maritimtyper.SOKKEL);
    }

    @Test
    public void lag_utenMaritimtArbeid_setterMaritimtypeTilNull() throws FunksjonellException, TekniskException {
        when(avklartefaktaService.hentMaritimType(anyLong())).thenReturn(Optional.empty());

        BrevDataInnvilgelseFlereLand brevData = (BrevDataInnvilgelseFlereLand) brevDataByggerInnvilgelse.lag(saksbehandler);
        assertThat(brevData.avklartMaritimType).isNull();
    }

    @Test
    public void lag_innvilgelsesBrev_harBestillingsinformasjon() throws FunksjonellException, TekniskException {
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.mottaker = Aktoersroller.BRUKER;
        brevbestillingDto.begrunnelseKode = "BEGRUNNELSEKODE";
        brevbestillingDto.fritekst = "FRITEKST";

        BrevDataBygger brevDataByggerInnvilgelse =
            new BrevDataByggerInnvilgelseFlereLand(lagBrevressurser(), avklartefaktaService, brevbestillingDto, brevDataByggerA1);

        BrevData brevData = brevDataByggerInnvilgelse.lag(saksbehandler);
        assertThat(brevData).isEqualToComparingOnlyGivenFields(brevbestillingDto, "begrunnelseKode", "fritekst");
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
    }
}