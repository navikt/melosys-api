package no.nav.melosys.domain.inngangsvilkar;

import java.util.ArrayList;
import java.util.Collection;

public class InngangsvilkarResponse {
    private Boolean kvalifisererForEf883_2004;

    private Collection<Feilmelding> feilmeldinger = new ArrayList<>();

    public Boolean getKvalifisererForEf883_2004() {
        return kvalifisererForEf883_2004;
    }

    public void setKvalifisererForEf883_2004(Boolean kvalifisererForEf883_2004) {
        this.kvalifisererForEf883_2004 = kvalifisererForEf883_2004;
    }

    public Collection<Feilmelding> getFeilmeldinger() {
        return feilmeldinger;
    }

    public void setFeilmeldinger(Collection<Feilmelding> feilmeldinger) {
        this.feilmeldinger = feilmeldinger;
    }

}
