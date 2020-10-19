package no.nav.melosys.domain.jpa.kodeverk;

import no.nav.melosys.domain.dokument.person.Sivilstand;

public class SivilstandConverter extends AbstraktKodeverkHjelperConverter<Sivilstand> {

    @Override
    public Sivilstand convertToEntityAttribute(String s) {
        if (s == null) {
            return null;
        }
        Sivilstand sivilstand = new Sivilstand();
        sivilstand.setKode(s);
        return sivilstand;
    }
}
