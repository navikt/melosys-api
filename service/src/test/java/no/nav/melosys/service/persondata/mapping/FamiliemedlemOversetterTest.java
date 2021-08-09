package no.nav.melosys.service.persondata.mapping;

import java.time.LocalDate;

import no.nav.melosys.domain.person.Foedsel;
import no.nav.melosys.domain.person.Folkeregisteridentifikator;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.domain.person.familie.Familierelasjon;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.person.familie.Familierelasjon.MOR;
import static no.nav.melosys.domain.person.familie.Familierelasjon.RELATERT_VED_SIVILSTAND;
import static no.nav.melosys.service.persondata.PdlObjectFactory.lagPerson;
import static org.assertj.core.api.Assertions.assertThat;

class FamiliemedlemOversetterTest {
    @Test
    void oversettBarn() {
        final var familiemedlem = FamiliemedlemOversetter.oversettBarn(lagPerson());
        assertThat(familiemedlem.folkeregisteridentifikator()).isEqualTo(new Folkeregisteridentifikator("IdNr"));
        assertThat(familiemedlem.fødsel()).isEqualTo(new Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested"));
        assertThat(familiemedlem.familierelasjon()).isEqualTo(Familierelasjon.BARN);
        assertThat(familiemedlem.navn()).isEqualTo(new Navn("fornavn", "mellomnavn", "etternavn"));
    }

    @Test
    void oversettForelder() {
        final var familiemedlem = FamiliemedlemOversetter.oversettForelder(lagPerson(),
            no.nav.melosys.integrasjon.pdl.dto.person.Familierelasjonsrolle.MOR);
        assertThat(familiemedlem.folkeregisteridentifikator()).isEqualTo(new Folkeregisteridentifikator("IdNr"));
        assertThat(familiemedlem.familierelasjon()).isEqualTo(MOR);
        assertThat(familiemedlem.navn()).isEqualTo(new Navn("fornavn", "mellomnavn", "etternavn"));
    }

    @Test
    void oversettRelatertVedSivilstand() {
        final var familiemedlem = FamiliemedlemOversetter.oversettRelatertVedSivilstand(lagPerson());
        assertThat(familiemedlem.folkeregisteridentifikator()).isEqualTo(new Folkeregisteridentifikator("IdNr"));
        assertThat(familiemedlem.fødsel()).isEqualTo(new Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested"));
        assertThat(familiemedlem.familierelasjon()).isEqualTo(RELATERT_VED_SIVILSTAND);
        assertThat(familiemedlem.navn()).isEqualTo(new Navn("fornavn", "mellomnavn", "etternavn"));
    }
}
