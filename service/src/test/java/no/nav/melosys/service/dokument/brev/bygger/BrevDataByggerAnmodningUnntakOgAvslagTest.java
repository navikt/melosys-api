package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerAnmodningUnntakOgAvslagTest {
    @Mock
    AvklartefaktaService avklartefaktaService;

    @Mock
    RegisterOppslagService registerOppslagService;

    private BrevDataByggerAnmodningUnntakOgAvslag brevDataByggerAnmodningUnntakOgAvslag;


    @Before
    public void setUp() {
        brevDataByggerAnmodningUnntakOgAvslag = new BrevDataByggerAnmodningUnntakOgAvslag(avklartefaktaService, registerOppslagService);
    }

    @Test
    public void lag_annmodningUntakkBrev_setterForendelseMottatt() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        behandling.setFagsak(fagsak);

        Saksopplysning saksopplysning = new Saksopplysning();
        SoeknadDokument soeknadDokument = new SoeknadDokument();
        soeknadDokument.selvstendigArbeid.erSelvstendig = true;
        SelvstendigForetak selvstendigForetak = new SelvstendigForetak();
        selvstendigForetak.orgnr = "999";
        soeknadDokument.selvstendigArbeid.selvstendigForetak = Collections.singletonList(selvstendigForetak);

        saksopplysning.setDokument(soeknadDokument);
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));

        String saksbehandler = "saksbehandler";

        Set<String> orgSet = new HashSet<>(Collections.singletonList("999"));
        when(avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId())).thenReturn(orgSet);

        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.setOrgnummer("999");

        when(registerOppslagService.hentOrganisasjoner(orgSet)).thenReturn(new HashSet<>(Collections.singletonList(organisasjonDokument)));

        BrevDataAnmodningUnntakOgAvslag brevData = (BrevDataAnmodningUnntakOgAvslag) brevDataByggerAnmodningUnntakOgAvslag.lag(behandling, saksbehandler);

        assertThat(brevData.hovedvirksomhet.orgnr).isEqualTo("999");
        assertThat(brevData.hovedvirksomhet.isSelvstendigForetak()).isEqualTo(true);

    }
}