package no.nav.melosys.service.avklartefakta;

import no.nav.melosys.domain.Avklartefakta;
import no.nav.melosys.domain.AvklartefaktaRegistrering;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AvklartefaktaDtoKonverterer {

    private AvklarteFaktaRepository avklarteFaktaRepository;

    @Autowired
    public AvklartefaktaDtoKonverterer(AvklarteFaktaRepository avklarteFaktaRepository) {
        this.avklarteFaktaRepository = avklarteFaktaRepository;
    }

    public void oppdaterAvklartefaktaFraDto(Avklartefakta avklartefakta, AvklartefaktaDto avklarteFaktaDto) {
        avklartefakta.setSubjekt(avklarteFaktaDto.getSubjektID());
        avklartefakta.setAvklartefaktakode(avklarteFaktaDto.getAvklartefaktaKode());
        avklartefakta.setReferanse(avklarteFaktaDto.getReferanse());
        avklartefakta.setBegrunnelseFritekst(avklarteFaktaDto.getBegrunnelsefritekst());

        String fakta = avklarteFaktaDto.getFakta().stream().collect(Collectors.joining(" "));
        avklartefakta.setFakta(fakta);

        oppdaterFaktaregistreringer(avklartefakta, avklarteFaktaDto);
    }

    private void oppdaterFaktaregistreringer(Avklartefakta avklartefakta, AvklartefaktaDto avklarteFaktaDto) {
        Set<AvklartefaktaRegistrering> registreringer = new HashSet<>();

        if (avklarteFaktaDto.harBegrunnelseKoder()) {
            for (String begrunnelse : avklarteFaktaDto.getBegrunnelsekoder()) {
                AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
                registrering.setBegrunnelseKode(begrunnelse);
                registrering.setAvklartefakta(avklartefakta);

                registreringer.add(registrering);
                avklartefakta.oppdaterRegistreringer(registreringer);
            }
        }
    }

    public Set<AvklartefaktaDto> lagDtoFraAvklartefakta(Set<Avklartefakta> avklartefakta) {
        Set<AvklartefaktaDto> avklartefaktaDtoer = new HashSet<>();

        for (Avklartefakta a : avklartefakta) {
            avklartefaktaDtoer.add(new AvklartefaktaDto(a));
        }
        return avklartefaktaDtoer;
    }
}
