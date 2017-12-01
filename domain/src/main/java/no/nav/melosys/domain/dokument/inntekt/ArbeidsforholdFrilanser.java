package no.nav.melosys.domain.dokument.inntekt;

import no.nav.melosys.domain.dokument.felles.Periode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
/*@XmlType(name = "ArbeidsforholdFrilanser", propOrder = {
        "frilansPeriode",
        "yrke"
})*/
public class ArbeidsforholdFrilanser {

    public Periode frilansPeriode;

    public String yrke; //"http://nav.no/kodeverk/Kodeverk/Yrker"

    public Periode getFrilansPeriode() {
        return frilansPeriode;
    }

    public void setFrilansPeriode(Periode frilansPeriode) {
        this.frilansPeriode = frilansPeriode;
    }

    public String getYrke() {
        return yrke;
    }

    public void setYrke(String yrke) {
        this.yrke = yrke;
    }
}
