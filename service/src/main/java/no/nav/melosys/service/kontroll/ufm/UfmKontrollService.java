package no.nav.melosys.service.kontroll.ufm;

import java.util.*;
import java.util.function.Function;

import io.micrometer.core.instrument.Metrics;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static no.nav.melosys.metrics.MetrikkerNavn.TAG_BEGRUNNELSE;
import static no.nav.melosys.metrics.MetrikkerNavn.UNNTAKSPERIODE_KONTROLL_TREFF;

@Service
@Primary
public class UfmKontrollService {

    private final KontrollFactory kontrollFactory;
    private final PersondataFasade persondataFasade;

    public UfmKontrollService(KontrollFactory kontrollFactory, PersondataFasade persondataFasade) {
        this.kontrollFactory = kontrollFactory;
        this.persondataFasade = persondataFasade;
    }

    static {
        Arrays.stream(Kontroll_begrunnelser.values())
            .forEach(b -> Metrics.counter(UNNTAKSPERIODE_KONTROLL_TREFF, TAG_BEGRUNNELSE, b.getKode()));
    }

    public List<Kontroll_begrunnelser> utførKontroller(Behandling behandling) {
        SedDokument sedDokument = behandling.hentSedDokument();
        if (feilIPeriode(sedDokument)) {
            return Collections.singletonList(Kontroll_begrunnelser.FEIL_I_PERIODEN);
        }

        Persondata persondata = hentPersondata(behandling);
        MedlemskapDokument medlemskapDokument = behandling.hentMedlemskapDokument();
        InntektDokument inntektDokument = behandling.hentInntektDokument();
        UtbetalingDokument utbetalingDokument = behandling.finnUtbetalingDokument().orElse(null);
        UfmKontrollData kontrollData = new UfmKontrollData(sedDokument, persondata, medlemskapDokument, inntektDokument, utbetalingDokument);

        return utførKontroller(kontrollData, kontrollFactory.hentKontrollerForSedType(sedDokument.getSedType()));
    }

    private Persondata hentPersondata(Behandling behandling) {
        return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID());
    }

    private List<Kontroll_begrunnelser> utførKontroller(UfmKontrollData kontrollData,
                                                              Collection<Function<UfmKontrollData, Kontroll_begrunnelser>> kontroller) {
        return kontroller.stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .peek(this::registrerMetrikk) //NOSONAR
            .toList();
    }

    private void registrerMetrikk(Kontroll_begrunnelser unntak_periode_begrunnelse) {
        Metrics.counter(UNNTAKSPERIODE_KONTROLL_TREFF, TAG_BEGRUNNELSE, unntak_periode_begrunnelse.getKode()).increment();
    }

    private boolean feilIPeriode(SedDokument sedDokument) {
        return PeriodeKontroller.feilIPeriode(
            sedDokument.getLovvalgsperiode().getFom(),
            sedDokument.getLovvalgsperiode().getTom());
    }
}
