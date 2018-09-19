package no.nav.melosys.domain;

import javax.persistence.AttributeConverter;

/**
 * Felles interface for alle enums som korresponderer til kodeverktabell.
 */
public interface InterntKodeverkTabell<E extends Enum<?> & InterntKodeverkTabell<?>> extends Kodeverk {
    
    /**
     * Superklasse for AttributeConverter for kodeverk.
     * Det skal være nok å subklasse denne og implementere getLovligeVerdier til å returnere array over alle enums av typen (altså samme 
     * som E.values() når typen til E er kjent).
     */
    public abstract static class DbKonverterer<E extends Enum<?> & InterntKodeverkTabell<?>> implements AttributeConverter<E, String> {
        
        protected abstract E[] getLovligeVerdier();
        
        @Override
        public String convertToDatabaseColumn(E attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public E convertToEntityAttribute(String kode) {
            if (kode == null) {
                return null;
            }
            for (E kandidat : getLovligeVerdier()) {
                if (kode.equals(kandidat.getKode())) {
                    return kandidat;
                }
            }
            throw new IllegalArgumentException("Ukjent kode for Kodeverk: " + kode);
        }
    }

}
