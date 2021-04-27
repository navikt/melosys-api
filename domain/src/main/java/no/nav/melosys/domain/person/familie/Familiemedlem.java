package no.nav.melosys.domain.person.familie;

import no.nav.melosys.domain.person.ident.Folkeregisteridentifikator;

// NB borMedBruker og fnrAnnenForelder leveres ikke av PDL og må beregnes
// TODO Hvilken sivilstand skal vi bruke for familierelasjoner?
public record Familiemedlem(Folkeregisteridentifikator fnr, // NB Kun fnr støttes i PDL
                            FamilierelasjonRolle relasjonsrolle) {
}
