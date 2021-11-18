package no.nav.melosys.tjenester.gui.dto.trygdeavtale;

import no.nav.melosys.service.TrygdeavtaleService;
import no.nav.melosys.tjenester.gui.dto.MedfolgendeFamilieDto;

import java.util.ArrayList;
import java.util.List;

public record TrygdeavtaleResultatDto(
    List<String> virksomheter,
    String bestemmelse,
    List<MedfolgendeFamilieDto> barn,
    MedfolgendeFamilieDto ektefelle
) {
    public TrygdeavtaleService.TrygdeavtaleResultat til() {
        return new TrygdeavtaleService.TrygdeavtaleResultat.Builder()
            .barn(this.barn.stream()
                .map(b -> new TrygdeavtaleService.Familie(b.uuid(), b.omfattet(), b.begrunnelseKode(), b.begrunnelseFritekst()))
                .toList())
            .ektefelle(ektefelle.uuid(), ektefelle.omfattet(), ektefelle.begrunnelseKode(), ektefelle.begrunnelseFritekst())
            .bestemmelse(bestemmelse)
            .virksomheter(virksomheter)
            .build();
    }

    public static class Builder {
        private List<String> virksomheter;
        private String bestemmelse;
        private final List<MedfolgendeFamilieDto> barn = new ArrayList<>();
        private MedfolgendeFamilieDto ektefelle;

        public Builder virksomheter(List<String> virksomheter) {
            this.virksomheter = virksomheter;
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

        public TrygdeavtaleResultatDto build() {
            return new TrygdeavtaleResultatDto(
                virksomheter,
                bestemmelse,
                barn,
                ektefelle
            );
        }
    }
}
