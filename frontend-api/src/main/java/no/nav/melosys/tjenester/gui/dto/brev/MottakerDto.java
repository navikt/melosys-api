package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Trygdeavtale_myndighetsland;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MottakerDto {
    private final String type;
    private final Aktoersroller rolle;
    private final boolean orgnrSettesAvSaksbehandler;
    private final Collection<MottakerAdresseDto> adresser;
    private final String feilmelding;
    private final List<String> trygdemyndighet;

    private MottakerDto(Builder builder) {
        this.type = builder.type;
        this.rolle = builder.rolle;
        this.orgnrSettesAvSaksbehandler = builder.orgnrSettesAvSaksbehandler;
        this.adresser = builder.adresser;
        this.feilmelding = builder.feilmelding;
        this.trygdemyndighet = builder.trygdemyndighet;
    }

    public String getType() {
        return type;
    }

    public Aktoersroller getRolle() {
        return rolle;
    }

    public boolean getOrgnrSettesAvSaksbehandler() {
        return orgnrSettesAvSaksbehandler;
    }

    public Collection<MottakerAdresseDto> getAdresser() {
        return adresser;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public List<String> getTrygdemyndighet() {
        return trygdemyndighet;
    }

    public static final class Builder {
        private String type;
        private Aktoersroller rolle;
        private boolean orgnrSettesAvSaksbehandler = false;
        private Collection<MottakerAdresseDto> adresser;
        private String feilmelding;
        private List<String> trygdemyndighet;

        public Builder medType(MottakerType mottakerType) {
            this.type = mottakerType.getBeskrivelse();
            return this;
        }

        public Builder medRolle(Aktoersroller rolle) {
            this.rolle = rolle;
            return this;
        }

        public Builder orgnrSettesAvSaksbehandler() {
            this.orgnrSettesAvSaksbehandler = true;
            return this;
        }

        public Builder medAdresse(Collection<MottakerAdresseDto> adresser) {
            this.adresser = adresser;
            return this;
        }

        public Builder medFeilmelding(String feilmelding) {
            this.feilmelding = feilmelding;
            return this;
        }

        public Builder medTrygdemyndighet(List<Trygdeavtale_myndighetsland> trygdemyndighetsland) {
            this.trygdemyndighet = trygdemyndighetsland.stream().map(Trygdeavtale_myndighetsland::getBeskrivelse).toList();
            return this;
        }

        public MottakerDto build() {
            return new MottakerDto(this);
        }
    }
}
