package no.nav.melosys.integrasjon.kodeverk.rest;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import no.nav.melosys.integrasjon.kodeverk.UkjentKodeverkException;
import no.nav.melosys.integrasjon.kodeverk.rest.dto.KodeDto;
import org.springframework.beans.factory.annotation.Autowired;

public class KodeverkRegisterImpl implements KodeverkRegister {

    private final KodeverkConsumerImpl kodeverkConsumer;

    static final String BOKMÅL = "nb";

    @Autowired
    KodeverkRegisterImpl(KodeverkConsumerImpl kodeverkConsumerImpl) {
        this.kodeverkConsumer = kodeverkConsumerImpl;
    }

    @Override
    public Kodeverk hentKodeverk(String kodeverkNavn) throws UkjentKodeverkException {
        KodeDto kodeDto = kodeverkConsumer.hentKodeverk(kodeverkNavn);
        Map<String, List<Kode>> koder = new HashMap<>();
        kodeDto.betydninger.forEach((kode, betydninger) -> {
            List<Kode> termer = betydninger.stream().map(betydning -> {
                String term = betydning.beskrivelser.get(BOKMÅL).term;
                return new Kode(kode, term, betydning.gyldigFra, betydning.gyldigTil);
            }).collect(Collectors.toList());
            koder.put(kode, termer);
        });
        return new Kodeverk(kodeverkNavn, koder);
    }
}
