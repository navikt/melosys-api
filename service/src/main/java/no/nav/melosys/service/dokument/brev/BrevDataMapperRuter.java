package no.nav.melosys.service.dokument.brev;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.mapper.*;

final class BrevDataMapperRuter {
    private static final Map<Produserbaredokumenter, Class<? extends BrevDataMapper>> DOKUMENT_DATAMAPPER_MAP =
        Maps.immutableEnumMap(ImmutableMap.<Produserbaredokumenter, Class<? extends BrevDataMapper>>builder()
            .put(Produserbaredokumenter.ANMODNING_UNNTAK, AttestMapper.class)
            .put(Produserbaredokumenter.ATTEST_A1, AttestMapper.class)
            .put(Produserbaredokumenter.AVSLAG_ARBEIDSGIVER, AvslagArbeidsgiverMapper.class)
            .put(Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER, AvslagManglendeOpplysningerMapper.class) //TODO: Fjern når ny løsning er ferdigtestet (toggle "melosys.brev.AVSLAG_MANGLENDE_OPPLYSNINGER").
            .put(Produserbaredokumenter.AVSLAG_YRKESAKTIV, AvslagYrkesaktivMapper.class)
            .put(Produserbaredokumenter.INNVILGELSE_ARBEIDSGIVER, InnvilgelseArbeidsgiverMapper.class)
            .put(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV, InnvilgelsesbrevMapper.class)
            .put(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV_FLERE_LAND, InnvilgelsesbrevFlereLandMapper.class)
            .put(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID, ForvaltningsmeldingMapper.class)
            .put(Produserbaredokumenter.MELDING_HENLAGT_SAK, HenleggelsesbrevMapper.class)
            .put(Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER, MangelbrevMapper.class)
            .put(Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK, AnmodningUnntakMapper.class)
            .put(Produserbaredokumenter.ORIENTERING_UTPEKING_UTLAND, UtpekingAnnetLandMapper.class)
            .put(Produserbaredokumenter.ORIENTERING_VIDERESENDT_SOEKNAD, VideresendSoknadMapper.class)
            .build());

    private BrevDataMapperRuter() {
    }

    static BrevDataMapper brevDataMapper(Produserbaredokumenter type) {
        if (!DOKUMENT_DATAMAPPER_MAP.containsKey(type)) {
            throw new TekniskException("Produserbaredokumenter " + type.getKode() + " støttes ikke");
        }

        try {
            return DOKUMENT_DATAMAPPER_MAP.get(type).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new TekniskException(e);
        }
    }
}
