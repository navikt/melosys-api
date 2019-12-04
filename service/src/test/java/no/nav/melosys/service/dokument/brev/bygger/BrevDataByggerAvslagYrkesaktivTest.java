package no.nav.melosys.service.dokument.brev.bygger;

import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagYrkesaktiv;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning.KORT_OPPDRAG_RETUR_NORSK_AG;
import static no.nav.melosys.service.SaksopplysningStubs.lagSøknadOgArbeidsforholdOpplysninger;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerAvslagYrkesaktivTest {
    @Mock
    AvklartefaktaService avklartefaktaService;
    @Mock
    RegisterOppslagService registerOppslagService;
    @Mock
    VilkaarsresultatRepository vilkaarsresultatRepository;
    @Mock
    LandvelgerService landvelgerService;
    @Mock
    AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    KodeverkService kodeverkService;

    private BrevDataByggerAvslagYrkesaktiv brevDataByggerAvslagYrkesaktiv;
    private AnmodningsperiodeSvar anmodningsperiodeSvar;

    @Before
    public void setUp() {
        anmodningsperiodeSvar = lagAnmodningsperiodeSvarAvslag();
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);
        anmodningsperiode.setSendtUtland(true);
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singletonList(anmodningsperiode));

        when(vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(anyLong(), eq(Vilkaar.FO_883_2004_ART16_1)))
            .thenReturn(Optional.of(lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, KORT_OPPDRAG_RETUR_NORSK_AG)));

        when(kodeverkService.dekod(any(), any(), any())).thenReturn("Oslo");
        brevDataByggerAvslagYrkesaktiv = new BrevDataByggerAvslagYrkesaktiv(landvelgerService, anmodningsperiodeService, vilkaarsresultatRepository, new BrevbestillingDto());
    }

    @Test
    public void lag_annmodningUnntakBrev_avklarVirksomhetSomSelvstendigForetak() throws Exception {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        behandling.setFagsak(fagsak);

        List<String> selvstendigeForetak = Collections.singletonList("987654321");
        List<String> arbeidsgivereRegister = Collections.singletonList("123456789");

        Set<Saksopplysning> saksopplysninger =
            lagSøknadOgArbeidsforholdOpplysninger(selvstendigeForetak, Collections.emptyList(), arbeidsgivereRegister);
        behandling.setSaksopplysninger(saksopplysninger);
        behandling.getSaksopplysninger().add(lagPersonsaksopplysning(new PersonDokument()));

        Set<String> orgSet = new HashSet<>(Collections.singletonList("987654321"));
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.getId())).thenReturn(orgSet);

        when(landvelgerService.hentArbeidsland(anyLong())).thenReturn(Landkoder.DE);
        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.setOrgnummer("999");
        OrganisasjonsDetaljer organisasjonsDetaljer = mock(OrganisasjonsDetaljer.class);
        when(organisasjonsDetaljer.hentStrukturertForretningsadresse()).thenReturn(lagStrukturertAdresse());
        organisasjonDokument.organisasjonDetaljer = organisasjonsDetaljer;

        when(registerOppslagService.hentOrganisasjoner(orgSet)).thenReturn(new HashSet<>(Collections.singletonList(organisasjonDokument)));

        String saksbehandler = "saksbehandler";
        BrevDataAvslagYrkesaktiv brevData = (BrevDataAvslagYrkesaktiv) brevDataByggerAvslagYrkesaktiv.lag(lagBrevressurser(behandling), saksbehandler);

        assertThat(brevData.hovedvirksomhet.orgnr).isEqualTo("999");
        assertThat(brevData.hovedvirksomhet.erSelvstendigForetak()).isEqualTo(true);
        assertThat(brevData.arbeidsland).isEqualTo(Landkoder.DE.getBeskrivelse());
        assertThat(brevData.anmodningsperiodeSvar.get()).isEqualToComparingFieldByField(anmodningsperiodeSvar);
    }

    private AnmodningsperiodeSvar lagAnmodningsperiodeSvarAvslag() {
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setBegrunnelseFritekst("No tiendo");
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        return anmodningsperiodeSvar;
    }

    public BrevDataGrunnlag lagBrevressurser(Behandling behandling) throws TekniskException {
        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
        return new BrevDataGrunnlag(behandling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
    }
}