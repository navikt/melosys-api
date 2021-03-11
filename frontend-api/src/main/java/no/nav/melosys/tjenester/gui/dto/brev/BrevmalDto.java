package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public class BrevmalDto {
    private final Produserbaredokumenter type;
    private final String beskrivelse;
    private final List<BrevmalFeltDto> felter;
    private final List<MottakerDto> muligeMottakere;
    private final String mottakereHjelpetekst;

    private BrevmalDto(Produserbaredokumenter type, String beskrivelse, List<BrevmalFeltDto> felter, List<MottakerDto> muligeMottakere, String mottakereHjelpetekst) {
        this.type = type;
        this.beskrivelse = beskrivelse;
        this.felter = felter;
        this.muligeMottakere = muligeMottakere;
        this.mottakereHjelpetekst = mottakereHjelpetekst;
    }

    public Produserbaredokumenter getType() {
        return type;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public List<BrevmalFeltDto> getFelter() {
        return felter;
    }

    public List<MottakerDto> getMuligeMottakere() {
        return muligeMottakere;
    }

    public String getMottakereHjelpetekst() {
        return mottakereHjelpetekst;
    }

    public static final class Builder {
        private Produserbaredokumenter type;
        private String beskrivelse;
        private List<BrevmalFeltDto> felter;
        private List<MottakerDto> muligeMottakere;
        private String mottakereHjelpetekst;

        public Builder medType(Produserbaredokumenter type) {
            this.type = type;
            return this;
        }

        public Builder medBeskrivelse(String beskrivelse) {
            this.beskrivelse = beskrivelse;
            return this;
        }

        public Builder medFelter(List<BrevmalFeltDto> felter) {
            this.felter = felter;
            return this;
        }


        public Builder medFelt(BrevmalFeltDto felt) {
            if (this.felter == null) {
                this.felter = new ArrayList<>();
            }
            this.felter.add(felt);
            return this;
        }

        public Builder medMuligeMottakere(List<MottakerDto> muligeMottaker) {
            this.muligeMottakere = muligeMottaker;
            return this;
        }

        public Builder medMuligMottaker(MottakerDto mottaker) {
            if (this.muligeMottakere == null) {
                this.muligeMottakere = new ArrayList<>();
            }
            this.muligeMottakere.add(mottaker);
            return this;
        }

        public Builder medMottakereHjelpetekst(String mottakereHjelpetekst) {
            this.mottakereHjelpetekst = mottakereHjelpetekst;
            return this;
        }

        public BrevmalDto build() {
            return new BrevmalDto(type, beskrivelse, felter, muligeMottakere, mottakereHjelpetekst);
        }
    }
}
