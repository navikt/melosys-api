package no.nav.melosys.service.persondata.mapping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import no.nav.melosys.integrasjon.pdl.dto.Endring;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;
import no.nav.melosys.integrasjon.pdl.dto.person.Navn;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.integrasjon.pdl.dto.Endringstype.KORRIGER;
import static no.nav.melosys.integrasjon.pdl.dto.Endringstype.OPPRETT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class NavnOversetterTest {

    @Test
    void oversettTilModel_flereNavn_nyesteIkkeHistoriskNavnHentes() {
        final Set<Navn> navn = Set.of(new Navn("fornavn", "histo", "risk", historiskMetadata()),
            new Navn("fornavn", "mellomnavn", "nyetternavn", metadata()));

        final no.nav.melosys.domain.person.Navn resultat = NavnOversetter.oversett(navn);

        assertThat(resultat).isEqualTo(new no.nav.melosys.domain.person.Navn("fornavn", "mellomnavn", "nyetternavn"));
    }

    private Metadata metadata() {
        return new Metadata("PDL", false,
            List.of(new Endring(OPPRETT, LocalDateTime.parse("2022-03-16T10:04:52"), "Dolly")));
    }

    private Metadata historiskMetadata() {
        return new Metadata("PDL", true,
            List.of(new Endring(OPPRETT, LocalDateTime.parse("2021-05-07T10:04:52"), "Dolly"),
                new Endring(KORRIGER, LocalDateTime.parse("2022-03-16T12:04:52"), "Dolly")));
    }
}
