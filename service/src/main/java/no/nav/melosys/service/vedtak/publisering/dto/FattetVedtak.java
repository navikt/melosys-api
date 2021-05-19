package no.nav.melosys.service.vedtak.publisering.dto;

import java.util.Collection;

public record FattetVedtak(Sak sak, Vedtak vedtak, Soeknad soknad, Saksopplysninger saksopplysninger,
                           Collection<AvklarteFakta> avklarteFakta,
                           Collection<LovvalgOgMedlemskapsperiode> lovvalgOgMedlemskapsperioder,
                           Fullmektig fullmektig, RepresentantAvgift representantAvgift) {
}
