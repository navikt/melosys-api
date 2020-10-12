package no.nav.melosys.domain.avklartefakta;

import java.util.List;

public class AvklarteMedfolgendeFamiliemedlemmer {

    public final List<AvklartFamiliemedlem> avklarteFamiliemedlemmer;
    public final String begrunnelseFritekst;

    public AvklarteMedfolgendeFamiliemedlemmer(List<AvklartFamiliemedlem> avklarteFamiliemedlemmer, String begrunnelseFritekst) {
        this.avklarteFamiliemedlemmer = avklarteFamiliemedlemmer;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }
}
