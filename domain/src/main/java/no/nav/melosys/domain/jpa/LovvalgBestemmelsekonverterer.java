package no.nav.melosys.domain.jpa;

import javax.persistence.AttributeConverter;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.util.LovvalgBestemmelseUtils;

public final class LovvalgBestemmelsekonverterer implements AttributeConverter<LovvalgBestemmelse, String> {

    @Override
    public String convertToDatabaseColumn(LovvalgBestemmelse attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public LovvalgBestemmelse convertToEntityAttribute(String dbData) {
        return LovvalgBestemmelseUtils.dbDataTilLovvalgBestemmelse(dbData);
    }
}
