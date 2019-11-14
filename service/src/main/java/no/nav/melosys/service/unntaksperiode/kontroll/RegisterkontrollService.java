package no.nav.melosys.service.unntaksperiode.kontroll;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Metrics;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.domain.kodeverk.begrunnelser.Unntak_periode_begrunnelser;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import org.springframework.stereotype.Service;

import static no.nav.melosys.metrics.MetrikkerNavn.TAG_BEGRUNNELSE;
import static no.nav.melosys.metrics.MetrikkerNavn.UNNTAKSPERIODE_KONTROLL_TREFF;

@Service
public class RegisterkontrollService {

    private final KontrollFactory kontrollFactory;

    public RegisterkontrollService(KontrollFactory kontrollFactory) {
        this.kontrollFactory = kontrollFactory;
    }

    static {
        Arrays.stream(Unntak_periode_begrunnelser.values())
            .forEach(b -> Metrics.counter(UNNTAKSPERIODE_KONTROLL_TREFF, TAG_BEGRUNNELSE, b.getKode()));
    }

    public List<Unntak_periode_begrunnelser> utførKontroller(Behandling behandling) throws TekniskException {
        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        if (feilIPeriode(sedDokument)) {
            return Collections.singletonList(Unntak_periode_begrunnelser.FEIL_I_PERIODEN);
        }

        PersonDokument personDokument = SaksopplysningerUtils.hentPersonDokument(behandling);
        MedlemskapDokument medlemskapDokument = SaksopplysningerUtils.hentMedlemskapDokument(behandling);
        InntektDokument inntektDokument = SaksopplysningerUtils.hentInntektDokument(behandling);
        UtbetalingDokument utbetalingDokument = SaksopplysningerUtils.finnUtbetalingDokument(behandling).orElse(null);
        KontrollData kontrollData = new KontrollData(sedDokument, personDokument, medlemskapDokument, inntektDokument, utbetalingDokument);

        return utførKontroller(kontrollData, kontrollFactory.hentKontrollerForSedType(sedDokument.getSedType()));
    }

    private List<Unntak_periode_begrunnelser> utførKontroller(KontrollData kontrollData,
                                                                     Collection<Function<KontrollData, Unntak_periode_begrunnelser>> kontroller) {
        return kontroller.stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .peek(this::registrerMetrikk)
            .collect(Collectors.toList());
    }

    private void registrerMetrikk(Unntak_periode_begrunnelser unntak_periode_begrunnelse) {
        Metrics.counter(UNNTAKSPERIODE_KONTROLL_TREFF, TAG_BEGRUNNELSE, unntak_periode_begrunnelse.getKode()).increment();
    }

    private boolean feilIPeriode(SedDokument sedDokument) {
        return PeriodeKontroller.feilIPeriode(
            sedDokument.getLovvalgsperiode().getFom(),
            sedDokument.getLovvalgsperiode().getTom());
    }
}
