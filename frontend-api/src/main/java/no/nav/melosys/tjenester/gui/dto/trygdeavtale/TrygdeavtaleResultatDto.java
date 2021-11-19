package no.nav.melosys.tjenester.gui.dto.trygdeavtale;

import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.service.trygdeavtale.TrygdeavtaleService;
import no.nav.melosys.tjenester.gui.dto.MedfolgendeFamilieDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record TrygdeavtaleResultatDto(
    List<String> virksomheter,
    String bestemmelse,
    List<MedfolgendeFamilieDto> barn,
    MedfolgendeFamilieDto ektefelle
) {
    public TrygdeavtaleService.TrygdeavtaleResultat til() {
        return new TrygdeavtaleService.TrygdeavtaleResultat.Builder()
            .familie(lagAvklarteMedfolgendeFamilie())
            .bestemmelse(bestemmelse)
            .virksomheter(virksomheter)
            .build();
    }

    private AvklarteMedfolgendeFamilie lagAvklarteMedfolgendeFamilie() {
        var omfattetFamilie = Stream.concat(barn.stream(), Stream.of(ektefelle))
            .filter(MedfolgendeFamilieDto::omfattet)
            .map(f -> new OmfattetFamilie(f.uuid()))
            .collect(Collectors.toSet());
        var ikkeOmfattetFamilie = Stream.concat(barn.stream(), Stream.of(ektefelle))
            .filter(MedfolgendeFamilieDto::erIkkeOmfattet)
            .map(f -> new IkkeOmfattetFamilie(f.uuid(), f.begrunnelseKode(), f.begrunnelseFritekst()))
            .collect(Collectors.toSet());

        return new AvklarteMedfolgendeFamilie(omfattetFamilie, ikkeOmfattetFamilie);
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
