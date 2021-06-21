package no.nav.melosys.service.registeropplysninger;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import org.apache.commons.lang3.StringUtils;

public class RegisteropplysningerRequest {
    private final Long behandlingID;
    private final Set<SaksopplysningType> opplysningstyper;
    private final String fnr;
    private final LocalDate fom;
    private final LocalDate tom;
    private final Informasjonsbehov informasjonsbehov;

    private RegisteropplysningerRequest(Long behandlingID, Set<SaksopplysningType> opplysningstyper,
                                       String fnr, LocalDate fom, LocalDate tom, Informasjonsbehov informasjonsbehov) {
        this.behandlingID = behandlingID;
        this.opplysningstyper = opplysningstyper;
        this.fnr = fnr;
        this.fom = fom;
        this.tom = tom;
        this.informasjonsbehov = informasjonsbehov;
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

    public LocalDate getTom() {
        return tom;
    }

    public Informasjonsbehov getInformasjonsbehov() {
        return Objects.requireNonNullElse(informasjonsbehov, Informasjonsbehov.STANDARD);
    }

    RegisteropplysningerRequest lagKopiUtenPeriodeOgOpplysningstyperSomKreverPeriode() {
        Set<SaksopplysningType> opplysningstyperSet = getOpplysningstyper().stream().collect(Collectors.toSet());
        opplysningstyperSet.removeAll(SaksopplysningType.KREVER_PERIODE);
        return new RegisteropplysningerRequest(getBehandlingID(), opplysningstyperSet, getFnr(), null, null, getInformasjonsbehov());
    }

    // Støtter ikke type SEDOPPL
    public static SaksopplysningTyper hentAlleSaksopplysningTyper() {
        return new SaksopplysningTyper(
            Arrays.stream(SaksopplysningType.values()).filter(s -> !SaksopplysningType.SEDOPPL.equals(s)).collect(Collectors.toSet())
        );
    }

    public static class RegisteropplysningerRequestBuilder {
        private Long behandlingID;
        private SaksopplysningTyper saksopplysningTyper = new SaksopplysningTyper(new HashSet<>());
        private String fnr;
        private LocalDate fom;
        private LocalDate tom;
        private Informasjonsbehov informasjonsbehov;

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

        public RegisteropplysningerRequestBuilder informasjonsbehov(Informasjonsbehov informasjonsbehov) {
            this.informasjonsbehov = informasjonsbehov;
            return this;
        }

        public RegisteropplysningerRequest build() {
            valider();
            return new RegisteropplysningerRequest(behandlingID, saksopplysningTyper.getOpplysningstyper(), fnr, fom, tom, informasjonsbehov);
        }

        private void valider() {
            if (behandlingID == null) {
                throw new TekniskException("BehandlingID er påkrevd for å hente registeropplysninger");
            }

            if (saksopplysningTyper.getOpplysningstyper().isEmpty()) {
                throw new TekniskException("Krever minst én saksopplysningstype for å hente registeropplysninger");
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

            public SaksopplysningTyperBuilder personhistorikkopplysninger() {
                this.opplysningstyper.add(SaksopplysningType.PERSHIST);
                return this;
            }

            public SaksopplysningTyperBuilder personopplysninger() {
                this.opplysningstyper.add(SaksopplysningType.PERSOPL);
                return this;
            }

            public SaksopplysningTyperBuilder sakOgBehandlingopplysninger() {
                this.opplysningstyper.add(SaksopplysningType.SOB_SAK);
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
