package no.nav.melosys.service.persondata.mapping;

import no.nav.melosys.domain.person.Foedsel;
import no.nav.melosys.domain.person.Folkeregisteridentifikator;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.domain.person.familie.Familierelasjon;
import no.nav.melosys.integrasjon.pdl.dto.person.Familierelasjonsrolle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.melosys.domain.person.familie.Familierelasjon.RELATERT_VED_SIVILSTAND;
import static no.nav.melosys.service.persondata.PdlObjectFactory.lagPerson;
import static no.nav.melosys.service.persondata.PdlObjectFactory.lagSivilstand;
import static org.assertj.core.api.Assertions.assertThat;

class FamiliemedlemOversetterTest {
    @Test
    void oversettBarn() {
        Familiemedlem familiemedlem = FamiliemedlemOversetter.oversettBarn(lagPerson(),
                new Folkeregisteridentifikator("identForelder1"));


        assertThat(familiemedlem.folkeregisteridentifikator()).isEqualTo(new Folkeregisteridentifikator("IdNr"));
        assertThat(familiemedlem.fødsel()).isEqualTo(new Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested"));
        assertThat(familiemedlem.familierelasjon()).isEqualTo(Familierelasjon.BARN);
        assertThat(familiemedlem.navn()).isEqualTo(new Navn("fornavn", "mellomnavn", "etternavn"));
        assertThat(familiemedlem.foreldreansvarstype()).isEqualTo("felles");
        assertThat(familiemedlem.folkeregisteridentAnnenForelder()).isEqualTo(new Folkeregisteridentifikator("forelderIdent"));
    }

    @Test
    void oversettForelder() {
        Familiemedlem familiemedlem = FamiliemedlemOversetter.oversettForelder(lagPerson(), Familierelasjonsrolle.MOR);


        assertThat(familiemedlem.folkeregisteridentifikator()).isEqualTo(new Folkeregisteridentifikator("IdNr"));
        assertThat(familiemedlem.familierelasjon()).isEqualTo(Familierelasjon.MOR);
        assertThat(familiemedlem.navn()).isEqualTo(new Navn("fornavn", "mellomnavn", "etternavn"));
    }

    @Test
    void oversettPersonRelatertVedSivilstandMedSivilstand() {
        String forventetSivilstandID = "forventetSivilstandID";


        Familiemedlem familiemedlem = FamiliemedlemOversetter.oversettEktefelleEllerPartner(lagPerson(), lagSivilstand(forventetSivilstandID));


        assertThat(familiemedlem.folkeregisteridentifikator()).isEqualTo(new Folkeregisteridentifikator("IdNr"));
        assertThat(familiemedlem.fødsel()).isEqualTo(new Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested"));
        assertThat(familiemedlem.familierelasjon()).isEqualTo(RELATERT_VED_SIVILSTAND);
        assertThat(familiemedlem.navn()).isEqualTo(new Navn("fornavn", "mellomnavn", "etternavn"));
        assertThat(familiemedlem.sivilstand()).isNotNull();
        assertThat(familiemedlem.sivilstand().relatertVedSivilstand()).isEqualTo(forventetSivilstandID);
    }
}
