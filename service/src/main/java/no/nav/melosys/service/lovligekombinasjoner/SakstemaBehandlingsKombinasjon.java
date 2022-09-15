package no.nav.melosys.service.lovligekombinasjoner;

import java.util.Set;

import no.nav.melosys.domain.kodeverk.Sakstemaer;

public record SakstemaBehandlingsKombinasjon(
    Sakstemaer sakstema,
    Set<BehandlingstemaBehandlingstyperKombinasjon> behandlingstemaBehandlingstyperKombinasjoner) {
}

