package no.nav.melosys.service.dokument;

import java.util.ArrayList;
import java.util.List;

public class MuligeMottakereDto {
    private final MuligMottakerDto hovedMottaker;
    private final List<MuligMottakerDto> kopiMottakere;
    private final List<MuligMottakerDto> fasteMottakere;

    private MuligeMottakereDto(Builder builder) {
        this.hovedMottaker = builder.hovedMottaker;
        this.kopiMottakere = builder.kopiMottakere;
        this.fasteMottakere = builder.fasteMottakere;
    }

    public MuligMottakerDto getHovedMottaker() {
        return hovedMottaker;
    }

    public List<MuligMottakerDto> getKopiMottakere() {
        return kopiMottakere;
    }

    public List<MuligMottakerDto> getFasteMottakere() {
        return fasteMottakere;
    }

    public static final class Builder {
        private MuligMottakerDto hovedMottaker;
        private final List<MuligMottakerDto> kopiMottakere = new ArrayList<>();
        private final List<MuligMottakerDto> fasteMottakere = new ArrayList<>();

        public Builder medHovedMottaker(MuligMottakerDto mottaker) {
            this.hovedMottaker = mottaker;
            return this;
        }

        public Builder medKopiMottaker(MuligMottakerDto mottaker) {
            this.kopiMottakere.add(mottaker);
            return this;
        }

        public Builder medFastMottaker(MuligMottakerDto mottaker) {
            this.fasteMottakere.add(mottaker);
            return this;
        }

        public MuligeMottakereDto build() {
            return new MuligeMottakereDto(this);
        }
    }
}
