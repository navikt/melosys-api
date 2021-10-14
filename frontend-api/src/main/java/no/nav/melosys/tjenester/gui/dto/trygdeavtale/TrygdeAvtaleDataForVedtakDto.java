package no.nav.melosys.tjenester.gui.dto.trygdeavtale;

import no.nav.melosys.tjenester.gui.dto.MedfolgendeFamilieDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record TrygdeAvtaleDataForVedtakDto(
    LocalDate fom,
    LocalDate tom,
    List<String> land,
    List<String> virksomheter,
    String vedtak,
    String innvilgelse,
    String bestemmelse,
    List<MedfolgendeFamilieDto> barn,
    MedfolgendeFamilieDto ektefelle
) {
    // Mulig å bytte ut med https://github.com/Randgalt/record-builder
    static public class Builder {
        private LocalDate fom;
        private LocalDate tom;
        private List<String> land;
        private List<String> virksomheter;
        private String vedtak;
        private String innvilgelse;
        private String bestemmelse;
        private final List<MedfolgendeFamilieDto> barn = new ArrayList<>();
        private MedfolgendeFamilieDto ektefelle;

        public Builder fom(LocalDate fom) {
            this.fom = fom;
            return this;
        }

        public Builder tom(LocalDate tom) {
            this.tom = tom;
            return this;
        }

        public Builder land(List<String> land) {
            this.land = land;
            return this;
        }

        public Builder virksomheter(List<String> virksomheter) {
            this.virksomheter = virksomheter;
            return this;
        }

        public Builder innvilgelse(String innvilgelse) {
            this.innvilgelse = innvilgelse;
            return this;
        }

        public Builder vedtak(String vedtak) {
            this.vedtak = vedtak;
            return this;
        }

        public Builder bestemmelse(String bestemmelse) {
            this.bestemmelse = bestemmelse;
            return this;
        }

        public Builder addBarn(String uuid, boolean omfattet, String begrunnelseKode, String begrunnelseFritekst) {
            this.barn.add(new MedfolgendeFamilieDto(uuid, omfattet, begrunnelseKode, begrunnelseFritekst));
            return this;
        }

        public Builder ektefelle(String uuid, boolean omfattet, String begrunnelseKode, String begrunnelseFritekst) {
            this.ektefelle = new MedfolgendeFamilieDto(uuid, omfattet, begrunnelseKode, begrunnelseFritekst);
            return this;
        }

        public TrygdeAvtaleDataForVedtakDto build() {
            return new TrygdeAvtaleDataForVedtakDto(
                fom,
                tom,
                land,
                virksomheter,
                vedtak,
                innvilgelse,
                bestemmelse,
                barn,
                ektefelle
            );
        }

    }

}
