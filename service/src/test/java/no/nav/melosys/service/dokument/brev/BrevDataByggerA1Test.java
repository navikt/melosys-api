package no.nav.melosys.service.dokument.brev;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerA1;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerA1Test {

    @Mock
    private AvklartefaktaService avklartefaktaService;

    @Mock
    private EregFasade ereg;

    @Mock
    private Behandling behandling;

    private Set<String> avklarteOrganisasjoner;

    private SoeknadDokument søknad;

    private BrevDataByggerA1 brevDataByggerA1;

    private String saksbehandler = "";

    private String orgnr1 = "12345678910";
    private String orgnr2 = "10987654321";

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        RegisterOppslagSystemService registerOppslagService = new RegisterOppslagSystemService(ereg, mock(TpsFasade.class));

        avklarteOrganisasjoner = new HashSet<>();
        when(avklartefaktaService.hentAvklarteOrganisasjoner(anyLong())).thenReturn(avklarteOrganisasjoner);

        søknad = new SoeknadDokument();
        Saksopplysning soeknad = new Saksopplysning();
        soeknad.setDokument(søknad);
        soeknad.setType(SaksopplysningType.SØKNAD);

        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = orgnr1;
        søknad.foretakUtland.add(foretakUtland);

        Saksopplysning person = new Saksopplysning();
        PersonDokument personDok = new PersonDokument();
        person.setDokument(personDok);
        person.setType(SaksopplysningType.PERSONOPPLYSNING);
        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Arrays.asList(soeknad, person)));

        OrganisasjonsDetaljer detaljer = mock(OrganisasjonsDetaljer.class);
        when(detaljer.hentStrukturertForretningsadresse()).thenReturn(new StrukturertAdresse());

        leggTilTestorganisasjon("navn1", orgnr1, detaljer);
        leggTilTestorganisasjon("navn2", orgnr2, detaljer);

        KodeverkService kodeverkService = mock(KodeverkService.class);
        when(kodeverkService.dekod(any(), any(), any())).thenReturn("Oslo");

        brevDataByggerA1 = new BrevDataByggerA1(avklartefaktaService, registerOppslagService, kodeverkService);
    }

    private void leggTilTestorganisasjon(String navn, String orgnummer, OrganisasjonsDetaljer detaljer) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        OrganisasjonDokument org = new OrganisasjonDokument();
        org.setOrgnummer(orgnummer);
        org.setOrganisasjonDetaljer(detaljer);
        org.setNavn(Arrays.asList(navn));
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ORGANISASJON);
        saksopplysning.setDokument(org);
        when(ereg.hentOrganisasjon(eq(orgnummer))).thenReturn(saksopplysning);
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

        BrevDataA1 brevDataDto = (BrevDataA1) brevDataByggerA1.lag(behandling, saksbehandler);
        assertThat(brevDataDto.selvstendigeForetak).containsOnly(foretak.orgnr);
    }

    @Test
    public void testHentAvklarteNorskeForetak() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        avklarteOrganisasjoner.add(orgnr1);
        avklarteOrganisasjoner.add(orgnr2);

        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = orgnr1;
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.add(orgnr2);

        BrevDataA1 brevDataDto = (BrevDataA1) brevDataByggerA1.lag(behandling, saksbehandler);
        assertThat(brevDataDto.selvstendigeForetak).containsOnly(orgnr1);
        assertThat(brevDataDto.norskeVirksomheter.stream()
                .map(nv -> nv.orgnr)
                .collect(Collectors.toList())).containsOnly(orgnr1, orgnr2);
    }

    @Test(expected = TekniskException.class)
    public void testForetakiUtlandetSkalKasteException() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        avklarteOrganisasjoner.add(orgnr1);
        søknad.foretakUtland.clear();

        brevDataByggerA1.lag(behandling, saksbehandler);
    }

    @Test
    public void testIngenAvklarteforetak() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = orgnr1;
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        BrevDataA1 brevDataDto = (BrevDataA1) brevDataByggerA1.lag(behandling, saksbehandler);
        assertThat(brevDataDto.selvstendigeForetak).isEmpty();
        // TODO: Orgnr ikke obligatorisk registrert for utenlandske foretak
        //assertThat(brevDataDto.utenlandskeVirksomheter).isEmpty();
    }
}