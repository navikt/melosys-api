package no.nav.melosys.service.persondata;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.Personstatus;
import no.nav.melosys.domain.kodeverk.Personstatuser;
import no.nav.melosys.domain.person.*;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.identer.Ident;
import no.nav.melosys.integrasjon.pdl.dto.identer.Identliste;
import no.nav.melosys.integrasjon.pdl.dto.person.Adressebeskyttelse;
import no.nav.melosys.integrasjon.pdl.dto.person.AdressebeskyttelseGradering;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokgenTestData;
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.familie.FamiliemedlemService;
import no.nav.melosys.service.persondata.mapping.FamiliemedlemOversetter;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.integrasjon.pdl.dto.identer.IdentGruppe.*;
import static no.nav.melosys.service.SaksbehandlingDataFactory.*;
import static no.nav.melosys.service.persondata.PdlObjectFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
    private FamiliemedlemService familiemedlemService;

    private PersondataService persondataService;

    @BeforeEach
    public void setup() {
        persondataService = new PersondataService(behandlingService, kodeverkService, pdlConsumer,
            saksopplysningerService, familiemedlemService);
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
        String forventetRelatertVedSivilstandID = "forventetRelatertVedSivilstandID";

        when(pdlConsumer.hentPerson(anyString())).thenReturn(lagPerson());
        when(familiemedlemService.hentFamiliemedlemmer(lagPerson())).thenReturn(
            Set.of(
                FamiliemedlemOversetter.oversettBarn(lagPerson(), lagFolkeregisterIdent("identForelder1")),
                FamiliemedlemOversetter.oversettEktefelleEllerPartner(lagPerson(),
                    lagSivilstand(forventetRelatertVedSivilstandID))
            ));


        Personopplysninger persondata = (Personopplysninger) persondataService.hentPerson("IdNr",
            Informasjonsbehov.MED_FAMILIERELASJONER);


        assertThat(persondata.bostedsadresse()).isNotNull();
        assertThat(persondata.dødsfall()).isEqualTo(new Doedsfall(LocalDate.MAX));
        assertThat(persondata.fødsel()).isEqualTo(new Foedsel(LocalDate.parse("1970-01-01"), 1970, "NOR", "fødested"));
        assertThat(persondata.folkeregisteridentifikator()).isEqualTo(lagFolkeregisterIdent("IdNr"));
        assertThat(persondata.kjønn()).isEqualTo(KjoennType.UKJENT);
        assertThat(persondata.navn()).isEqualTo(new Navn("fornavn", "mellomnavn", "etternavn"));
        assertThat(persondata.statsborgerskap()).containsExactlyInAnyOrder(
            new Statsborgerskap("AIA", null, LocalDate.parse("1979-11-18"),
                LocalDate.parse("1980-11-18"), "PDL", "Dolly", false),
            new Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null,
                null, "PDL", "Dolly", false));
        assertThat(persondata.familiemedlemmer()).isNotEmpty()
            .anyMatch(Familiemedlem::erBarn)
            .anyMatch(harForventetRelatertVedSivilstandId(forventetRelatertVedSivilstandID))
            .anyMatch(Familiemedlem::erRelatertVedSivilstand);
    }

    @NotNull
    private Predicate<Familiemedlem> harForventetRelatertVedSivilstandId(String forventetRelatertVedSivilstandID) {
        return familiemedlem -> familiemedlem.sivilstand() != null &&
            forventetRelatertVedSivilstandID.equals(familiemedlem.sivilstand().relatertVedSivilstand());
    }

    @NotNull
    private Folkeregisteridentifikator lagFolkeregisterIdent(String identForelder1) {
        return new Folkeregisteridentifikator(identForelder1);
    }

    @Test
    void hentPersonMedHistorikk_aktivBehandling_konverteringOk() {
        when(behandlingService.hentBehandling(1L)).thenReturn(lagBehandling());
        when(pdlConsumer.hentPersonMedHistorikk(anyString())).thenReturn(lagPerson());

        final var personMedHistorikk = persondataService.hentPersonMedHistorikk(1L);
        assertThat(personMedHistorikk.bostedsadresser()).isNotEmpty();
        assertThat(personMedHistorikk.dødsfall()).isEqualTo(new Doedsfall(LocalDate.MAX));
        assertThat(personMedHistorikk.fødsel()).isEqualTo(new Foedsel(LocalDate.parse("1970-01-01"), 1970, "NOR", "fødested"));
        assertThat(personMedHistorikk.folkeregisteridentifikator()).isEqualTo(lagFolkeregisterIdent("IdNr"));
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

    private PersonDokument lagPersonDokument(no.nav.melosys.domain.dokument.person.Sivilstand sivilstand) {
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
