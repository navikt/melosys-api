package no.nav.melosys.service.registeropplysninger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.exception.TekniskException;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.domain.SaksopplysningType.TYPER_SOM_LAGRES_INITIELT;

public class RegisteropplysningerRequest {
    private final Long behandlingID;
    private final Set<SaksopplysningType> opplysningstyper;
    private final String fnr;
    private LocalDate fom;
    private final LocalDate tom;
    private final boolean hentOpplysningerFor5aar;

    private RegisteropplysningerRequest(Long behandlingID, Set<SaksopplysningType> opplysningstyper,
                                        String fnr, LocalDate fom, LocalDate tom, boolean hentOpplysningerFor5aar) {
        this.behandlingID = behandlingID;
        this.opplysningstyper = opplysningstyper;
        this.fnr = fnr;
        this.fom = fom;
        this.tom = tom;
        this.hentOpplysningerFor5aar = hentOpplysningerFor5aar;
    }

    public static RegisteropplysningerRequestBuilder builder() {
        return new RegisteropplysningerRequestBuilder();
    }

    public Long getBehandlingID() {
        return behandlingID;
    }

    public Set<SaksopplysningType> getOpplysningstyper() {
        return opplysningstyper;
    }

    public String getFnr() {
        return fnr;
    }

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public boolean hentOpplysningerFor5aar() {
        return hentOpplysningerFor5aar;
    }

    RegisteropplysningerRequest lagKopiUtenPeriodeOgOpplysningstyperSomKreverPeriode() {
        Set<SaksopplysningType> opplysningstyperSet = new HashSet<>(getOpplysningstyper());
        opplysningstyperSet.removeAll(SaksopplysningType.KREVER_PERIODE);
        return new RegisteropplysningerRequest(getBehandlingID(), opplysningstyperSet, getFnr(), null, null, hentOpplysningerFor5aar());
    }

    public static SaksopplysningTyper hentSaksopplysningTyperSomLagres() {
        return new SaksopplysningTyper(TYPER_SOM_LAGRES_INITIELT);
    }

    public static class RegisteropplysningerRequestBuilder {
        private Long behandlingID;
        private SaksopplysningTyper saksopplysningTyper = new SaksopplysningTyper(new HashSet<>());
        private String fnr;
        private LocalDate fom;
        private LocalDate tom;
        private boolean hentRegisteropplysninger5AarFørFom;

        RegisteropplysningerRequestBuilder() {
        }

        public RegisteropplysningerRequestBuilder behandlingID(Long behandlingID) {
            this.behandlingID = behandlingID;
            return this;
        }

        public RegisteropplysningerRequestBuilder saksopplysningTyper(SaksopplysningTyper saksopplysningTyper) {
            this.saksopplysningTyper = saksopplysningTyper;
            return this;
        }

        public RegisteropplysningerRequestBuilder fnr(String fnr) {
            this.fnr = fnr;
            return this;
        }

        public RegisteropplysningerRequestBuilder fom(LocalDate fom) {
            this.fom = fom;
            return this;
        }

        public RegisteropplysningerRequestBuilder tom(LocalDate tom) {
            this.tom = tom;
            return this;
        }

        public RegisteropplysningerRequestBuilder hentOpplysningerFor5aar(boolean hentOpplysningerFor5aar) {
            this.hentRegisteropplysninger5AarFørFom = hentOpplysningerFor5aar;
            return this;
        }

        public RegisteropplysningerRequest build() {
            valider();
            return new RegisteropplysningerRequest(behandlingID, saksopplysningTyper.getOpplysningstyper(), fnr, fom, tom, hentRegisteropplysninger5AarFørFom);
        }

        private void valider() {
            if (behandlingID == null) {
                throw new TekniskException("BehandlingID er påkrevd for å hente registeropplysninger");
            }

            if (StringUtils.isEmpty(fnr) && !Collections.disjoint(SaksopplysningType.KREVER_FNR, saksopplysningTyper.getOpplysningstyper())) {
                String påkrevdeSaksopplysningstyper = intersect(SaksopplysningType.KREVER_FNR, saksopplysningTyper.getOpplysningstyper())
                    .stream().map(SaksopplysningType::getBeskrivelse).collect(Collectors.joining(", "));

                throw new TekniskException(String.format("Krever at fnr er satt ved henting av %s", påkrevdeSaksopplysningstyper));
            }
        }

        private static <T> Set<T> intersect(Set<T> left, Set<T> right) {
            return left.stream()
                .distinct()
                .filter(right::contains)
                .collect(Collectors.toSet());
        }
    }

    public static class SaksopplysningTyper {
        private final Set<SaksopplysningType> opplysningstyper;

        SaksopplysningTyper(Set<SaksopplysningType> opplysningstyper) {
            this.opplysningstyper = opplysningstyper;
        }

        public Set<SaksopplysningType> getOpplysningstyper() {
            return opplysningstyper;
        }

        public static SaksopplysningTyperBuilder builder() {
            return new SaksopplysningTyperBuilder();
        }

        public static class SaksopplysningTyperBuilder {
            private final Set<SaksopplysningType> opplysningstyper = new HashSet<>();

            SaksopplysningTyperBuilder() {
            }

            public SaksopplysningTyperBuilder arbeidsforholdopplysninger() {
                this.opplysningstyper.add(SaksopplysningType.ARBFORH);
                return this;
            }

            public SaksopplysningTyperBuilder inntektsopplysninger() {
                this.opplysningstyper.add(SaksopplysningType.INNTK);
                return this;
            }

            public SaksopplysningTyperBuilder medlemskapsopplysninger() {
                this.opplysningstyper.add(SaksopplysningType.MEDL);
                return this;
            }

            public SaksopplysningTyperBuilder organisasjonsopplysninger() {
                this.opplysningstyper.add(SaksopplysningType.ORG);
                return this;
            }

            public SaksopplysningTyperBuilder utbetalingsopplysninger() {
                this.opplysningstyper.add(SaksopplysningType.UTBETAL);
                return this;
            }

            public SaksopplysningTyper build() {
                return new SaksopplysningTyper(opplysningstyper);
            }
        }
    }
}
