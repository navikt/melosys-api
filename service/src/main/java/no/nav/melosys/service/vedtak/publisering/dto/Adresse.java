package no.nav.melosys.service.vedtak.publisering.dto;

import no.nav.melosys.domain.eessi.sed.Adressetype;

public record Adresse(Adressetype adressetype, String gatenavn, String gatenummer, String postnummer, String poststed) {
}
