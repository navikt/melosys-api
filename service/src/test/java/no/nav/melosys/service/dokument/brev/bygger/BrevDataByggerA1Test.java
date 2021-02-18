package no.nav.melosys.service.dokument.brev.bygger;

import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.SelvstendigForetak;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagSystemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.SaksopplysningStubs.lagArbeidsforholdOpplysning;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevDataByggerA1Test {
    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private LandvelgerService landvelgerService;
    @Mock
    RegisterOppslagSystemService registerOppslagService;

    private Set<String> avklarteOrganisasjoner;
    private Soeknad søknad;
    private BrevDataGrunnlag dataGrunnlag;
    private BrevDataByggerA1 brevDataByggerA1;

    private final String saksbehandler = "";
    private final String orgnr2 = "10987654321";

    @BeforeEach
    void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Behandling behandling = new Behandling();
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

        søknad = new Soeknad();
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

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService,
            registerOppslagService,
            mock(BehandlingService.class),
                kodeverkService);
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder().medBehandling(behandling).build();
        dataGrunnlag = new BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
        brevDataByggerA1 = new BrevDataByggerA1(avklartefaktaService, landvelgerService);
    }

    private void mockAvklarteOrganisasjoner(List<String> orgnumre) throws IkkeFunnetException, IntegrasjonException {
        avklarteOrganisasjoner.addAll(orgnumre);
        OrganisasjonsDetaljer detaljer = mock(OrganisasjonsDetaljer.class);
        when(detaljer.hentStrukturertForretningsadresse()).thenReturn(lagStrukturertAdresse());

        Set<OrganisasjonDokument> organisasjonDokumenter = new HashSet<>();
        for (String orgnr : orgnumre) {
            organisasjonDokumenter.add(leggTilTestorganisasjon("navn" + orgnr, orgnr, detaljer));
        }

        when(registerOppslagService.hentOrganisasjoner(any()))
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
    void lag_brukAlleArbeidsland() throws FunksjonellException, TekniskException {
        mockAvklarteOrganisasjoner(Collections.singletonList("1"));
        brevDataByggerA1.lag(dataGrunnlag, saksbehandler);

        verify(landvelgerService).hentAlleArbeidsland(anyLong());
    }

    @Test
    void lag_sjekkAvklarteSelvstendigeForetak() throws FunksjonellException, TekniskException {
        mockAvklarteOrganisasjoner(Collections.singletonList("999"));
        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = "999";
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        BrevDataA1 brevDataDto = (BrevDataA1) brevDataByggerA1.lag(dataGrunnlag, saksbehandler);
        assertThat(brevDataDto.hovedvirksomhet.orgnr).isEqualTo(foretak.orgnr);
    }

    @Test
    void lag_hentAvklarteArbeidsgivere() throws FunksjonellException, TekniskException {
        mockAvklarteOrganisasjoner(Collections.singletonList("7777"));
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
    void lag_ArbeidsstedHosOppdragsgiver_girUtenlandskvirksomhet() throws FunksjonellException, TekniskException {
        mockAvklarteOrganisasjoner(Collections.singletonList(orgnr2));

        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.virksomhetNavn = "Utenlandsk Oppdragsgiver LTD";
        fysiskArbeidssted.adresse = lagStrukturertAdresse();
        søknad.arbeidPaaLand.fysiskeArbeidssteder.add(fysiskArbeidssted);

        BrevDataA1 brevDataDto = (BrevDataA1) brevDataByggerA1.lag(dataGrunnlag, saksbehandler);
        assertThat(brevDataDto.bivirksomheter.stream().map(uv -> uv.navn)).contains(fysiskArbeidssted.virksomhetNavn);
        assertThat(brevDataDto.arbeidssteder.stream()
            .filter(no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted::erFysisk)
            .map(no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted.class::cast)
            .map(no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted::getAdresse)).contains(fysiskArbeidssted.adresse);
    }
}