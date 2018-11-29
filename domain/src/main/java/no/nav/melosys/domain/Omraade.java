package no.nav.melosys.domain;

import javax.persistence.AttributeConverter;

public interface Omraade extends Kodeverk {

    // Brukes av Jersey til å mappe Omraade som path-parameter
    static Omraade valueOf(String kode) {
        if (kode == null) {
            return null;
        }
        for (Territorier kandidat : Territorier.values()) {
            if (kode.equals(kandidat.getKode())) {
                return kandidat;
            }
        }
        for (Landkoder kandidat : Landkoder.values()) {
            if (kode.equals(kandidat.getKode())) {
                return kandidat;
            }
        }
        throw new IllegalArgumentException("Ukjent kode for Kodeverk: " + kode);
    }

    class DbKonverterer implements AttributeConverter<Omraade, String> {
        @Override
        public String convertToDatabaseColumn(Omraade område) {
            return område == null ? null : område.getKode();
        }

        @Override
        public Omraade convertToEntityAttribute(String kode) {
            return valueOf(kode);
        }
    }
}
