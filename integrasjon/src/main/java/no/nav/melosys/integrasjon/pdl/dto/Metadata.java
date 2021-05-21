package no.nav.melosys.integrasjon.pdl.dto;

import java.util.List;

public record Metadata(String master, boolean historisk, List<Endring> endringer) {
}
