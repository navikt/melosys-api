package no.nav.melosys.integrasjon.kodeverk.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import no.nav.melosys.integrasjon.kodeverk.UkjentKodeverkException;
import no.nav.melosys.integrasjon.kodeverk.impl.dto.KodeDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class KodeverkRegisterImpl implements KodeverkRegister {

    private final KodeverkConsumerImpl kodeverkConsumer;

    static final String BOKMÅL = "nb";

    KodeverkRegisterImpl(KodeverkConsumerImpl kodeverkConsumerImpl) {
        this.kodeverkConsumer = kodeverkConsumerImpl;
    }

    @Override
    @Cacheable("kodeverk")
    public Kodeverk hentKodeverk(String kodeverkNavn) {
        try {
            KodeDto kodeDto = kodeverkConsumer.hentKodeverk(kodeverkNavn);
            Map<String, List<Kode>> koder = new HashMap<>();
            kodeDto.betydninger.forEach((kode, betydninger) -> {
                List<Kode> termer = betydninger.stream().map(betydning -> {
                    String term = betydning.beskrivelser.get(BOKMÅL).getTerm();
                    return new Kode(kode, term, betydning.gyldigFra, betydning.gyldigTil);
                }).toList();
                koder.put(kode, termer);
            });
            return new Kodeverk(kodeverkNavn, koder);
        } catch (NotFoundException e) {
            throw new UkjentKodeverkException("Finner ingen kodeverk med navn " + kodeverkNavn);
        }
    }
}
