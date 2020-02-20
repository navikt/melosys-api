package no.nav.melosys.service.registeropplysninger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.exception.TekniskException;

public class RegisteropplysningerRequest {
    private final Behandling behandling;
    private final Long behandlingID;
    private final List<SaksopplysningType> opplysningstyper;
    private final String fnr;
    private final LocalDate fom;
    private final LocalDate tom;

    public RegisteropplysningerRequest(Behandling behandling, List<SaksopplysningType> opplysningstyper, String fnr, LocalDate fom, LocalDate tom) {
        this.behandling = behandling;
        this.behandlingID = null;
        this.opplysningstyper = opplysningstyper;
        this.fnr = fnr;
        this.fom = fom;
        this.tom = tom;
    }

    public RegisteropplysningerRequest(Long behandlingID, List<SaksopplysningType> opplysningstyper, String fnr, LocalDate fom, LocalDate tom) {
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

    public List<SaksopplysningType> getOpplysningstyper() {
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
        private SaksopplysningTyper saksopplysningTyper = new SaksopplysningTyper(new ArrayList<>());
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
        }
    }

    public static class SaksopplysningTyper {
        private final List<SaksopplysningType> opplysningstyper;

        SaksopplysningTyper(List<SaksopplysningType> opplysningstyper) {
            this.opplysningstyper = opplysningstyper;
        }

        public List<SaksopplysningType> getOpplysningstyper() {
            return opplysningstyper;
        }

        public static SaksopplysningTyperBuilder builder() {
            return new SaksopplysningTyperBuilder();
        }

        public static class SaksopplysningTyperBuilder {
            private List<SaksopplysningType> opplysningstyper = new ArrayList<>();

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
