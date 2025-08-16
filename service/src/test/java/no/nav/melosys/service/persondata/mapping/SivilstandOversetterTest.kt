package no.nav.melosys.service.persondata.mapping;

import java.time.LocalDate;

import no.nav.melosys.domain.person.Sivilstand;
import no.nav.melosys.domain.person.Sivilstandstype;
import no.nav.melosys.service.persondata.PdlObjectFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SivilstandOversetterTest {

    @Test
    void oversettForRelatertVedSivilstand() {
        final Sivilstand sivilstand = SivilstandOversetter.oversettForRelatertVedSivilstand(PdlObjectFactory.lagPerson().sivilstand());
        assertThat(sivilstand).isEqualTo(new Sivilstand(Sivilstandstype.GIFT, null, "relatertVedSivilstandID",
            LocalDate.MIN, LocalDate.EPOCH, "PDL", "Dolly", false));
    }
}
