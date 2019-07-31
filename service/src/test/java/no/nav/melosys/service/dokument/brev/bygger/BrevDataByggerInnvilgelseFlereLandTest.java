package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
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
import no.nav.melosys.service.dokument.brev.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

        when(brevDataByggerA1.lag(any(), any())).thenReturn(new BrevDataA1());

        Lovvalgsperiode periode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(periode));

        when(landVelgerService.hentAlleArbeidsland(any())).thenReturn(Collections.singleton(Landkoder.AT));
        when(landVelgerService.hentTrygdemyndighetsland(any())).thenReturn(Landkoder.DE);
        when(landVelgerService.hentBostedsland(any(), any())).thenReturn(Landkoder.DE);

        brevDataByggerInnvilgelse = new BrevDataByggerInnvilgelseFlereLand(avklartefaktaService,
            avklarteVirksomheterService,
            landVelgerService,
            lovvalgsperiodeService,
            brevbestillingDto,
            brevDataByggerA1);
    }

    private static Saksopplysning lagSøknadsopplysning() {
        SoeknadDokument søknad = new SoeknadDokument();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(søknad);
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        return saksopplysning;
    }

    @Test
    public void lag_medSokkel_setterMaritimtypeSokkel() throws FunksjonellException, TekniskException {
        Maritimtyper maritimType = Maritimtyper.SOKKEL;
        when(avklartefaktaService.hentMaritimType(anyLong())).thenReturn(Optional.of(maritimType));

        BrevDataInnvilgelseFlereLand brevData = (BrevDataInnvilgelseFlereLand) brevDataByggerInnvilgelse.lag(behandling, saksbehandler);
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
        assertThat(brevData.avklartMaritimType).isEqualTo(Maritimtyper.SOKKEL);
    }

    @Test
    public void lag_utenMaritimtArbeid_setterMaritimtypeTilNull() throws FunksjonellException, TekniskException {
        when(avklartefaktaService.hentMaritimType(anyLong())).thenReturn(Optional.empty());

        BrevDataInnvilgelseFlereLand brevData = (BrevDataInnvilgelseFlereLand) brevDataByggerInnvilgelse.lag(behandling, saksbehandler);
        assertThat(brevData.avklartMaritimType).isNull();
    }

    @Test
    public void lag_innvilgelsesBrev_harBestillingsinformasjon() throws FunksjonellException, TekniskException {
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.mottaker = Aktoersroller.BRUKER;
        brevbestillingDto.begrunnelseKode = "BEGRUNNELSEKODE";
        brevbestillingDto.fritekst = "FRITEKST";

        BrevDataBygger brevDataByggerInnvilgelse =
            new BrevDataByggerInnvilgelseFlereLand(avklartefaktaService, avklarteVirksomheterService, landVelgerService, lovvalgsperiodeService, brevbestillingDto, brevDataByggerA1);

        BrevData brevData = brevDataByggerInnvilgelse.lag(behandling, saksbehandler);
        assertThat(brevData).isEqualToComparingOnlyGivenFields(brevbestillingDto, "begrunnelseKode", "fritekst");
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
    }
}