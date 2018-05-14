package no.nav.melosys.integrasjon.joark.dokumentmottak;

import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.Forsendelsesinformasjon;

public interface Meldingsfordeler {

    void execute(Forsendelsesinformasjon forsendelsesinfo);
}
