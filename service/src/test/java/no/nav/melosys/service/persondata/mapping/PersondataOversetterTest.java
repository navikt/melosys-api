package no.nav.melosys.service.persondata.mapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.person.*;
import no.nav.melosys.domain.person.adresse.Adressebeskyttelse;
import no.nav.melosys.domain.person.adresse.AdressebeskyttelseGradering;
import no.nav.melosys.integrasjon.pdl.dto.person.Kjoenn;
import no.nav.melosys.integrasjon.pdl.dto.person.KjoennType;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.persondata.PdlObjectFactory.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PersondataOversetterTest {
    @Mock
    KodeverkService kodeverkService;

    @Test
    void oversett() {
        final Personopplysninger personopplysninger = PersondataOversetter.oversett(lagPerson(), kodeverkService);

        assertThat(personopplysninger.adressebeskyttelser())
            .containsExactly(new Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, "PDL"));
        assertThat(personopplysninger.bostedsadresse().strukturertAdresse().getGatenavn()).isEqualTo("gata");
        assertThat(personopplysninger.dødsfall().dødsdato()).isEqualTo(LocalDate.MAX);
        assertThat(personopplysninger.fødsel()).isEqualTo(new Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested"));
        assertThat(personopplysninger.folkeregisteridentifikator()).isEqualTo(new Folkeregisteridentifikator("IdNr"));
        assertThat(personopplysninger.kjønn()).isEqualTo(no.nav.melosys.domain.person.KjoennType.MANN);
        assertThat(personopplysninger.navn()).isEqualTo(new Navn("fornavn", "mellomnavn", "etternavn"));
        assertThat(personopplysninger.statsborgerskap()).containsExactlyInAnyOrder(new Statsborgerskap("AIA", null,
            LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"), "PDL", "Dolly", false),
            new Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null, null, "PDL",
                "Dolly", false));
    }

    private Person lagPerson() {
        return new Person(
            Set.of(lagAdressebeskyttelse(no.nav.melosys.integrasjon.pdl.dto.person.AdressebeskyttelseGradering.FORTROLIG)),
            Set.of(lagUtenlandskBostedsadresse("adresse utland", LocalDateTime.MIN), lagNorskBostedsadresse("gata",
                LocalDateTime.MAX)),
            Set.of(new no.nav.melosys.integrasjon.pdl.dto.person.Doedsfall(LocalDate.MAX, metadata())),
            Set.of(new no.nav.melosys.integrasjon.pdl.dto.person.Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested",
                metadata())),
            Set.of(new no.nav.melosys.integrasjon.pdl.dto.person.Folkeregisteridentifikator("IdNr", metadata())),
            null,
            null,
            Set.of(new Kjoenn(KjoennType.UKJENT, lagMetadata(LocalDateTime.MIN)), new Kjoenn(KjoennType.MANN, lagMetadata(LocalDateTime.MAX))),
            Collections.emptyList(),
            Set.of(new no.nav.melosys.integrasjon.pdl.dto.person.Navn("fornavn", "mellomnavn", "etternavn", metadata())),
            Collections.emptyList(),
            null,
            Set.of(new no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap("AIA", null,
                LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"), lagMetadata(LocalDateTime.MIN)),
                new no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null,
                null, lagMetadata(LocalDateTime.MAX))),
            null
        );
    }
}
