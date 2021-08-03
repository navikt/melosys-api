package no.nav.melosys.service.persondata.mapping;

import java.time.LocalDate;

import no.nav.melosys.domain.person.*;
import no.nav.melosys.domain.person.adresse.Adressebeskyttelse;
import no.nav.melosys.domain.person.adresse.AdressebeskyttelseGradering;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.persondata.PdlObjectFactory.lagPerson;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PersonopplysningerOversetterTest {
    @Mock
    KodeverkService kodeverkService;

    @Test
    void oversett() {
        final Personopplysninger personopplysninger = PersonopplysningerOversetter.oversett(lagPerson(), kodeverkService);

        assertThat(personopplysninger.adressebeskyttelser())
            .containsExactly(new Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, "PDL"));
        assertThat(personopplysninger.bostedsadresse().strukturertAdresse().getGatenavn()).isEqualTo("gata");
        assertThat(personopplysninger.dødsfall().dødsdato()).isEqualTo(LocalDate.MAX);
        assertThat(personopplysninger.fødsel()).isEqualTo(new Foedsel(LocalDate.EPOCH, 1970, "NOR", "fødested"));
        assertThat(personopplysninger.folkeregisteridentifikator()).isEqualTo(new Folkeregisteridentifikator("IdNr"));
        assertThat(personopplysninger.kjønn()).isEqualTo(KjoennType.UKJENT);
        assertThat(personopplysninger.navn()).isEqualTo(new Navn("fornavn", "mellomnavn", "etternavn"));
        assertThat(personopplysninger.statsborgerskap()).containsExactlyInAnyOrder(new Statsborgerskap("AIA", null,
            LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"), "PDL", "Dolly", false),
            new Statsborgerskap("NOR", LocalDate.parse("2021-05-08"), null, null, "PDL",
                "Dolly", false));
    }
}
