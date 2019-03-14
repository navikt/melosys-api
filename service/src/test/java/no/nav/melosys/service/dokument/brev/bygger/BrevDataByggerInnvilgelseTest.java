package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.avklartefakta.AvklartInnstallasjonsType;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
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
public class BrevDataByggerInnvilgelseTest {

    @Mock
    AvklartefaktaService avklartefaktaService;

    @Mock
    LovvalgsperiodeService lovvalgsperiodeService;

    @Mock
    BrevDataByggerA1 brevDataByggerA1;

    @Mock
    private Behandling behandling;

    @Mock
    private BrevbestillingDto brevbestillingDto;

    private SoeknadDokument søknad;
    private String saksbehandler = "saksbehandler";
    private BrevDataByggerInnvilgelse brevDataByggerInnvilgelse;

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        behandling.setId(1L);

        when(brevDataByggerA1.lag(any(), any())).thenReturn(new BrevDataA1());
        Lovvalgsperiode periode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(periode));

        søknad = new SoeknadDokument();
        Saksopplysning soeknad = new Saksopplysning();
        soeknad.setDokument(søknad);
        soeknad.setType(SaksopplysningType.SØKNAD);
        søknad.oppholdUtland.oppholdslandKoder.add("DE");
        søknad.bosted.oppgittAdresse = new StrukturertAdresse();
        søknad.bosted.oppgittAdresse.landKode = "DK";

        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Arrays.asList(soeknad)));

        brevDataByggerInnvilgelse = new BrevDataByggerInnvilgelse(avklartefaktaService,
                                                                  lovvalgsperiodeService,
                                                                  brevbestillingDto);
        brevDataByggerInnvilgelse.setA1Bygger(brevDataByggerA1);
    }

    @Test
    public void lag_medSokkel_setterInnstallasjonstypeSokkel() throws FunksjonellException, TekniskException {
        AvklartInnstallasjonsType innstallasjonsType = AvklartInnstallasjonsType.SOKKEL;
        when(avklartefaktaService.hentInnstallasjonsType(anyLong())).thenReturn(Optional.of(innstallasjonsType));

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(behandling, saksbehandler);
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
        assertThat(brevData.avklartSokkelEllerSkip).isEqualTo(AvklartInnstallasjonsType.SOKKEL);
    }

    @Test
    public void lag_utenMaritimtArbeid_setterInnstallasjonsTypeTilNull() throws FunksjonellException, TekniskException {
        when(avklartefaktaService.hentInnstallasjonsType(anyLong())).thenReturn(Optional.empty());

        BrevDataInnvilgelse brevData = (BrevDataInnvilgelse) brevDataByggerInnvilgelse.lag(behandling, saksbehandler);
        assertThat(brevData.avklartSokkelEllerSkip).isNull();
    }

    @Test
    public void lag_innvilgelsesBrev_harBestillingsinformasjon() throws FunksjonellException, TekniskException {
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.mottaker = Aktoersroller.BRUKER;
        brevbestillingDto.begrunnelseKode = "BEGRUNNELSEKODE";
        brevbestillingDto.fritekst = "FRITEKST";

        BrevDataByggerInnvilgelse brevDataByggerInnvilgelse =
            new BrevDataByggerInnvilgelse(avklartefaktaService, lovvalgsperiodeService, brevbestillingDto);
        brevDataByggerInnvilgelse.setA1Bygger(brevDataByggerA1);

        BrevData brevData = brevDataByggerInnvilgelse.lag(behandling, saksbehandler);
        assertThat(brevbestillingDto).isEqualToComparingFieldByField(brevData);
        assertThat(saksbehandler).isEqualTo(brevData.saksbehandler);
    }
}