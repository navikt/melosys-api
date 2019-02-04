package no.nav.melosys.service.dokument.sed;

import java.util.HashMap;
import java.util.Map;

import no.nav.melosys.eux.model.SedType;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.sed.mapper.A009Mapper;
import no.nav.melosys.service.dokument.sed.mapper.SedMapper;

public class SedDataMapperRuter {

    static Map<SedType, Class<? extends SedMapper>> sedMappere = new HashMap<>();

    static {
        sedMappere.put(SedType.A009, A009Mapper.class);
    }

    private SedDataMapperRuter() {
    }

    public static SedMapper sedMapper(SedType sedType) throws TekniskException {
        if (!sedMappere.containsKey(sedType)) {
            throw new TekniskException("Sed-type " + sedType.name() + " støttes ikke");
        }
        try {
            return sedMappere.get(sedType).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new TekniskException(e);
        }
    }
}
