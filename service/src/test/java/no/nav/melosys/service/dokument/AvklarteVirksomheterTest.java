package no.nav.melosys.service.dokument;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.felles.AvklarteVirksomheter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.service.dokument.felles.AvklarteVirksomheter.ingenAdresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AvklarteVirksomheterTest {

    @Mock
    AvklartefaktaService avklartefaktaService;

    @Mock
    RegisterOppslagService registerOppslagService;

    @Mock
    Behandling behandling;

    AvklarteVirksomheter avklarteVirksomheter;

    String orgnr1 = "111111111";
    String orgnr2 = "222222222";
    String orgnr3 = "333333333";

    @Before
    public void setUp() {
        when(behandling.getId()).thenReturn(1L);
        when(avklartefaktaService.hentAvklarteOrganisasjoner(anyLong())).thenReturn(new HashSet<>(Arrays.asList(orgnr1)));

        avklarteVirksomheter = new AvklarteVirksomheter(avklartefaktaService, registerOppslagService, behandling);
    }

    @Test
    public void hentSelvstendigeForetakOrgnumre_girListeMedKunAvklarteOrgnumre() throws TekniskException {
        List<String> selvstendigeForetak = Arrays.asList(orgnr1, orgnr2);
        Saksopplysning søknad = lagSøknadOpplysning(selvstendigeForetak, Collections.emptyList());
        when(behandling.getSaksopplysninger()).thenReturn(Collections.singleton(søknad));

        Set<String> avklarteSelvstendigeOrgnumre = avklarteVirksomheter.hentSelvstendigeForetakOrgnumre();
        assertThat(avklarteSelvstendigeOrgnumre).containsOnly(orgnr1);
    }

    @Test
    public void hentArbeidsgivendeEkstraOrgnumre_girListeMedKunAvklarteOrgnumre() throws TekniskException {
        List<String> arbeidgivendeEkstraOrgnumre = Arrays.asList(orgnr2, orgnr1);
        Set<Saksopplysning> saksopplysninger =
            lagSøknadOgArbeidsforholdOpplysninger(Collections.emptyList(), arbeidgivendeEkstraOrgnumre, Collections.emptyList());
        when(behandling.getSaksopplysninger()).thenReturn(saksopplysninger);

        Set<String> avklarteSelvstendigeOrgnumre = avklarteVirksomheter.hentArbeidsgivendeOrgnumre();
        assertThat(avklarteSelvstendigeOrgnumre).containsOnly(orgnr1);
    }

    @Test
    public void hentArbeidsgivendeRegistreOrgnumre_girListeMedKunAvklarteOrgnumre() throws TekniskException {
        List<String> arbeidgivendeOrgnumreEkstra = Arrays.asList(orgnr1, orgnr2, orgnr3);
        Set<Saksopplysning> saksopplysninger =
            lagSøknadOgArbeidsforholdOpplysninger(Collections.emptyList(), Collections.emptyList(), arbeidgivendeOrgnumreEkstra);
        when(behandling.getSaksopplysninger()).thenReturn(saksopplysninger);

        Set<String> avklarteSelvstendigeOrgnumre = avklarteVirksomheter.hentArbeidsgivendeOrgnumre();
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
        when(avklartefaktaService.hentAvklarteOrganisasjoner(anyLong())).thenReturn(avklarteOrganisasjoner);

        leggTilIRegisterOppslag(Arrays.asList(orgnr2, orgnr3));

        AvklarteVirksomheter avklarteVirksomheter = new AvklarteVirksomheter(avklartefaktaService, registerOppslagService, behandling);
        assertThat(avklarteVirksomheter.hentAlleNorskeVirksomheter(ingenAdresse).stream()
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
        when(avklartefaktaService.hentAvklarteOrganisasjoner(anyLong())).thenReturn(avklarteOrganisasjoner);

        leggTilIRegisterOppslag(selvstendigeForetak);

        AvklarteVirksomheter avklarteVirksomheter = new AvklarteVirksomheter(avklartefaktaService, registerOppslagService, behandling);
        assertThat(avklarteVirksomheter.hentAlleNorskeVirksomheter(ingenAdresse).stream()
            .map(nv -> nv.orgnr)
            .collect(Collectors.toList())).contains(orgnr1);
    }

    private void leggTilIRegisterOppslag(Collection<String> orgnumre) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        when(registerOppslagService.hentOrganisasjoner(eq(new HashSet<>(orgnumre)))).thenReturn(lagOrganisasjonDokumenter(orgnumre));
    }

    public static Saksopplysning lagArbeidsforholdOpplysning(List<String> registrereArbeidsgiverOrgnumre) {
        ArbeidsforholdDokument arbeidsforholdDokument = mock(ArbeidsforholdDokument.class);
        when(arbeidsforholdDokument.hentOrgnumre()).thenReturn(new HashSet<>(registrereArbeidsgiverOrgnumre));
        Saksopplysning arbeidsforhold = new Saksopplysning();
        arbeidsforhold.setDokument(arbeidsforholdDokument);
        arbeidsforhold.setType(SaksopplysningType.ARBEIDSFORHOLD);
        return arbeidsforhold;
    }

    public static Saksopplysning lagSøknadOpplysning(List<String> selvstendigeForetak, List<String> ekstraArbeidsgivere) {
        SoeknadDokument søknad = new SoeknadDokument();
        for (String orgnr : selvstendigeForetak) {
            SelvstendigForetak selvstendigForetak = new SelvstendigForetak();
            selvstendigForetak.orgnr = orgnr;
            søknad.selvstendigArbeid.selvstendigForetak.add(selvstendigForetak);
        }

        søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.addAll(ekstraArbeidsgivere);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(søknad);
        saksopplysning.setType(SaksopplysningType.SØKNAD);

        return saksopplysning;
    }

    public static Set<Saksopplysning> lagSøknadOgArbeidsforholdOpplysninger(List<String> selvstendigeForetak, List<String> ekstraArbeidsgivere, List<String> registrerteArbeidsgivere) {
        Saksopplysning søknad = lagSøknadOpplysning(selvstendigeForetak, ekstraArbeidsgivere);
        Saksopplysning arbeidsforhold = lagArbeidsforholdOpplysning(registrerteArbeidsgivere);
        return new HashSet<>(Arrays.asList(søknad, arbeidsforhold));
    }

    private static Set<OrganisasjonDokument> lagOrganisasjonDokumenter(Collection<String> organisasjonsnumre)  {
        Set<OrganisasjonDokument> organisasjonDokumenter = new HashSet<>();
        for (String orgnummer : organisasjonsnumre) {
            OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
            organisasjonDokument.setOrgnummer(orgnummer);
            organisasjonDokument.setNavn(Arrays.asList("Test:", orgnummer));
            organisasjonDokument.setOrganisasjonDetaljer(new OrganisasjonsDetaljer());
            organisasjonDokumenter.add(organisasjonDokument);
        }
        return organisasjonDokumenter;
    }
}
