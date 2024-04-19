package no.nav.melosys.integrasjon.dokumentmottak;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Dette er en intern representasjon av no.nav.melding.virksomhet.dokumentnotifikasjon.v1.Forsendelsesinformasjon
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "Forsendelsesinformasjon",
    propOrder = {"arkivId", "arkivsystem", "tema", "behandlingstema"}
)
public class ForsendelsesinformasjonDto {

    String arkivId;

    String arkivsystem;

    String tema; // http://nav.no/kodeverk/Kodeverk/Tema

    String behandlingstema; // http://nav.no/kodeverk/Kodeverk/Behandlingstema
}
