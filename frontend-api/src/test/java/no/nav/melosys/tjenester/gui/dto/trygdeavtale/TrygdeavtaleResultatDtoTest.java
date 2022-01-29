package no.nav.melosys.tjenester.gui.dto.trygdeavtale;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.service.trygdeavtale.TrygdeavtaleResultat;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TrygdeavtaleResultatDtoTest {

    private static final String ORGID_1 = "11111111111";
    private static final String EKTEFELLE_FNR = "01108049800";
    private static final String BARN1_FNR = "01100099728";
    private static final String BARN2_FNR = "02109049878";
    private static final String BARN_NAVN_1 = "Doffen Duck";
    private static final String BARN_NAVN_2 = "Dole Duck";
    private static final String EKTEFELLE_NAVN = "Dolly Duck";
    private final static String UUID_BARN_1 = UUID.randomUUID().toString();
    private final static String UUID_BARN_2 = UUID.randomUUID().toString();
    private final static String UUID_EKTEFELLE = UUID.randomUUID().toString();

    @Test
    void fra() throws JsonProcessingException {

        TrygdeavtaleResultat resultat = new TrygdeavtaleResultat
            .Builder()
            .virksomhet(ORGID_1)
            .bestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1.getKode())
            .familie(lagAvklartMedfølgendeBarn()).build();

        MedfolgendeFamilie e1 = MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_EKTEFELLE, EKTEFELLE_FNR, EKTEFELLE_NAVN, MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER
        );
        MedfolgendeFamilie b1 = MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_BARN_1, BARN1_FNR, BARN_NAVN_1, MedfolgendeFamilie.Relasjonsrolle.BARN
        );
        MedfolgendeFamilie b2 = MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_BARN_2, BARN2_FNR, BARN_NAVN_2, MedfolgendeFamilie.Relasjonsrolle.BARN
        );
        TrygdeavtaleResultatDto fra = TrygdeavtaleResultatDto.fra(resultat, List.of(e1, b1, b2));

        String result = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(fra);
        System.out.println(result);
    }

    private AvklarteMedfolgendeFamilie lagAvklartMedfølgendeBarn() {
        var ektefelle = new OmfattetFamilie(UUID_EKTEFELLE);
        var barn1 = new OmfattetFamilie(UUID_BARN_1);
        barn1.setIdent(BARN1_FNR);
        var barn2 = new IkkeOmfattetFamilie(
            UUID_BARN_2,
            Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(),
            null);
        barn2.setIdent(BARN2_FNR);
        return new AvklarteMedfolgendeFamilie(
            Set.of(ektefelle, barn1),
            Set.of(barn2)
        );
    }
}
