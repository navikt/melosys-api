package no.nav.melosys.integrasjon.joark.dokumentmottak;

import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.Forsendelsesinformasjon;
import no.nav.melosys.domain.Prosessinstans;
import org.springframework.stereotype.Component;

@Component
public class ProsessinstansMeldingsfordeler implements Meldingsfordeler {

    @Override
    public void execute(Forsendelsesinformasjon forsendelsesinfo) {
        Prosessinstans prosessinstans = new Prosessinstans();
        // FIXME Legg på nødvendig informasjon
        //binge.leggTil(prosessinstans);
    }
}
