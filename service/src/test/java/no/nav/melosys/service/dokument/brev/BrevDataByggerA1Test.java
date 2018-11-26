package no.nav.melosys.service.dokument.brev;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
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
public class BrevDataByggerA1Test {

    @Mock
    private AvklartefaktaService avklartefaktaService;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private RegisterOppslagService registerOppslagService;

    @Mock
    Behandling behandling;

    private Set<String> avklarteOrganisasjoner;

    private SoeknadDokument søknad;

    BrevDataByggerA1 brevDataByggerA1;

    String saksbehandler = "";

    String orgnr1 = "12345678910";
    String orgnr2 = "10987654321";
    OrganisasjonDokument org1;
    OrganisasjonDokument org2;

    @Before
    public void setUp() throws IkkeFunnetException {
        avklarteOrganisasjoner = new HashSet<>();
        when(avklartefaktaService.hentAvklarteOrganisasjoner(anyLong())).thenReturn(avklarteOrganisasjoner);
        when(behandlingRepository.findOneWithSaksopplysningerById(1L)).thenReturn(behandling);

        søknad = new SoeknadDokument();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(søknad);
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Arrays.asList(saksopplysning)));

        org1 = new OrganisasjonDokument();
        org1.setOrgnummer(orgnr1);
        org2 = new OrganisasjonDokument();
        org2.setOrgnummer(orgnr2);

        brevDataByggerA1 = new BrevDataByggerA1(avklartefaktaService, behandlingRepository, registerOppslagService);
    }

    @Test
    public void testHentAvklarteSelvstendigeForetak() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        avklarteOrganisasjoner.add("12345678910");

        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = "12345678910";
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        SelvstendigForetak foretak2 = new SelvstendigForetak();
        foretak2.orgnr = "10987654321";
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak2);

        BrevDataA1Dto brevDataDto = (BrevDataA1Dto) brevDataByggerA1.lag(1L, saksbehandler);
        assertThat(brevDataDto.selvstendigeForetak).containsOnly(foretak.orgnr);
    }

    @Test
    public void testHentAvklarteNorskeForetak() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        avklarteOrganisasjoner.add(orgnr1);
        avklarteOrganisasjoner.add(orgnr2);

        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = orgnr1;
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        when(registerOppslagService.hentOrganisasjoner(any())).thenReturn(new HashSet<>(Arrays.asList(org1, org2)));
        søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.add(orgnr2);

        BrevDataA1Dto brevDataDto = (BrevDataA1Dto) brevDataByggerA1.lag(1L, saksbehandler);
        assertThat(brevDataDto.selvstendigeForetak).containsOnly(orgnr1);
        assertThat(brevDataDto.norskeVirksomheter).containsOnly(org1, org2);
    }

    @Test
    public void testForetakiUtlandet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        avklarteOrganisasjoner.add(orgnr1);

        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = orgnr1;
        søknad.foretakUtland.add(foretakUtland);

        ForetakUtland foretakUtland2 = new ForetakUtland();
        foretakUtland2.orgnr = orgnr2;
        søknad.foretakUtland.add(foretakUtland2);

        BrevDataA1Dto brevDataDto = (BrevDataA1Dto) brevDataByggerA1.lag(1L, saksbehandler);
        assertThat(brevDataDto.utenlandskeVirksomheter).containsOnly(foretakUtland);
    }

    @Test
    public void testIngenForetak() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        avklarteOrganisasjoner.add(orgnr1);
        avklarteOrganisasjoner.add(orgnr2);

        BrevDataA1Dto brevDataDto = (BrevDataA1Dto) brevDataByggerA1.lag(1L, saksbehandler);
        assertThat(brevDataDto.selvstendigeForetak).isEmpty();
        assertThat(brevDataDto.norskeVirksomheter).isEmpty();
        assertThat(brevDataDto.utenlandskeVirksomheter).isEmpty();
    }

    @Test
    public void testIngenAvklarteforetak() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = orgnr1;
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = orgnr1;
        søknad.foretakUtland.add(foretakUtland);

        BrevDataA1Dto brevDataDto = (BrevDataA1Dto) brevDataByggerA1.lag(1L, saksbehandler);
        assertThat(brevDataDto.selvstendigeForetak).isEmpty();
        assertThat(brevDataDto.utenlandskeVirksomheter).isEmpty();
    }
}
