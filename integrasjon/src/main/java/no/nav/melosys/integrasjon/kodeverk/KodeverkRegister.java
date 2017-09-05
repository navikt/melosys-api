package no.nav.melosys.integrasjon.kodeverk;

public interface KodeverkRegister {

    public Kodeverk hentKodeverk(String kodeverkNavn) throws UkjentKodeverkException;
    
}
