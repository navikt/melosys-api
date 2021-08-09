package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

public record UtpekDto(Set<String> mottakerinstitusjoner, String fritekstSed, String fritekstBrev) { }
