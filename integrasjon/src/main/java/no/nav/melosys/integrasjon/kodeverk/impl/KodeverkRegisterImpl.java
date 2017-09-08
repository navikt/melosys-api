package no.nav.melosys.integrasjon.kodeverk.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import no.nav.melosys.integrasjon.kodeverk.UkjentKodeverkException;
import no.nav.tjeneste.virksomhet.kodeverk.v2.HentKodeverkHentKodeverkKodeverkIkkeFunnet;
import no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.EnkeltKodeverk;
import no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.Periode;
import no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.Term;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkRequest;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkResponse;

@Component
public class KodeverkRegisterImpl implements KodeverkRegister {

    @Autowired
    private KodeverkConsumerImpl kodeverkConsumer;
    
    @Override
    public Kodeverk hentKodeverk(String kodeverkNavn) throws UkjentKodeverkException {
        try {
            HentKodeverkRequest req = new HentKodeverkRequest();
            req.setNavn(kodeverkNavn);
            HentKodeverkResponse res = kodeverkConsumer.hentKodeverk(req);
            if (res.getKodeverk() instanceof EnkeltKodeverk) {
                EnkeltKodeverk ek = (EnkeltKodeverk) res.getKodeverk();
                Map<String, List<Kode>> koder = new HashMap<>();
                for (no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.Kode k : ek.getKode()) {
                    List<Kode> termer = new ArrayList<>();
                    for (Term t : k.getTerm()) {
                        for (Periode p : t.getGyldighetsperiode()) {
                            Kode kode = new Kode(k.getNavn(), t.getNavn(), toLocalData(p.getFom()), toLocalData(p.getFom()));
                            termer.add(kode);
                        }
                    }
                    koder.put(ek.getNavn(), termer);
                }
                Kodeverk kodeverk = new Kodeverk(res.getKodeverk().getNavn(), koder);
                return kodeverk;
            } else {
                throw new RuntimeException("Støtter ikke kodeverk av type " + res.getKodeverk().getClass().getName());
            }
        } catch (HentKodeverkHentKodeverkKodeverkIkkeFunnet e) {
            throw new UkjentKodeverkException("Finner ingen kodeverk med navn " + kodeverkNavn);
        }

    }

    private static LocalDate toLocalData(XMLGregorianCalendar cal) {
        return cal.toGregorianCalendar().toZonedDateTime().toLocalDate();
    }

}
