package no.nav.melosys.service.registeropplysninger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import org.apache.commons.lang3.StringUtils;

public class RegisteropplysningerRequest {
    private final Behandling behandling;
    private final Long behandlingID;
    private final Set<SaksopplysningType> opplysningstyper;
    private final String fnr;
    private final LocalDate fom;
    private final LocalDate tom;

    public RegisteropplysningerRequest(Behandling behandling, Set<SaksopplysningType> opplysningstyper, String fnr, LocalDate fom, LocalDate tom) {
        this.behandling = behandling;
        this.behandlingID = null;
        this.opplysningstyper = opplysningstyper;
        this.fnr = fnr;
        this.fom = fom;
        this.tom = tom;
    }

    public RegisteropplysningerRequest(Long behandlingID, Set<SaksopplysningType> opplysningstyper, String fnr, LocalDate fom, LocalDate tom) {
        this.behandling = null;
        this.behandlingID = behandlingID;
        this.opplysningstyper = opplysningstyper;
        this.fnr = fnr;
        this.fom = fom;
        this.tom = tom;
    }

    public static RegisteropplysningerRequestBuilder builder() {
        return new RegisteropplysningerRequestBuilder();
    }

    public Behandling getBehandling() {
        return behandling;
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

    public static class RegisteropplysningerRequestBuilder {
        private Behandling behandling;
        private Long behandlingID;
        private SaksopplysningTyper saksopplysningTyper = new SaksopplysningTyper(new HashSet<>());
        private String fnr;
        private LocalDate fom;
        private LocalDate tom;

        RegisteropplysningerRequestBuilder() {
        }

        public RegisteropplysningerRequestBuilder behandling(Behandling behandling) {
            this.behandling = behandling;
            return this;
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

        public RegisteropplysningerRequest build() throws TekniskException {
            valider();
            if (behandling != null) {
                return new RegisteropplysningerRequest(behandling, saksopplysningTyper.getOpplysningstyper(), fnr, fom, tom);
            } else {
                return new RegisteropplysningerRequest(behandlingID, saksopplysningTyper.getOpplysningstyper(), fnr, fom, tom);
            }
        }

        private void valider() throws TekniskException {
            if (behandling == null && behandlingID == null) {
                throw new TekniskException("Behandling eller behandlingID er påkrevd for å hente registeropplysninger");
            }

            if (saksopplysningTyper.getOpplysningstyper().isEmpty()) {
                throw new TekniskException("Krever minst én saksopplysningstype for å hente registeropplysninger");
            }

            if (StringUtils.isEmpty(fnr) && !Collections.disjoint(kreverFnr, saksopplysningTyper.getOpplysningstyper())) {
                String påkrevdeSaksopplysningstyper = intersect(kreverFnr, saksopplysningTyper.getOpplysningstyper())
                    .stream().map(SaksopplysningType::getBeskrivelse).collect(Collectors.joining(", "));

                throw new TekniskException(String.format("Krever at fnr er satt ved henting av %s", påkrevdeSaksopplysningstyper));
            }

            if (PeriodeKontroller.feilIPeriode(fom, tom) && !Collections.disjoint(kreverPeriode, saksopplysningTyper.getOpplysningstyper())) {
                String påkrevdeSaksopplysningstyper = intersect(kreverPeriode, saksopplysningTyper.getOpplysningstyper())
                    .stream().map(SaksopplysningType::getBeskrivelse).collect(Collectors.joining(", "));

                throw new TekniskException(String.format("Feil i periode: %s krever en gyldig periode", påkrevdeSaksopplysningstyper));
            }
        }

        private static <T> Set<T> intersect(Set<T> l1, Set<T> l2) {
            return l1.stream()
                .distinct()
                .filter(l2::contains)
                .collect(Collectors.toSet());
        }

        private static final Set<SaksopplysningType> kreverFnr = Set.of(
            SaksopplysningType.ARBFORH,
            SaksopplysningType.INNTK,
            SaksopplysningType.MEDL,
            SaksopplysningType.PERSHIST,
            SaksopplysningType.PERSOPL,
            SaksopplysningType.SOB_SAK,
            SaksopplysningType.UTBETAL
        );

        private static final Set<SaksopplysningType> kreverPeriode = Set.of(
            SaksopplysningType.ARBFORH,
            SaksopplysningType.INNTK,
            SaksopplysningType.MEDL,
            SaksopplysningType.PERSHIST,
            SaksopplysningType.UTBETAL
        );
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
            private Set<SaksopplysningType> opplysningstyper = new HashSet<>();

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
