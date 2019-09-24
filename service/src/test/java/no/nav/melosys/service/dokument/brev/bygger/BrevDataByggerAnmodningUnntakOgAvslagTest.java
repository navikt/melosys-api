package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.service.SaksopplysningStubs.lagSøknadOgArbeidsforholdOpplysninger;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagPersonsaksopplysning;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagStrukturertAdresse;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerAnmodningUnntakOgAvslagTest {
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

    private BrevDataByggerAnmodningUnntakOgAvslag brevDataByggerAnmodningUnntakOgAvslag;
    private AnmodningsperiodeSvar anmodningsperiodeSvar;

    @Before
    public void setUp() {
        anmodningsperiodeSvar = lagAnmodningsperiodeSvarAvslag();
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);
        anmodningsperiode.setSendtUtland(true);
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singletonList(anmodningsperiode));

        when(kodeverkService.dekod(any(), any(), any())).thenReturn("Oslo");
        brevDataByggerAnmodningUnntakOgAvslag = new BrevDataByggerAnmodningUnntakOgAvslag(landvelgerService, anmodningsperiodeService, vilkaarsresultatRepository);
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
        BrevDataAnmodningUnntakOgAvslag brevData = (BrevDataAnmodningUnntakOgAvslag) brevDataByggerAnmodningUnntakOgAvslag.lag(lagBrevressurser(behandling), saksbehandler);

        assertThat(brevData.hovedvirksomhet.orgnr).isEqualTo("999");
        assertThat(brevData.hovedvirksomhet.isSelvstendigForetak()).isEqualTo(true);
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