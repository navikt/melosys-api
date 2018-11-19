package no.nav.melosys.service.dokument.brev;

import java.util.HashMap;
import java.util.Map;

import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.DokumentType;
import no.nav.melosys.service.dokument.brev.mapper.A1Mapper;
import no.nav.melosys.service.dokument.brev.mapper.BrevDataMapper;
import no.nav.melosys.service.dokument.brev.mapper.ForvaltningsmeldingMapper;
import no.nav.melosys.service.dokument.brev.mapper.MangelbrevMapper;

public class BrevDataMapperRuter {

    private static Map<DokumentType, Class<? extends BrevDataMapper>> mappere = new HashMap<>();

    static {
        mappere.put(DokumentType.MELDING_FORVENTET_SAKSBEHANDLINGSTID, ForvaltningsmeldingMapper.class);
        mappere.put(DokumentType.MELDING_MANGLENDE_OPPLYSNINGER, MangelbrevMapper.class);
        mappere.put(DokumentType.A1, A1Mapper.class);
    }

    private BrevDataMapperRuter() {
    }

    public static BrevDataMapper brevDataMapper(DokumentType type) throws TekniskException {
        if (!mappere.containsKey(type)) {
            throw new TekniskException("DokumentType med kode " + type.getKode() + " støttes ikke");
        }
        try {
            return mappere.get(type).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new TekniskException(e);
        }
    }
}
