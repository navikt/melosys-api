package no.nav.melosys.service.dokument.brev;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.felles.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerA001Test {

    @Mock
    private AvklartefaktaService avklartefaktaService;

    @Mock
    private RegisterOppslagSystemService registerOppslagService;

    @Mock
    private TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepo;

    @Mock
    private UtenlandskMyndighetRepository myndighetsRepo;

    @Mock
    private LovvalgsperiodeRepository lovvalgsperiodeRepo;

    @Mock
    private VilkaarsresultatRepository vilkårRepo;

    @Mock
    Behandling behandling;

    private Set<String> avklarteOrganisasjoner;

    private SoeknadDokument søknad;

    private BrevDataByggerA001 brevDataByggerA001;

    private String saksbehandler = "";

    private String orgnr1 = "12345678910";
    private String orgnr2 = "10987654321";
    private OrganisasjonDokument org1;
    private OrganisasjonDokument org2;

    @Before
    public void setUp() {
        avklarteOrganisasjoner = new HashSet<>();
        when(avklartefaktaService.hentAvklarteOrganisasjoner(anyLong())).thenReturn(avklarteOrganisasjoner);


        søknad = new SoeknadDokument();
        Saksopplysning soeknad = new Saksopplysning();
        soeknad.setDokument(søknad);
        soeknad.setType(SaksopplysningType.SØKNAD);

        Saksopplysning person = new Saksopplysning();
        PersonDokument personDok = new PersonDokument();
        person.setDokument(personDok);
        person.setType(SaksopplysningType.PERSONOPPLYSNING);
        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Arrays.asList(soeknad, person)));

        OrganisasjonsDetaljer detaljer = mock(OrganisasjonsDetaljer.class);
        when(detaljer.hentUstrukturertForretningsadresse()).thenReturn(new UstrukturertAdresse());

        org1 = new OrganisasjonDokument();
        org1.setOrgnummer(orgnr1);
        org1.setNavn(Arrays.asList("navn1"));
        org1.setOrganisasjonDetaljer(detaljer);

        org2 = new OrganisasjonDokument();
        org2.setOrgnummer(orgnr2);
        org2.setOrganisasjonDetaljer(detaljer);
        org2.setNavn(Arrays.asList("navn2"));

        KodeverkService kodeverkService = mock(KodeverkService.class);
        when(kodeverkService.dekod(any(), any(), any())).thenReturn("Oslo");
        brevDataByggerA001 = new BrevDataByggerA001(avklartefaktaService, registerOppslagService, kodeverkService, tidligereMedlemsperiodeRepo, myndighetsRepo, lovvalgsperiodeRepo, vilkårRepo);
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

        BrevDataA001 brevDataDto = (BrevDataA001) brevDataByggerA001.lag(behandling, saksbehandler);
        assertThat(brevDataDto.selvstendigeVirksomheter.stream()
                .map(nv -> nv.orgnr)).containsOnly(foretak.orgnr);
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

        BrevDataA001 brevDataDto = (BrevDataA001) brevDataByggerA001.lag(behandling, saksbehandler);
        assertThat(brevDataDto.selvstendigeVirksomheter.stream()
                .map(nv -> nv.orgnr)).containsOnly(orgnr1);
        assertThat(brevDataDto.arbeidsgivendeVirkomsheter.stream()
                .map(nv -> nv.orgnr)).containsOnly(orgnr1, orgnr2);
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

        BrevDataA001 brevDataDto = (BrevDataA001) brevDataByggerA001.lag(behandling, saksbehandler);

    }

    @Test
    public void testIngenForetak() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        avklarteOrganisasjoner.add(orgnr1);
        avklarteOrganisasjoner.add(orgnr2);

        BrevDataA001 brevDataDto = (BrevDataA001) brevDataByggerA001.lag(behandling, saksbehandler);
        assertThat(brevDataDto.selvstendigeVirksomheter).isEmpty();
        assertThat(brevDataDto.arbeidsgivendeVirkomsheter).isEmpty();
    }

    @Test
    public void testIngenAvklarteforetak() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = orgnr1;
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = orgnr1;
        søknad.foretakUtland.add(foretakUtland);

        BrevDataA001 brevDataDto = (BrevDataA001) brevDataByggerA001.lag(behandling, saksbehandler);
        assertThat(brevDataDto.arbeidsgivendeVirkomsheter).isEmpty();
        // TODO: Orgnr ikke obligatorisk registrert for utenlandske foretak
        //assertThat(brevDataDto.utenlandskeVirksomheter).isEmpty();
    }
}