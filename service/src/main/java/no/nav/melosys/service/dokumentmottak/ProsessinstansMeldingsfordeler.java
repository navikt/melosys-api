package no.nav.melosys.service.dokumentmottak;

import no.nav.melosys.domain.Prosessinstans;
import org.springframework.stereotype.Component;

@Component
public class ProsessinstansMeldingsfordeler implements Meldingsfordeler {

    @Override
    public void execute(ForsendelsesinformasjonDto forsendelsesinfo) {
        Prosessinstans prosessinstans = new Prosessinstans();
        String arkivsystem = forsendelsesinfo.arkivsystem;
        String arkivId = forsendelsesinfo.arkivId;
        // FIXME Legg på nødvendig informasjon
    }
}
