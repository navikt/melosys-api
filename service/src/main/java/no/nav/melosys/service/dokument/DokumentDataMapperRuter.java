package no.nav.melosys.service.dokument;

import java.util.HashMap;
import java.util.Map;

import no.nav.melosys.domain.ProduserbartDokument;
import no.nav.melosys.eux.model.SedType;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.mapper.*;
import no.nav.melosys.service.dokument.sed.mapper.A009Mapper;
import no.nav.melosys.service.dokument.sed.mapper.AbstraktSedMapper;

public class DokumentDataMapperRuter {

    static Map<ProduserbartDokument, Class<? extends BrevDataMapper>> mappere = new HashMap<>();
    static Map<SedType, Class<? extends AbstraktSedMapper>> sedMappere = new HashMap<>();

    static {
        mappere.put(ProduserbartDokument.ATTEST_A1, AttestMapper.class);
        mappere.put(ProduserbartDokument.AVSLAG_YRKESAKTIV, AvslagMapper.class);
        mappere.put(ProduserbartDokument.INNVILGELSE_YRKESAKTIV, InnvilgelsesbrevMapper.class);
        mappere.put(ProduserbartDokument.ORIENTERING_ANMODNING_UNNTAK, AnmodningUnntakMapper.class);
        mappere.put(ProduserbartDokument.MELDING_FORVENTET_SAKSBEHANDLINGSTID, ForvaltningsmeldingMapper.class);
        mappere.put(ProduserbartDokument.MELDING_MANGLENDE_OPPLYSNINGER, MangelbrevMapper.class);
        mappere.put(ProduserbartDokument.SED_A001, AttestMapper.class);
    }

    static {
        sedMappere.put(SedType.A009, A009Mapper.class);
    }

    private DokumentDataMapperRuter() {
    }

    public static BrevDataMapper brevDataMapper(ProduserbartDokument type) throws TekniskException {
        if (!mappere.containsKey(type)) {
            throw new TekniskException("ProduserbartDokument " + type.getKode() + " støttes ikke");
        }
        try {
            return mappere.get(type).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new TekniskException(e);
        }
    }

    public static AbstraktSedMapper sedMapper(SedType sedType) throws TekniskException {
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
