package no.nav.melosys.tjenester.gui.dto.trygdeavtale;

import no.nav.melosys.tjenester.gui.dto.MedfolgendeFamilieDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record TrygdeAvtaleDataForVedtakDto(
    List<String> virksomheter,
    String vedtak,
    String innvilgelse,
    String bestemmelse,
    List<MedfolgendeFamilieDto> barn,
    MedfolgendeFamilieDto ektefelle
) {
    // Mulig å bytte ut med https://github.com/Randgalt/record-builder
    static public class Builder {
        private List<String> virksomheter;
        private String vedtak;
        private String innvilgelse;
        private String bestemmelse;
        private final List<MedfolgendeFamilieDto> barn = new ArrayList<>();
        private MedfolgendeFamilieDto ektefelle;

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
