package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "ARBEIDSFORHOLD_TYPE")
public class ArbeidsforholdsType extends Kodeverk {

    public static final ArbeidsforholdsType ORDINAERT = new ArbeidsforholdsType("ordinaertArbeidsforhold");
    public static final ArbeidsforholdsType MARITIMT = new ArbeidsforholdsType("maritimtArbeidsforhold");
    public static final ArbeidsforholdsType FORENKLET_OPPGJØR = new ArbeidsforholdsType("forenkletOppgjoersordning");
    public static final ArbeidsforholdsType FRILANSER_MM = new ArbeidsforholdsType("frilanserOppdragstakerHonorarPersonerMm");
    public static final ArbeidsforholdsType UTEN = new ArbeidsforholdsType("pensjonOgAndreTyperYtelserUtenAnsettelsesforhold ");

    ArbeidsforholdsType() {
    }

    private ArbeidsforholdsType(String kode) {
        super(kode);
    }

    /**
     * Returnerer en ArbeidsforholdsType ut fra en kode.
     *
     * @param kode
     * @return
     * @throws IllegalArgumentException hvis koden svarer ikke til en eksisterende ArbeidsforholdsType
     */
    public static final ArbeidsforholdsType getFraKode(String kode) {
        if (ORDINAERT.getKode().equalsIgnoreCase(kode)) {
            return ORDINAERT;
        }

        if (MARITIMT.getKode().equalsIgnoreCase(kode)) {
            return MARITIMT;
        }

        if (FORENKLET_OPPGJØR.getKode().equalsIgnoreCase(kode)) {
            return FORENKLET_OPPGJØR;
        }

        if (FRILANSER_MM.getKode().equalsIgnoreCase(kode)) {
            return FRILANSER_MM;
        }

        if (UTEN.getKode().equalsIgnoreCase(kode)) {
            return UTEN;
        }

        throw new IllegalArgumentException("ArbeidsforholdsType :" + kode + " finnes ikke.");

    }

}
