package no.nav.melosys.service.dokument.brev;

import java.util.EnumMap;
import java.util.Map;

import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.mapper.*;

public class BrevDataMapperRuter {

    static Map<Produserbaredokumenter, Class<? extends BrevDataMapper>> mappere = new EnumMap<>(Produserbaredokumenter.class);

    static {
        mappere.put(Produserbaredokumenter.ANMODNING_UNNTAK, AttestMapper.class);
        mappere.put(Produserbaredokumenter.ATTEST_A1, AttestMapper.class);
        mappere.put(Produserbaredokumenter.AVSLAG_YRKESAKTIV, AvslagYrkesaktivMapper.class);
        mappere.put(Produserbaredokumenter.AVSLAG_ARBEIDSGIVER, AvslagArbeidsgiverMapper.class);
        mappere.put(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV, InnvilgelsesbrevMapper.class);
        mappere.put(Produserbaredokumenter.INNVILGELSE_ARBEIDSGIVER, InnvilgelseArbeidsgiverMapper.class);
        mappere.put(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID, ForvaltningsmeldingMapper.class);
        mappere.put(Produserbaredokumenter.MELDING_HENLAGT_SAK, HenleggelsesbrevMapper.class);
        mappere.put(Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER, MangelbrevMapper.class);
        mappere.put(Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK, AnmodningUnntakMapper.class);
    }

    private BrevDataMapperRuter() {
    }

    public static BrevDataMapper brevDataMapper(Produserbaredokumenter type) throws TekniskException {
        if (!mappere.containsKey(type)) {
            throw new TekniskException("Produserbaredokumenter " + type.getKode() + " støttes ikke");
        }
        try {
            return mappere.get(type).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new TekniskException(e);
        }
    }
}
