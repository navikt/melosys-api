package no.nav.melosys.service.avklartefakta;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Avklartefakta;
import no.nav.melosys.domain.AvklartefaktaRegistrering;
import no.nav.melosys.domain.Behandlingsresultat;
import org.springframework.stereotype.Component;

@Component
public class AvklartefaktaDtoKonverterer {

    public Avklartefakta opprettAvklartefaktaFraDto(AvklartefaktaDto avklarteFaktaDto,
                                                    Behandlingsresultat resultat) {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setSubjekt(avklarteFaktaDto.getSubjektID());
        avklartefakta.setType(avklarteFaktaDto.getAvklartefaktaType());
        avklartefakta.setReferanse(avklarteFaktaDto.getReferanse());
        avklartefakta.setBegrunnelseFritekst(avklarteFaktaDto.getBegrunnelseFritekst());
        avklartefakta.setBehandlingsresultat(resultat);

        String fakta = avklarteFaktaDto.getFakta().stream().collect(Collectors.joining(" "));
        avklartefakta.setFakta(fakta);

        oppdaterFaktaregistreringer(avklartefakta, avklarteFaktaDto);
        return avklartefakta;
    }

    private void oppdaterFaktaregistreringer(Avklartefakta avklartefakta, AvklartefaktaDto avklarteFaktaDto) {
        Set<AvklartefaktaRegistrering> registreringer = new HashSet<>();

        if (avklarteFaktaDto.harBegrunnelseKoder()) {
            for (String begrunnelse : avklarteFaktaDto.getBegrunnelseKoder()) {
                AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
                registrering.setBegrunnelseKode(begrunnelse);
                registrering.setAvklartefakta(avklartefakta);

                registreringer.add(registrering);
            }
        }
        avklartefakta.oppdaterRegistreringer(registreringer);
    }
}
