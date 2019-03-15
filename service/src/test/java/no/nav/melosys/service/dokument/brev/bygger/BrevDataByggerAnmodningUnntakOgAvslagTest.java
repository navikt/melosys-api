package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.service.SaksopplysningStubs.lagSøknadOgArbeidsforholdOpplysninger;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerAnmodningUnntakOgAvslagTest {
    @Mock
    AvklartefaktaService avklartefaktaService;

    @Mock
    RegisterOppslagService registerOppslagService;

    @Mock
    LandvelgerService landvelgerService;

    private BrevDataByggerAnmodningUnntakOgAvslag brevDataByggerAnmodningUnntakOgAvslag;

    @Before
    public void setUp() {
        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
        brevDataByggerAnmodningUnntakOgAvslag = new BrevDataByggerAnmodningUnntakOgAvslag(avklartefaktaService, avklarteVirksomheterService, landvelgerService);
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

        String saksbehandler = "saksbehandler";

        Set<String> orgSet = new HashSet<>(Collections.singletonList("987654321"));
        when(avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId())).thenReturn(orgSet);

        when(landvelgerService.hentArbeidsland(any())).thenReturn("Tyskland");

        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.setOrgnummer("999");

        when(registerOppslagService.hentOrganisasjoner(orgSet)).thenReturn(new HashSet<>(Collections.singletonList(organisasjonDokument)));

        BrevDataAnmodningUnntakOgAvslag brevData = (BrevDataAnmodningUnntakOgAvslag) brevDataByggerAnmodningUnntakOgAvslag.lag(behandling, saksbehandler);

        assertThat(brevData.hovedvirksomhet.orgnr).isEqualTo("999");
        assertThat(brevData.hovedvirksomhet.isSelvstendigForetak()).isEqualTo(true);
        assertThat(brevData.arbeidsland).isEqualTo(Landkoder.DE.getBeskrivelse());
    }
}