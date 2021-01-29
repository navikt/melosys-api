package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.ArrayList;
import java.util.List;

public class BrevmalFeltDto {
    private final String kode;
    private final String beskrivelse;
    private final List<FeltvalgDto> valg;

    private BrevmalFeltDto(String kode, String beskrivelse, List<FeltvalgDto> valg) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
        this.valg = valg;
    }

    public static final class Builder {
        private String kode;
        private String beskrivelse;
        private List<FeltvalgDto> valg;

        public Builder medKode(String kode) {
            this.kode = kode;
            return this;
        }

        public Builder medBeskrivelse(String beskrivelse) {
            this.beskrivelse = beskrivelse;
            return this;
        }

        public Builder medValg(List<FeltvalgDto> valg) {
            this.valg = valg;
            return this;
        }

        public Builder medValg(FeltvalgDto valg) {
            if (this.valg == null) {
                this.valg = new ArrayList<>();
            }
            this.valg.add(valg);
            return this;
        }

        public BrevmalFeltDto build() {
            return new BrevmalFeltDto(kode, beskrivelse, valg);
        }
    }
}
