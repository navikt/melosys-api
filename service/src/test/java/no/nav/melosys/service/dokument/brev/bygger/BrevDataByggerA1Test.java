package no.nav.melosys.service.dokument.brev.bygger;

import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
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
import no.nav.melosys.service.registeropplysninger.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
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

    private Behandling behandling;

    private Set<String> avklarteOrganisasjoner;

    @Mock
    RegisterOppslagSystemService registerOppslagService;

    private SoeknadDokument søknad;
    private BrevDataGrunnlag dataGrunnlag;

    private BrevDataByggerA1 brevDataByggerA1;

    private String saksbehandler = "";

    private String orgnr2 = "10987654321";

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        behandling = new Behandling();
        behandling.setId(123L);

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

        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = "12345678910";
        foretakUtland.navn = "Utenlandsk arbeidsgiver AS";
        søknad.foretakUtland.add(foretakUtland);

        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(søknad);

        Saksopplysning person = new Saksopplysning();
        PersonDokument personDok = new PersonDokument();
        person.setDokument(personDok);
        person.setType(SaksopplysningType.PERSOPL);

        Saksopplysning arbeidsforhold = lagArbeidsforholdOpplysning(Collections.singletonList(orgnr2));
        behandling.setSaksopplysninger(new HashSet<>(Arrays.asList(person, arbeidsforhold)));

        KodeverkService kodeverkService = mock(KodeverkService.class);
        when(kodeverkService.dekod(any(), any(), any())).thenReturn("Oslo");

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
        dataGrunnlag = new BrevDataGrunnlag(behandling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
        brevDataByggerA1 = new BrevDataByggerA1(avklartefaktaService);
    }

    private void lagAvklartOrganisasjoner(List<String> orgnumre) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        OrganisasjonsDetaljer detaljer = mock(OrganisasjonsDetaljer.class);
        when(detaljer.hentStrukturertForretningsadresse()).thenReturn(lagStrukturertAdresse());

        Set<OrganisasjonDokument> organisasjonDokumenter = new HashSet<>();
        for (String orgnr : orgnumre) {
            organisasjonDokumenter.add(leggTilTestorganisasjon("navn"+orgnr, orgnr, detaljer));
        }

        avklarteOrganisasjoner.addAll(orgnumre);
        when(registerOppslagService.hentOrganisasjoner(avklarteOrganisasjoner))
            .thenReturn(organisasjonDokumenter);
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
        lagAvklartOrganisasjoner(Collections.singletonList("999"));

        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = "999";
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        BrevDataA1 brevDataDto = (BrevDataA1) brevDataByggerA1.lag(dataGrunnlag, saksbehandler);
        assertThat(brevDataDto.hovedvirksomhet.orgnr).isEqualTo(foretak.orgnr);
    }

    @Test
    public void testHentAvklarteArbeidsgivere() throws FunksjonellException, TekniskException {
        lagAvklartOrganisasjoner(Collections.singletonList("7777"));
        søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.add("7777");

        BrevDataA1 brevDataDto = (BrevDataA1) brevDataByggerA1.lag(dataGrunnlag, saksbehandler);
        assertThat(brevDataDto.hovedvirksomhet.orgnr).isEqualTo("7777");
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
        lagAvklartOrganisasjoner(Collections.singletonList(orgnr2));

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.foretakNavn = "Utenlandsk Oppdragsgiver LTD";
        arbeidUtland.adresse = lagStrukturertAdresse();
        søknad.arbeidUtland.add(arbeidUtland);

        BrevDataA1 brevDataDto = (BrevDataA1) brevDataByggerA1.lag(dataGrunnlag, saksbehandler);
        assertThat(brevDataDto.bivirksomheter.stream().map(uv -> uv.navn)).contains(arbeidUtland.foretakNavn);
        assertThat(brevDataDto.arbeidssteder.stream()
            .filter(Arbeidssted::erFysisk)
            .map(FysiskArbeidssted.class::cast)
            .map(FysiskArbeidssted::getAdresse)).contains(arbeidUtland.adresse);
    }
}