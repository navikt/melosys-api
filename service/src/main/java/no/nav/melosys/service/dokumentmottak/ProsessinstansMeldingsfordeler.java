package no.nav.melosys.service.dokumentmottak;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Properties;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// FIXME: Dette er en egen komponent for å teste saksflyt uten å lytte på kø. Kan integreres i DokumentmottakConsumerImpl.
@Component
public class ProsessinstansMeldingsfordeler {

    private static final Logger log = LoggerFactory.getLogger(ProsessinstansMeldingsfordeler.class);

    // FIXME: Autowire Binge i saksflyt-api (MELOSYS-1214)

    public void execute(ForsendelsesinformasjonDto forsendelsesinfo) {
        log.info("info", forsendelsesinfo);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setRegistrertDato(LocalDateTime.now());
        prosessinstans.setSteg(ProsessSteg.A1_JOURF);

        Properties properties = new Properties();
        properties.setProperty("arkivsystem",  forsendelsesinfo.arkivsystem);
        properties.setProperty("arkivId", forsendelsesinfo.arkivId);
        properties.setProperty("behandlingstema", forsendelsesinfo.behandlingstema);
        properties.setProperty("tema", forsendelsesinfo.tema);

        StringWriter writer = new StringWriter(512);
        properties.forEach((k, v) -> {writer.append((String) k).append('=').append((String) v).append('\n');});

        prosessinstans.setData(writer.toString());

        // FIXME: Kall til Binge.leggTil(prosessinstans) (MELOSYS-1214)
    }

}
