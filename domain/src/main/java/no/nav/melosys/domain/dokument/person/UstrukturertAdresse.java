package no.nav.melosys.domain.dokument.person;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import no.nav.melosys.domain.dokument.felles.Land;
import org.apache.commons.lang3.StringUtils;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UstrukturertAdresse")
public class UstrukturertAdresse {

    public String adresselinje1;

    public String adresselinje2;

    public String adresselinje3;

    public String adresselinje4;

    public String postnr;

    public String poststed;

    public Land land;

    public boolean erTom() {
        return StringUtils.isEmpty(adresselinje1) &&
            StringUtils.isEmpty(adresselinje2) &&
            StringUtils.isEmpty(adresselinje3) &&
            StringUtils.isEmpty(adresselinje4) &&
            StringUtils.isEmpty(postnr) &&
            StringUtils.isEmpty(poststed) &&
            land == null;
    }
}
