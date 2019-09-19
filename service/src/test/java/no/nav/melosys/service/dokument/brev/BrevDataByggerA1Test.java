package no.nav.melosys.service.dokument.brev;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerA1;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.service.SaksopplysningStubs.lagArbeidsforholdOpplysning;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerA1Test {

    @Mock
    private AvklartefaktaService avklartefaktaService;

    @Mock
    private Behandling behandling;

    private Set<String> avklarteOrganisasjoner;

    private SoeknadDokument søknad;
    private DokumentdataGrunnlag dataGrunnlag;

    private BrevDataByggerA1 brevDataByggerA1;

    private String saksbehandler = "";

    private String orgnr1 = "12345678910";
    private String orgnr2 = "10987654321";

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {

        avklarteOrganisasjoner = new HashSet<>();

        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong()))
            .thenReturn(avklarteOrganisasjoner);

        StrukturertAdresse oppgittAdresse = new StrukturertAdresse();
        oppgittAdresse.gatenavn = "HjemmeGata";
        oppgittAdresse.husnummer = "23B";
        oppgittAdresse.postnummer = "0165";
        oppgittAdresse.poststed = "Oslo";
        oppgittAdresse.landkode = Landkoder.NO.getKode();

        søknad = new SoeknadDokument();
        søknad.bosted.oppgittAdresse = oppgittAdresse;

        Saksopplysning soeknad = new Saksopplysning();
        soeknad.setDokument(søknad);
        soeknad.setType(SaksopplysningType.SØKNAD);

        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = orgnr1;
        foretakUtland.navn = "Utenlandsk arbeidsgiver AS";
        søknad.foretakUtland.add(foretakUtland);

        Saksopplysning person = new Saksopplysning();
        PersonDokument personDok = new PersonDokument();
        person.setDokument(personDok);
        person.setType(SaksopplysningType.PERSOPL);

        Saksopplysning arbeidsforhold = lagArbeidsforholdOpplysning(Collections.singletonList(orgnr1));

        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Arrays.asList(soeknad, person, arbeidsforhold)));

        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.gatenavn = "gate 12";
        strukturertAdresse.postnummer = "123";

        OrganisasjonsDetaljer detaljer = mock(OrganisasjonsDetaljer.class);
        when(detaljer.hentStrukturertForretningsadresse()).thenReturn(strukturertAdresse);

        Set<OrganisasjonDokument> organisasjonDokumenter = new HashSet<>();

        organisasjonDokumenter.add(leggTilTestorganisasjon("navn1", orgnr1, detaljer));
        organisasjonDokumenter.add(leggTilTestorganisasjon("navn2", orgnr2, detaljer));

        KodeverkService kodeverkService = mock(KodeverkService.class);
        when(kodeverkService.dekod(any(), any(), any())).thenReturn("Oslo");

        RegisterOppslagSystemService registerOppslagService = mock(RegisterOppslagSystemService.class);
        when(registerOppslagService.hentOrganisasjoner(avklarteOrganisasjoner))
            .thenReturn(organisasjonDokumenter);

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
        dataGrunnlag = new DokumentdataGrunnlag(behandling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
        brevDataByggerA1 = new BrevDataByggerA1(avklartefaktaService);
    }

    private OrganisasjonDokument leggTilTestorganisasjon(String navn, String orgnummer, OrganisasjonsDetaljer detaljer) {
        OrganisasjonDokument org = new OrganisasjonDokument();
        org.setOrgnummer(orgnummer);
        org.setOrganisasjonDetaljer(detaljer);
        org.setNavn(Collections.singletonList(navn));
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ORG);
        saksopplysning.setDokument(org);
        return org;
    }

    @Test
    public void testHentAvklarteSelvstendigeForetak() throws FunksjonellException, TekniskException {
        avklarteOrganisasjoner.add("12345678910");

        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = "12345678910";
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        SelvstendigForetak foretak2 = new SelvstendigForetak();
        foretak2.orgnr = "10987654321";
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak2);

        BrevDataA1 brevDataDto = (BrevDataA1) brevDataByggerA1.lag(dataGrunnlag, saksbehandler);
        assertThat(brevDataDto.selvstendigeForetak).containsOnly(foretak.orgnr);
    }


    @Test
    public void testIngenAvklarteforetak() throws FunksjonellException, TekniskException {
        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = orgnr1;
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        BrevDataA1 brevDataDto = (BrevDataA1) brevDataByggerA1.lag(dataGrunnlag, saksbehandler);
        assertThat(brevDataDto.selvstendigeForetak).isEmpty();
        // TODO: Orgnr ikke obligatorisk registrert for utenlandske foretak
        //assertThat(brevDataDto.utenlandskeVirksomheter).isEmpty();
    }

    private StrukturertAdresse lagStrukturertAdresse() {
        StrukturertAdresse oppgittAdresse = new StrukturertAdresse();
        oppgittAdresse.gatenavn = "HjemmeGata";
        oppgittAdresse.husnummer = "23B";
        oppgittAdresse.postnummer = "0165";
        oppgittAdresse.poststed = "Oslo";
        oppgittAdresse.landkode = Landkoder.NO.getKode();
        return oppgittAdresse;
    }

    @Test
    public void testArbeidsstedHosOppdragsgiver_girUtenlandskvirksomhet() throws FunksjonellException, TekniskException {
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.foretakNavn = "Utenlandsk Oppdragsgiver LTD";
        arbeidUtland.adresse = lagStrukturertAdresse();
        søknad.arbeidUtland.add(arbeidUtland);

        BrevDataA1 brevDataDto = (BrevDataA1) brevDataByggerA1.lag(dataGrunnlag, saksbehandler);
        assertThat(brevDataDto.bivirksomheter.stream().map(uv -> uv.navn)).contains(arbeidUtland.foretakNavn);
        assertThat(brevDataDto.arbeidssteder.stream()
            .filter(Arbeidssted::erFysisk)
            .map(FysiskArbeidssted.class::cast)
            .map(uv -> uv.getAdresse())).contains(arbeidUtland.adresse);
    }
}