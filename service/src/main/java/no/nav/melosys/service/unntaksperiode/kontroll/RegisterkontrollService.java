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
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.domain.kodeverk.begrunnelser.Unntak_periode_begrunnelser;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import org.springframework.stereotype.Service;

import static no.nav.melosys.service.unntaksperiode.kontroll.UnntaksperiodeKontroller.feilIPeriode;

@Service
public class RegisterkontrollService {

    private final KontrollFactory kontrollFactory;

    public RegisterkontrollService(KontrollFactory kontrollFactory) {
        this.kontrollFactory = kontrollFactory;
    }

    public List<Unntak_periode_begrunnelser> utførKontroller(Behandling behandling) throws TekniskException {
        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        PersonDokument personDokument = SaksopplysningerUtils.hentPersonDokument(behandling);
        MedlemskapDokument medlemskapDokument = SaksopplysningerUtils.hentMedlemskapDokument(behandling);
        InntektDokument inntektDokument = SaksopplysningerUtils.hentInntektDokument(behandling);
        UtbetalingDokument utbetalingDokument = SaksopplysningerUtils.hentUtbetalingDokument(behandling);
        KontrollData kontrollData = new KontrollData(sedDokument, personDokument, medlemskapDokument, inntektDokument, utbetalingDokument);

        return utførKontroller(kontrollData, kontrollFactory.hentKontrollerForSedType(sedDokument.getSedType()));
    }

    private List<Unntak_periode_begrunnelser> utførKontroller(KontrollData kontrollData,
                                                                     Collection<Function<KontrollData, Unntak_periode_begrunnelser>> kontroller) {

        Unntak_periode_begrunnelser feilIPeriode = feilIPeriode(kontrollData);
        if (feilIPeriode != null) {
            return Collections.singletonList(feilIPeriode);
        }

        return kontroller.stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
