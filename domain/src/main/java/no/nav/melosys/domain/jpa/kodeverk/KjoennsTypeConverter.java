package no.nav.melosys.domain.jpa.kodeverk;

import no.nav.melosys.domain.dokument.person.KjoennsType;

public class KjoennsTypeConverter extends AbstraktKodeverkHjelperConverter<KjoennsType> {

    @Override
    public KjoennsType convertToEntityAttribute(String s) {
        if (s == null) {
            return null;
        }
        KjoennsType kjoennsType = new KjoennsType();
        kjoennsType.setKode(s);
        return kjoennsType;
    }
}
