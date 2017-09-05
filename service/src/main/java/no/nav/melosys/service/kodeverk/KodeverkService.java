package no.nav.melosys.service.kodeverk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;

/**
 * Klassen tilbyr tjenester for å slå opp kodeverk.
 * Merk: Klassen casher oppslag mot felles-kodevek, og er derfor egentlig ikke stateless (men den er trådsikker).
 */
@Service
public class KodeverkService {
    
    private Map<String, Kodeverk> kodeverkCache = new HashMap<>(); // Ikke aksesser denne usynkronisert med mindre du vet hva du gjør
    
    @Autowired
    private KodeverkRegister kodeverkRegister;
    
    /**
     * Henter alle gyldige verdier for et kodeverk på et gitt tidspunkt.
     */
    public List<String> gyldigeVerdier(KodeverkNavn kodeverkNavn, LocalDate dato) {
        Map<String, List<Kode>> koder = hentKodeverk(kodeverkNavn.getNavn()).getKoder();
        List<String> res = new ArrayList<>(koder.size());
        for (List<Kode> kodeperioder : koder.values()) {
            // Kodeperioder er en liste med samme kode men med forskjellige gyldighetsperiode. Det holder at en er gyldig.
            for (Kode kandidat : kodeperioder) {
                if (!kandidat.getGyldigFom().isAfter(dato) && !kandidat.getGyldigTom().isBefore(dato)) {
                    res.add(kandidat.getKode());
                    break;
                }
            }
        }
        return res;
    }
    
    /**
     * Henter verdien for en kode i et kodeverk på en gitt dato, eller null hvis koden ikke er omfattet av kodeverket på angitt dato.
     */
    public String dekod(KodeverkNavn kodeverkNavn, String kode, LocalDate dato) {
        List<Kode> kodeperioder = hentKodeverk(kodeverkNavn.getNavn()).getKoder().get(kode);
        if (kodeperioder == null) {
            return null;
        }
        // kodeperioder er en liste med samme kode men med forskjellige gyldighetsperiode. Det holder at en er gyldig.
        for (Kode kandidat : kodeperioder) {
            if (!kandidat.getGyldigFom().isAfter(dato) && !kandidat.getGyldigTom().isBefore(dato)) {
                return kandidat.getNavn();
            }
        }
        return null;
    }
    
    /*
     * Merk: Vi har en forsvinnende liten mulighet for feil hvis dette ikke synkroniseres. 
     * Problemer kan oppstå hvis metoden kalles i parallell der det ene kallet medfører put og resize/rehash.
     * Synchronized kan fjernes hvis kodeverkCache gjøres om til ThreadLocal.
     * NB! Ikke gjør "smarte" ting her (som f.eks. delvis synkronisering) med mindre du vet om alle konsekvensene.
     */
    private synchronized Kodeverk hentKodeverk(String kodeverkNavn) {
        Kodeverk kodeverk = kodeverkCache.get(kodeverkNavn);
        if (kodeverk != null)  {
            return kodeverk;
        }
        kodeverk = kodeverkRegister.hentKodeverk(kodeverkNavn);
        kodeverkCache.put(kodeverkNavn, kodeverk);
        return kodeverk;
    }

}
