package no.nav.melosys.integrasjon.pdl.dto.person;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

import static no.nav.melosys.integrasjon.pdl.dto.person.AdressebeskyttelseGradering.*;

public record Adressebeskyttelse(AdressebeskyttelseGradering gradering, Metadata metadata) implements HarMetadata {
    public boolean erStrengtFortrolig() {
        return gradering() == STRENGT_FORTROLIG || gradering() == STRENGT_FORTROLIG_UTLAND;
    }
}
