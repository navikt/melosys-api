package no.nav.melosys.domain.dokument.adresse;

import java.util.Objects;

import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlType(name = "adresse", namespace = "dokument") // Løser navnekonflikt med no.nav.melosys.domain.eessi.melding.Adresse
public abstract class Adresse {
    public String landkode;

    @JsonIgnore
    public abstract boolean erTom();

    public static String sammenslå(String s1, String s2) {
        return (Objects.toString(s1, "") + " " + Objects.toString(s2, "")).trim();
    }
}
