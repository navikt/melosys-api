package no.nav.melosys.integrasjon.pdl.dto;

import java.util.Collection;

public record Metadata(String opplysningsId, String master, Collection<Endring> endringer) {
}
