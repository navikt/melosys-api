package no.nav.melosys.service.avklartefakta;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.service.SaksopplysningStubs.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AvklarteVirksomheterServiceTest {

    @Mock
    AvklartefaktaService avklartefaktaService;

    @Mock
    RegisterOppslagService registerOppslagService;

    @Mock
    Behandling behandling;

    AvklarteVirksomheterService avklarteVirksomheterService;

    String orgnr1 = "111111111";
    String orgnr2 = "222222222";
    String orgnr3 = "333333333";
    String uuid1 = "a2k2jf-a3khs";
    String uuid2 = "0dkf93-kj701";

    Function<OrganisasjonDokument, Adresse> ingenAdresse = org -> null;

    @Before
    public void setUp() {
        when(behandling.getId()).thenReturn(1L);
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(new HashSet<>(Arrays.asList(orgnr1, uuid1)));

        avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
    }

    @Test
    public void hentUtenlandskeVirksomheter_girListeMedKunAvklarteForetak() throws TekniskException {
        ForetakUtland foretak1 = lagForetakUtland("Utland1", uuid1, null);
        ForetakUtland foretak2 = lagForetakUtland("Utland2", uuid2, "SE-123456789");

        Saksopplysning søknad = lagSøknadOpplysning(Collections.emptyList(), Arrays.asList(foretak1, foretak2), Collections.emptyList());
        when(behandling.getSaksopplysninger()).thenReturn(Collections.singleton(søknad));

        List<AvklartVirksomhet> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling);
        assertThat(avklarteSelvstendigeOrgnumre.stream().map(av -> av.navn)).containsOnly("Utland1");
    }

    @Test
    public void hentUtenlandskeVirksomheter_girListeAvklartVirksomhetMedOrgnrIkkeUuid() throws TekniskException {
        ForetakUtland foretak1 = lagForetakUtland("Utland1", uuid1, "SE-123456789");

        Saksopplysning søknad = lagSøknadOpplysning(Collections.emptyList(), Collections.singletonList(foretak1), Collections.emptyList());
        when(behandling.getSaksopplysninger()).thenReturn(Collections.singleton(søknad));

        List<AvklartVirksomhet> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling);
        assertThat(avklarteSelvstendigeOrgnumre.stream().map(av -> av.orgnr)).containsOnly("SE-123456789");
    }

    private ForetakUtland lagForetakUtland(String navn, String uuid, String orgnr) {
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.navn = navn;
        foretakUtland.uuid = uuid;
        foretakUtland.orgnr = orgnr;
        return foretakUtland;
    }

    @Test
    public void hentSelvstendigeForetakOrgnumre_girListeMedKunAvklarteOrgnumre() throws TekniskException {
        List<String> selvstendigeForetak = Arrays.asList(orgnr1, orgnr2);
        Saksopplysning søknad = lagSøknadOpplysning(selvstendigeForetak, Collections.emptyList(), Collections.emptyList());
        when(behandling.getSaksopplysninger()).thenReturn(Collections.singleton(søknad));

        Set<String> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentSelvstendigeForetakOrgnumre(behandling);
        assertThat(avklarteSelvstendigeOrgnumre).containsOnly(orgnr1);
    }

    @Test
    public void hentArbeidsgivendeEkstraOrgnumre_girListeMedKunAvklarteOrgnumre() throws TekniskException {
        List<String> arbeidgivendeEkstraOrgnumre = Arrays.asList(orgnr2, orgnr1);
        Set<Saksopplysning> saksopplysninger =
            lagSøknadOgArbeidsforholdOpplysninger(Collections.emptyList(), arbeidgivendeEkstraOrgnumre, Collections.emptyList());
        when(behandling.getSaksopplysninger()).thenReturn(saksopplysninger);

        Set<String> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentArbeidsgivendeOrgnumre(behandling);
        assertThat(avklarteSelvstendigeOrgnumre).containsOnly(orgnr1);
    }

    @Test
    public void hentArbeidsgivendeRegistreOrgnumre_girListeMedKunAvklarteOrgnumre() throws TekniskException {
        List<String> arbeidgivendeOrgnumreEkstra = Arrays.asList(orgnr1, orgnr2, orgnr3);
        Set<Saksopplysning> saksopplysninger =
            lagSøknadOgArbeidsforholdOpplysninger(Collections.emptyList(), Collections.emptyList(), arbeidgivendeOrgnumreEkstra);
        when(behandling.getSaksopplysninger()).thenReturn(saksopplysninger);

        Set<String> avklarteSelvstendigeOrgnumre = avklarteVirksomheterService.hentArbeidsgivendeOrgnumre(behandling);
        assertThat(avklarteSelvstendigeOrgnumre).containsOnly(orgnr1);
    }

    @Test
    public void testHentAvklarteNorskeForetak_girAvklarteArbeidsgivere() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        List<String> arbeidsgivereEkstra = Collections.singletonList(orgnr2);
        List<String> arbeidsgivereRegister = Collections.singletonList(orgnr3);

        Set<Saksopplysning> saksopplysninger =
            lagSøknadOgArbeidsforholdOpplysninger(Collections.emptyList(),
                                                  arbeidsgivereEkstra,
                                                  arbeidsgivereRegister);

        when(behandling.getSaksopplysninger()).thenReturn(saksopplysninger);

        Set<String> avklarteOrganisasjoner = new HashSet<>(Arrays.asList(orgnr2, orgnr3));
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(avklarteOrganisasjoner);

        leggTilIRegisterOppslag(Arrays.asList(orgnr2, orgnr3));

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
        assertThat(avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, ingenAdresse).stream()
            .map(nv -> nv.orgnr)
            .collect(Collectors.toList())).contains(orgnr2, orgnr3);
    }

    @Test
    public void testHentAvklarteNorskeForetak_girAvklarteSelvstendigeForetak() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        List<String> selvstendigeForetak = Collections.singletonList(orgnr1);

        Set<Saksopplysning> saksopplysninger =
            lagSøknadOgArbeidsforholdOpplysninger(selvstendigeForetak,
                                                  Collections.emptyList(),
                                                  Collections.emptyList());

        when(behandling.getSaksopplysninger()).thenReturn(saksopplysninger);

        Set<String> avklarteOrganisasjoner = new HashSet<>(selvstendigeForetak);
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(avklarteOrganisasjoner);

        leggTilIRegisterOppslag(selvstendigeForetak);

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
        assertThat(avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, ingenAdresse).stream()
            .map(nv -> nv.orgnr)
            .collect(Collectors.toList())).contains(orgnr1);
    }

    private void leggTilIRegisterOppslag(Collection<String> orgnumre) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        when(registerOppslagService.hentOrganisasjoner(eq(new HashSet<>(orgnumre)))).thenReturn(lagOrganisasjonDokumenter(orgnumre));
    }
}