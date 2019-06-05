package no.nav.melosys.service.unntaksperiode.kontroll;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;

import static no.nav.melosys.service.unntaksperiode.kontroll.UnntaksperiodeKontroller.gyldigPeriode;

public final class UnntaksperiodeKontroll {

    private UnntaksperiodeKontroll() {
    }

    public static List<Unntak_periode_begrunnelser> utførKontroller(Behandling behandling, String sedType) throws TekniskException {
        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        PersonDokument personDokument = SaksopplysningerUtils.hentPersonDokument(behandling);
        MedlemskapDokument medlemskapDokument = SaksopplysningerUtils.hentMedlemskapDokument(behandling);
        InntektDokument inntektDokument = SaksopplysningerUtils.hentInntektDokument(behandling);
        KontrollData kontrollData = new KontrollData(sedDokument, personDokument, medlemskapDokument, inntektDokument);

        return utførKontroller(kontrollData, KontrollFactory.hentKontrollerForSedType(sedType));
    }

    private static List<Unntak_periode_begrunnelser> utførKontroller(KontrollData kontrollData,
                                                                     Collection<Function<KontrollData, Unntak_periode_begrunnelser>> kontroller) {

        Unntak_periode_begrunnelser feilIPeriode = gyldigPeriode(kontrollData);
        if (feilIPeriode != null) {
            return Collections.singletonList(feilIPeriode);
        }

        return kontroller.stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
