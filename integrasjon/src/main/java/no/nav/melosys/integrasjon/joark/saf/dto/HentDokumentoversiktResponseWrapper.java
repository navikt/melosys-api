package no.nav.melosys.integrasjon.joark.saf.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HentDokumentoversiktResponseWrapper(@JsonProperty("query") HentDokumentoversiktResponse hentDokumentoversiktResponse) {
}
