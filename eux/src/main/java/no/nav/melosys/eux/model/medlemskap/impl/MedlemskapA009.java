package no.nav.melosys.eux.model.medlemskap.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.nav.melosys.eux.model.medlemskap.Medlemskap;
import no.nav.melosys.eux.model.nav.Utsendingsland;
import no.nav.melosys.eux.model.nav.Vedtak;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MedlemskapA009 extends Medlemskap {

    private Utsendingsland utsendingsland;

    private Vedtak vedtak;

    public Utsendingsland getUtsendingsland() {
        return utsendingsland;
    }

    public void setUtsendingsland(Utsendingsland utsendingsland) {
        this.utsendingsland = utsendingsland;
    }

    public Vedtak getVedtak() {
        return vedtak;
    }

    public void setVedtak(Vedtak vedtak) {
        this.vedtak = vedtak;
    }
}
