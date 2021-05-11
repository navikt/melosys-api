package no.nav.melosys.service.vedtak.dto;

import no.nav.melosys.domain.eessi.sed.Adressetype;

public record Adresse(Adressetype adressetype, String gavenavn, String gatenummer, String postnummer, String poststed) {
}
