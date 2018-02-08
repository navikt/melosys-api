package no.nav.melosys.domain.dokument.inntekt;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class InntektDokument extends SaksopplysningDokument {

    @XmlElementWrapper(name="arbeidsInntektMaanedListe")
    @XmlElement(name="arbeidsInntektMaaned")
    public List<ArbeidsInntektMaaned> arbeidsInntektMaanedListe;

    public List<ArbeidsInntektMaaned> getArbeidsInntektMaanedListe() {
        return arbeidsInntektMaanedListe;
    }

}
