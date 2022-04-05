package no.nav.melosys.service.persondata;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.Personstatus;
import no.nav.melosys.domain.dokument.person.Sivilstand;
import no.nav.melosys.domain.kodeverk.Personstatuser;
import no.nav.melosys.domain.person.*;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.domain.person.familie.Familierelasjon;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.identer.Ident;
import no.nav.melosys.integrasjon.pdl.dto.identer.Identliste;
import no.nav.melosys.integrasjon.pdl.dto.person.Adressebeskyttelse;
import no.nav.melosys.integrasjon.pdl.dto.person.AdressebeskyttelseGradering;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokgenTestData;
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.integrasjon.pdl.dto.identer.IdentGruppe.*;
import static no.nav.melosys.service.SaksbehandlingDataFactory.*;
import static no.nav.melosys.service.persondata.PdlObjectFactory.lagPerson;
import static no.nav.melosys.service.persondata.PdlObjectFactory.metadata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersondataServiceTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private KodeverkService kodeverkService;
    @Mock
    private PDLConsumer pdlConsumer;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private TpsService tpsService;

    private PersondataService persondataService;

    @BeforeEach
    public void setup() {
        persondataService = new PersondataService(behandlingService, kodeverkService, pdlConsumer,
                                                  saksopplysningerService, tpsService);
    }

    @Test
    void hentAktørID_finnes_verifiserAktørId() {
        when(pdlConsumer.hentIdenter(anyString())).thenReturn(lagIdentliste());
        assertThat(persondataService.hentAktørIdForIdent("123")).isEqualTo("11111");
    }

    @Test
    void hentAktørID_finnesIkke_feiler() {
        when(pdlConsumer.hentIdenter("321")).thenReturn(lagTomIdentliste());
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> persondataService.hentAktørIdForIdent("321"))
            .withMessageContaining("Finner ikke aktørID");
    }

    @Test
    void hentFolkeregisterIdent_finnes_verifiserIdent() {
        when(pdlConsumer.hentIdenter(anyString())).thenReturn(lagIdentliste());
        assertThat(persondataService.hentFolkeregisterident("123")).isEqualTo("22222");
    }

    @Test
    void hentFolkeregisterIdent_finnesIkke_feiler() {
        when(pdlConsumer.hentIdenter(anyString())).thenReturn(lagTomIdentliste());
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> persondataService.hentFolkeregisterident("123"))
            .withMessageContaining("Finner ikke folkeregisterident");
    }

    @Test
    void hentPersonMedFamilie() {
        when(pdlConsumer.hentPerson(anyString())).thenReturn(lagPerson());
        when(pdlConsumer.hentBarn(anyString())).thenReturn(lagPerson());
        when(pdlConsumer.hentRelatertVedSivilstand(anyString())).thenReturn(lagPerson());

        final Personopplysninger persondata = (Personopplysninger) persondataService.hentPerson("ident",
            Informasjonsbehov.MED_FAMILIERELASJONER);

        assertThat(persondata.bostedsadresse()).isNotNull();
        assertThat(persondata.dødsfall()).isEqualTo(new Doedsfall(LocalDate.MAX));
        assertThat(persondata.fødsel()).isEqualTo(new Foedsel(LocalDate.parse("1970-01-01"), 1970, "NOR", "fødested"));
        assertThat(persondata.folkeregisteridentifikator()).isEqualTo(new Folkeregisteridentifikator("IdNr"));
        assertThat(persondata.kjønn()).isEqualTo(KjoennType.UKJENT);
        assertThat(persondata.navn()).isEqualTo(new Navn("fornavn", "mellomnavn", "etternavn"));
        assertThat(persondata.statsborgerskap()).containsExactlyInAnyOrder(
            new Statsborgerskap("AIA", null, LocalDate.parse("1979-11-18"),
                LocalDate.parse("1980-11-18"), "PDL", "Dolly", false),
            new Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null,
                null, "PDL", "Dolly", false));
        assertThat(persondata.familiemedlemmer()).isNotEmpty()
            .anyMatch(Familiemedlem::erBarn)
            .anyMatch(Familiemedlem::erRelatertVedSivilstand);
    }

    @Test
    void hentPersonMedHistorikk_aktivBehandling_konverteringOk() {
        when(behandlingService.hentBehandling(1L)).thenReturn(lagBehandling());
        when(pdlConsumer.hentPersonMedHistorikk(anyString())).thenReturn(lagPerson());

        final var personMedHistorikk = persondataService.hentPersonMedHistorikk(1L);
        assertThat(personMedHistorikk.bostedsadresser()).isNotEmpty();
        assertThat(personMedHistorikk.dødsfall()).isEqualTo(new Doedsfall(LocalDate.MAX));
        assertThat(personMedHistorikk.fødsel()).isEqualTo(new Foedsel(LocalDate.parse("1970-01-01"), 1970, "NOR", "fødested"));
        assertThat(personMedHistorikk.folkeregisteridentifikator()).isEqualTo(new Folkeregisteridentifikator("IdNr"));
        assertThat(personMedHistorikk.folkeregisterpersonstatuser()).map(Folkeregisterpersonstatus::personstatus).containsExactly(Personstatuser.IKKE_BOSATT);
        assertThat(personMedHistorikk.kjønn()).isEqualTo(KjoennType.UKJENT);
        assertThat(personMedHistorikk.navn()).isEqualTo(new Navn("fornavn", "mellomnavn", "etternavn"));
        assertThat(personMedHistorikk.statsborgerskap()).containsExactlyInAnyOrder(
            new Statsborgerskap("AIA", null, LocalDate.parse("1979-11-18"),
                LocalDate.parse("1980-11-18"), "PDL", "Dolly", false),
            new Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null,
                null, "PDL", "Dolly", false)
        );
    }

    @Test
    void hentPersonMedHistorikk_inaktivBehandling_inaktivBehandlingFraFørPdl() {
        var inaktivBehandling = lagInaktivBehandlingSomIkkeResulterIVedtak();
        when(behandlingService.hentBehandling(1L)).thenReturn(inaktivBehandling);
        no.nav.melosys.domain.dokument.person.Sivilstand sivilstand = spy(no.nav.melosys.domain.dokument.person.Sivilstand.class);
        when(sivilstand.getKode()).thenReturn("GLAD");
        when(saksopplysningerService.hentTpsPersonopplysninger(inaktivBehandling.getId())).thenReturn(lagPersonDokument(sivilstand));

        final var personMedHistorikk = persondataService.hentPersonMedHistorikk(1L);
        assertThat(personMedHistorikk.statsborgerskap()).containsExactly(
            new Statsborgerskap("NOR", null, LocalDate.parse("1989-08-07"),
                                null, "TPS", "TPS", false)
        );
    }

    private static PersonDokument lagPersonDokument(no.nav.melosys.domain.dokument.person.Sivilstand sivilstand) {
        PersonDokument person = new PersonDokument();
        person.setKjønn(new KjoennsType("K"));
        person.setFornavn("Kari");
        person.setMellomnavn("Mellom");
        person.setEtternavn("Nordmann");
        person.setFødselsdato(LocalDate.parse("1989-08-07"));
        person.setFnr("123456789");
        person.setBostedsadresse(BrevDataTestUtils.lagBostedsadresse());
        person.setPostadresse(DokgenTestData.lagAdresse());
        person.setPersonstatus(Personstatus.ABNR);
        person.setSivilstand(sivilstand);
        person.setSivilstandGyldighetsperiodeFom(LocalDate.parse("2019-08-07"));
        person.setStatsborgerskap(new Land(Land.NORGE));
        person.setStatsborgerskapDato(LocalDate.parse("1989-08-07"));
        person.setDødsdato(LocalDate.parse("2089-08-07"));
        return person;
    }

    @Test
    void hentPersonMedHistorikk_inaktivBehandling_returnerDataFraPDL() {
        when(behandlingService.hentBehandling(1L)).thenReturn(lagInaktivBehandling());
        when(saksopplysningerService.finnPdlPersonhistorikkTilSaksbehandler(1L)).thenReturn(Optional.of(PersonopplysningerObjectFactory.lagPersonMedHistorikk()));

        final var personMedHistorikk = persondataService.hentPersonMedHistorikk(1L);
        assertThat(personMedHistorikk.statsborgerskap()).containsExactlyInAnyOrder(
            new Statsborgerskap("AAA", null, LocalDate.parse("2009-11-18"),
                LocalDate.parse("1980-11-18"), "PDL", "Dolly", false),
            new Statsborgerskap("BBB", null, LocalDate.parse("1979-11-18"),
                LocalDate.parse("1980-11-18"), "PDL", "Dolly", false),
            new Statsborgerskap("CCC", null, null,
                LocalDate.parse("1980-11-18"), "PDL", "Dolly", false)
        );
    }

    @Test
    void hentPersonMedHistorikk_inaktivBehandlingTPSDataLagret_returnererDataFraTps() {
        when(behandlingService.hentBehandling(1L)).thenReturn(lagInaktivBehandling());
        when(saksopplysningerService.finnPdlPersonhistorikkTilSaksbehandler(1L)).thenReturn(Optional.empty());
        when(saksopplysningerService.hentTpsPersonopplysninger(1L)).thenReturn(lagPersonDokument(null));

        final var personMedHistorikk = persondataService.hentPersonMedHistorikk(1L);
        assertThat(personMedHistorikk.statsborgerskap()).containsExactly(
            new Statsborgerskap("NOR", null, LocalDate.parse("1989-08-07"),
                null, "TPS", "TPS", false)
        );
    }

    @Test
    void hentFamiliemedlemmerMedHistorikk_aktivBehandling() {
        when(behandlingService.hentBehandling(1L)).thenReturn(lagBehandling());
        when(pdlConsumer.hentFamilierelasjoner(anyString())).thenReturn(lagPerson());
        when(pdlConsumer.hentBarn("barnIdent")).thenReturn(lagPerson());
        when(pdlConsumer.hentRelatertVedSivilstand("relatertVedSivilstandID")).thenReturn(lagPerson());

        final Set<Familiemedlem> familiemedlemmer = persondataService.hentFamiliemedlemmerMedHistorikk(1L);
        assertThat(familiemedlemmer).extracting(Familiemedlem::familierelasjon).contains(Familierelasjon.BARN,
            Familierelasjon.RELATERT_VED_SIVILSTAND);
    }

    @Test
    void hentFamiliemedlemmerMedHistorikk_inaktivBehandling() {
        final var inaktivBehandling = lagInaktivBehandlingSomIkkeResulterIVedtak();
        when(behandlingService.hentBehandling(1L)).thenReturn(inaktivBehandling);
        when(saksopplysningerService.harTpsPersonopplysninger(1L)).thenReturn(false);
        when(saksopplysningerService.hentPdlPersonopplysninger(1L)).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerMedFamilie());

        final Set<Familiemedlem> familiemedlemmer = persondataService.hentFamiliemedlemmerMedHistorikk(1L);
        assertThat(familiemedlemmer).extracting(Familiemedlem::familierelasjon).contains(Familierelasjon.BARN,
            Familierelasjon.RELATERT_VED_SIVILSTAND);
    }

    @Test
    void hentFamiliemedlemmerMedHistorikk_inaktivBehandlingMedTpsData() {
        var inaktivBehandling = lagInaktivBehandlingSomIkkeResulterIVedtak();
        when(behandlingService.hentBehandling(1L)).thenReturn(inaktivBehandling);
        no.nav.melosys.domain.dokument.person.Sivilstand sivilstand = mock(no.nav.melosys.domain.dokument.person.Sivilstand.class);
        when(sivilstand.getKode()).thenReturn("BLA");
        when(saksopplysningerService.harTpsPersonopplysninger(1L)).thenReturn(true);
        when(saksopplysningerService.hentTpsPersonopplysninger(inaktivBehandling.getId())).thenReturn(lagPersonDokumentMedFamiliemedlemmer(sivilstand));

        final Set<Familiemedlem> familiemedlemmer = persondataService.hentFamiliemedlemmerMedHistorikk(1L);
        assertThat(familiemedlemmer).extracting(Familiemedlem::navn).extracting(Navn::fornavn).contains("BARN", "NAVN");
        assertThat(familiemedlemmer).extracting(Familiemedlem::familierelasjon).contains(Familierelasjon.BARN,
                                                                                         Familierelasjon.RELATERT_VED_SIVILSTAND);
    }

    private PersonDokument lagPersonDokumentMedFamiliemedlemmer(Sivilstand sivilstand) {
        PersonDokument person = new PersonDokument();
        List<no.nav.melosys.domain.dokument.person.Familiemedlem> familiemedlemmer = new ArrayList<>();

        familiemedlemmer.add(lagFamilemedlem("NAVN NAVNSEN", "354652678134", no.nav.melosys.domain.dokument.person.Familierelasjon.EKTE,
                                             sivilstand));
        familiemedlemmer.add(lagFamilemedlem("BARN NAVNSEN", "134354652678", no.nav.melosys.domain.dokument.person.Familierelasjon.BARN,
                                             null));
        person.setFamiliemedlemmer(familiemedlemmer);
        return person;
    }

    private no.nav.melosys.domain.dokument.person.Familiemedlem lagFamilemedlem(String navn, String fnr,
                                                                                no.nav.melosys.domain.dokument.person.Familierelasjon familierelasjon,
                                                                                Sivilstand sivilstand) {
        no.nav.melosys.domain.dokument.person.Familiemedlem familiemedlem = new no.nav.melosys.domain.dokument.person.Familiemedlem();
        familiemedlem.fnr = fnr;
        familiemedlem.navn = navn;
        familiemedlem.familierelasjon = familierelasjon;
        familiemedlem.fødselsdato = LocalDate.EPOCH;
        familiemedlem.fnrAnnenForelder = familierelasjon == no.nav.melosys.domain.dokument.person.Familierelasjon.BARN ? "fnrAnnen" : null;
        familiemedlem.sivilstand = sivilstand;
        return familiemedlem;
    }

    @Test
    void hentSammensatNavn() {
        when(pdlConsumer.hentNavn(anyString())).thenReturn(Set.of(
            new no.nav.melosys.integrasjon.pdl.dto.person.Navn("Fornavn", "Mellom", "Etternavnsen", metadata())
        ));

        assertThat(persondataService.hentSammensattNavn("")).isEqualTo("Etternavnsen Mellom Fornavn");
    }

    @Test
    void hentStatsborgerskap() {
        when(pdlConsumer.hentStatsborgerskap("ident")).thenReturn(Set.of(
            new no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap("AIA", LocalDate.parse("2021-05-08"), LocalDate.parse(
                "1979-11-18"),
                LocalDate.parse("1980-11-18"), metadata()))
        );

        assertThat(persondataService.hentStatsborgerskap("ident")).containsExactly(
            new Statsborgerskap(
                "AIA", LocalDate.parse("2021-05-08"), LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"),
                "PDL", "Dolly", false)
            );
    }

    @Test
    void harStrengtFortroligAdresse() {
        when(pdlConsumer.hentAdressebeskyttelser(anyString())).thenReturn(
            List.of(new Adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT, metadata()),
                new Adressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG, metadata())));

        assertThat(persondataService.harStrengtFortroligAdresse("")).isTrue();
    }

    private Identliste lagIdentliste() {
        var identliste = new Identliste(new HashSet<>());
        identliste.identer().add(new Ident("11111", AKTORID));
        identliste.identer().add(new Ident("22222", FOLKEREGISTERIDENT));
        identliste.identer().add(new Ident("33333", NPID));

        return identliste;
    }

    private Identliste lagTomIdentliste() {
        return new Identliste(Collections.emptySet());
    }
}
