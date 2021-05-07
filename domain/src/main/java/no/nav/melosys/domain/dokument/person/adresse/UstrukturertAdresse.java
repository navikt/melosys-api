package no.nav.melosys.domain.dokument.person.adresse;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.dokument.felles.Land;
import org.apache.commons.lang3.StringUtils;

import static java.util.Arrays.asList;

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

    @JsonIgnore
    public boolean erTom() {
        return StringUtils.isAllEmpty(
            adresselinje1,
            adresselinje2,
            adresselinje3,
            adresselinje4,
            postnr,
            poststed) &&
            land == null;
    }

    @JsonIgnore
    public List<String> adresselinjer() {
        return asList(
            adresselinje1,
            adresselinje2,
            adresselinje3,
            adresselinje4
        );
    }
}
