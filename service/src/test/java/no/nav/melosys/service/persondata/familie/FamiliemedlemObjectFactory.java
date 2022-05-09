package no.nav.melosys.service.persondata.familie;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.integrasjon.pdl.dto.Endring;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;
import no.nav.melosys.integrasjon.pdl.dto.person.*;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Bostedsadresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Vegadresse;
import org.jetbrains.annotations.NotNull;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.integrasjon.pdl.dto.Endringstype.OPPRETT;

public class FamiliemedlemObjectFactory {

    public static final String IDENT_HOVEDPERSON = "30056928150";
    public static final String IDENT_PERSON_GIFT = "21075114491";
    public static final String PERSON_GIFT_FORNAVN = "BRÅKETE";
    public static final String IDENT_PERSON_GIFT_HISTORISK = "12028536819";
    public static final String IDENT_BARN = "66628536666";
    public static final String NULL_IDENT_NÅR_SKILT = null;
    public static final long BEHANDLING_ID = 1L;

    private FamiliemedlemObjectFactory() {
    }

    public static Person lagHovedpersonMedBarn() {
        return new Person(
            Collections.emptySet(),
            Set.of(lagNorskBostedsadresse()),
            Collections.emptySet(),
            Set.of(lagFødselsdatoForVoksen()),
            Set.of(new Folkeregisteridentifikator(IDENT_HOVEDPERSON, lagAktivMetadata())),
            Collections.emptySet(),
            lagForelderBarnRelasjoner(),
            Collections.emptySet(),
            Set.of(new Kjoenn(KjoennType.MANN, lagAktivMetadata())),
            Collections.emptySet(),
            Set.of(new Navn("KARAFFEL", "", "TRIVIELL", lagAktivMetadata())),
            Collections.emptySet(),
            Set.of(
                new Sivilstand(Sivilstandstype.GIFT,
                    IDENT_PERSON_GIFT,
                    LocalDate.of(2011, 2, 2),
                    null,
                    lagAktivMetadata(toLocalDateTime("2020-12-05T08:32:21"))),
                new Sivilstand(Sivilstandstype.GIFT,
                    IDENT_PERSON_GIFT_HISTORISK,
                    LocalDate.of(2019, 8, 3),
                    null,
                    lagAktivMetadata(toLocalDateTime("2022-04-27T14:57:56"))),
                new Sivilstand(Sivilstandstype.SEPARERT,
                    IDENT_PERSON_GIFT_HISTORISK,
                    LocalDate.of(2013, 1, 3),
                    null,
                    lagHistoriskMetadata(toLocalDateTime("2022-04-28T13:28:43"))),
                new Sivilstand(Sivilstandstype.SKILT,
                    NULL_IDENT_NÅR_SKILT,
                    LocalDate.of(2014, 4, 9),
                    null,
                    lagHistoriskMetadata(toLocalDateTime("2022-05-04T12:20:45")))
            ),
            Set.of(new Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null, null, lagAktivMetadata(LocalDateTime.MAX))),
            Collections.emptySet());
    }

    public static Person lagHovedperson() {
        return new Person(
            Collections.emptySet(),
            Set.of(lagNorskBostedsadresse()),
            Collections.emptySet(),
            Set.of(lagFødselsdatoForVoksen()),
            Set.of(new Folkeregisteridentifikator(IDENT_HOVEDPERSON, lagAktivMetadata())),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Set.of(new Kjoenn(KjoennType.MANN, lagAktivMetadata())),
            Collections.emptySet(),
            Set.of(new Navn("KARAFFEL", "", "TRIVIELL", lagAktivMetadata())),
            Collections.emptySet(),
            Set.of(
                new Sivilstand(Sivilstandstype.GIFT,
                    IDENT_PERSON_GIFT,
                    LocalDate.of(2011, 2, 2),
                    null,
                    lagAktivMetadata(toLocalDateTime("2020-12-05T08:32:21"))),
                new Sivilstand(Sivilstandstype.GIFT,
                    IDENT_PERSON_GIFT_HISTORISK,
                    LocalDate.of(2019, 8, 3),
                    null,
                    lagAktivMetadata(toLocalDateTime("2022-04-27T14:57:56"))),
                new Sivilstand(Sivilstandstype.SEPARERT,
                    IDENT_PERSON_GIFT_HISTORISK,
                    LocalDate.of(2013, 1, 3),
                    null,
                    lagHistoriskMetadata(toLocalDateTime("2022-04-28T13:28:43"))),
                new Sivilstand(Sivilstandstype.SKILT,
                    NULL_IDENT_NÅR_SKILT,
                    LocalDate.of(2014, 4, 9),
                    null,
                    lagHistoriskMetadata(toLocalDateTime("2022-05-04T12:20:45")))
            ),
            Set.of(new Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null, null, lagAktivMetadata(LocalDateTime.MAX))),
            Collections.emptySet());
    }

    public static Person lagPersonGift() {
        return new Person(
            Collections.emptySet(),
            Set.of(lagNorskBostedsadresse()),
            Collections.emptySet(),
            Set.of(lagFødselsdatoForVoksen()),
            Set.of(new Folkeregisteridentifikator(IDENT_PERSON_GIFT, lagAktivMetadata())),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Set.of(new Kjoenn(KjoennType.KVINNE, lagAktivMetadata())),
            Collections.emptySet(),
            Set.of(new Navn(PERSON_GIFT_FORNAVN, "", "GYNGEHEST", lagAktivMetadata())),
            Collections.emptySet(),
            Set.of(
                new Sivilstand(Sivilstandstype.GIFT,
                    IDENT_HOVEDPERSON,
                    LocalDate.of(2019, 8, 3),
                    null,
                    lagAktivMetadata(toLocalDateTime("2020-12-05T08:32:21")))
            ),
            Set.of(new Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null, null, lagAktivMetadata(LocalDateTime.MAX))),
            Collections.emptySet());
    }

    public static Person lagPersonGiftHistorisk() {
        return new Person(
            Collections.emptySet(),
            Set.of(lagNorskBostedsadresse()),
            Collections.emptySet(),
            Set.of(lagFødselsdatoForVoksen()),
            Set.of(new Folkeregisteridentifikator(IDENT_PERSON_GIFT_HISTORISK, lagAktivMetadata())),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Set.of(new Kjoenn(KjoennType.KVINNE, lagAktivMetadata())),
            Collections.emptyList(),
            Set.of(new Navn("ROTAT", "", "KAFFE", lagAktivMetadata())),
            Collections.emptyList(),
            Set.of(
                new Sivilstand(Sivilstandstype.GIFT,
                    IDENT_HOVEDPERSON,
                    LocalDate.of(2019, 8, 3),
                    null,
                    lagAktivMetadata(toLocalDateTime("2022-04-27T14:57:56"))),
                new Sivilstand(Sivilstandstype.SEPARERT,
                    IDENT_HOVEDPERSON,
                    LocalDate.of(2013, 1, 3),
                    null,
                    lagHistoriskMetadata(toLocalDateTime("2022-04-28T13:28:43"))),
                new Sivilstand(Sivilstandstype.SKILT,
                    NULL_IDENT_NÅR_SKILT,
                    LocalDate.of(2014, 4, 9),
                    null,
                    lagHistoriskMetadata(toLocalDateTime("2022-05-04T12:20:45")))
            ),
            Set.of(new Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null, null, lagAktivMetadata(LocalDateTime.MAX))),
            Collections.emptyList());
    }


    public static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setFagsak(lagFagsak());
        return behandling;
    }

    public static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(lagBruker());
        return fagsak;
    }

    public static Aktoer lagBruker() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(BRUKER);
        aktoer.setAktørId(IDENT_HOVEDPERSON);
        return aktoer;
    }

    public static Bostedsadresse lagNorskBostedsadresse() {
        return new Bostedsadresse(
            LocalDateTime.of(2015, 1, 1, 0, 0, 0),
            LocalDateTime.of(2999, 5, 5, 0, 0, 0),
            "Kongen",
            new Vegadresse(
                "Slottsplassen 1",
                "1",
                null,
                "",
                "0010"
            ),
            null,
            null,
            null,
            lagAktivMetadata(LocalDateTime.of(2022, 6, 26, 13, 37, 0)));
    }

    public static Metadata lagAktivMetadata() {
        return lagAktivMetadata(LocalDateTime.of(1950, 1, 11, 12, 0, 0));
    }

    public static Metadata lagAktivMetadata(LocalDateTime registrertDato) {
        return new Metadata("PDL", false,
            List.of(new Endring(OPPRETT, registrertDato, "Z123456")));
    }

    public static Metadata lagHistoriskMetadata(LocalDateTime registrertDato) {
        return new Metadata("PDL", true,
            List.of(new Endring(OPPRETT, registrertDato, "Z123456")));
    }

    private static LocalDateTime toLocalDateTime(String localDateTime) {
        return LocalDateTime.parse(localDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @NotNull
    private static Set<ForelderBarnRelasjon> lagForelderBarnRelasjoner() {
        return Set.of(new ForelderBarnRelasjon(IDENT_BARN, Familierelasjonsrolle.BARN, Familierelasjonsrolle.MOR, lagAktivMetadata()),
            new ForelderBarnRelasjon("forelderIdent", Familierelasjonsrolle.MOR, Familierelasjonsrolle.BARN, lagAktivMetadata()));
    }

    @NotNull
    private static Foedsel lagFødselsdatoForVoksen() {
        return new Foedsel(LocalDate.of(1950, 1, 1), 1950, "NOR", "Oslo", lagAktivMetadata());
    }

}
