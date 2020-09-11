package no.nav.melosys.saksflyt.prosessflyt;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.MANGELBREV;

public final class ProsessflytFactory {

    private ProsessflytFactory() {}

    private static final Map<ProsessType, List<ProsessSteg>> PROSESS_FLYT_MAP = new EnumMap<>(ProsessType.class);

    static {
        PROSESS_FLYT_MAP.put(
            ProsessType.MANGELBREV,
            List.of(MANGELBREV)
        );
    }

    public static Optional<ProsessFlyt> lag(ProsessType prosessType) {
        return Optional.ofNullable(PROSESS_FLYT_MAP.get(prosessType)).map(l -> new ProsessFlyt(prosessType, l));
    }
}
