package no.nav.melosys.integrasjon.dokumentmottak;

import java.time.LocalDateTime;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.api.Binge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// FIXME: Dette er en egen komponent for å teste saksflyt uten å lytte på kø. Kan integreres i DokumentmottakConsumerImpl.
@Component
public class ProsessinstansMeldingsfordeler {

    private static final Logger log = LoggerFactory.getLogger(ProsessinstansMeldingsfordeler.class);

    private final Binge binge;

    public ProsessinstansMeldingsfordeler(Binge binge) {
        this.binge = binge;
    }

    public void execute(ForsendelsesinformasjonDto forsendelsesinfo) {
        Prosessinstans prosessinstans = new Prosessinstans();

        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        prosessinstans.setSteg(ProsessSteg.VURDER_AUTOMATISK_JFR);
        prosessinstans.setRegistrertDato(LocalDateTime.now());

        prosessinstans.setData(ProsessDataKey.ARKIVSYSTEM, forsendelsesinfo.arkivsystem);
        prosessinstans.setData(ProsessDataKey.ARKIV_ID, forsendelsesinfo.arkivId);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, forsendelsesinfo.behandlingstema);
        prosessinstans.setData(ProsessDataKey.TEMA, forsendelsesinfo.tema);

        if (binge.leggTil(prosessinstans)) {
            log.info("Opprettet prosessinstans av forsendelsesinfo fra DokMot", prosessinstans);
        }
    }

}
