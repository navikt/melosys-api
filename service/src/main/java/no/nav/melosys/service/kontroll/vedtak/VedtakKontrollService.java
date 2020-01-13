package no.nav.melosys.service.kontroll.vedtak;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.begrunnelser.Unntak_periode_begrunnelser;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import org.springframework.stereotype.Component;

@Component
public class VedtakKontrollService {

    private static final Set<Function<VedtakKontrollData, Unntak_periode_begrunnelser>> kontroller = Set.of(
        VedtakKontroller::overlappendeMedlemsperiode, VedtakKontroller::periodeOver24Mnd
    );

    public Collection<Unntak_periode_begrunnelser> utførKontroller(Behandling behandling, Behandlingsresultat behandlingsresultat) throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = behandlingsresultat.hentValidertLovvalgsperiode();
        MedlemskapDokument medlemskapDokument = SaksopplysningerUtils.hentMedlemskapDokument(behandling);
        VedtakKontrollData vedtakKontrollData = new VedtakKontrollData(medlemskapDokument, lovvalgsperiode);
        return kontroller.stream()
            .map(f -> f.apply(vedtakKontrollData))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
