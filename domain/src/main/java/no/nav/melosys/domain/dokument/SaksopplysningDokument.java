package no.nav.melosys.domain.dokument;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.SaksopplysningType;

import javax.xml.bind.annotation.XmlTransient;

/**
 * Superklasse for alle dokumenter.
 */
public abstract class SaksopplysningDokument extends Dokument {

    @JsonIgnore
    @XmlTransient
    private SaksopplysningType type;

    public SaksopplysningType getType() {
        return type;
    }

    public void setType(SaksopplysningType type) {
        this.type = type;
    }
}
