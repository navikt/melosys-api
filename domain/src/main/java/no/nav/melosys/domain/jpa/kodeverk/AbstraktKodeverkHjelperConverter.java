package no.nav.melosys.domain.jpa.kodeverk;

import javax.persistence.AttributeConverter;

import no.nav.melosys.domain.dokument.felles.AbstraktKodeverkHjelper;

public abstract class AbstraktKodeverkHjelperConverter<T extends AbstraktKodeverkHjelper> implements AttributeConverter<T, String> {

    @Override
    public String convertToDatabaseColumn(T kodeverkHjelper) {
        return kodeverkHjelper != null ? kodeverkHjelper.getKode() : null;
    }

    @Override
    public abstract T convertToEntityAttribute(String s);
}
