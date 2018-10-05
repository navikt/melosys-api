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

        String fakta = avklarteFaktaDto.getFakta().stream().collect(Collectors.joining(" "));
        avklartefakta.setFakta(fakta);

        oppdaterFaktaregistreringer(avklartefakta, avklarteFaktaDto);
    }

    private void oppdaterFaktaregistreringer(Avklartefakta avklartefakta, AvklartefaktaDto avklarteFaktaDto) {
        Set<AvklartefaktaRegistrering> registreringer = new HashSet<>();

        if (avklarteFaktaDto.harBegrunnelsefritekst()) {
            AvklartefaktaRegistrering registrering = lagFaktaregistrering();
            registrering.setAvklartefakta(avklartefakta);
            registrering.setBegrunnelseFritekst(avklarteFaktaDto.getBegrunnelsefritekst());

            registreringer.add(registrering);
            avklartefakta.oppdaterRegistreringer(registreringer);

            // Kun interessert i begrunnelseskoder eller kun fritekst
            return;
        }

        if (avklarteFaktaDto.harBegrunnelseKoder()) {
            for (String begrunnelse : avklarteFaktaDto.getBegrunnelsekoder()) {
                AvklartefaktaRegistrering registrering = lagFaktaregistrering();
                registrering.setBegrunnelseKode(begrunnelse);
                registrering.setAvklartefakta(avklartefakta);

                registreringer.add(registrering);
                avklartefakta.oppdaterRegistreringer(registreringer);
            }
        }
        avklartefakta.setRegistreringer(registreringer);
    }

    private AvklartefaktaRegistrering lagFaktaregistrering() {
        String ident = SubjectHandler.getInstance().getUserID();

        AvklartefaktaRegistrering avklartefaktaRegistrering = new AvklartefaktaRegistrering();
        avklartefaktaRegistrering.setRegistrertAv(ident);

        return avklartefaktaRegistrering;
    }

    public Set<AvklartefaktaDto> lagDtoFraAvklartefakta(Set<Avklartefakta> avklartefakta) {
        Set<AvklartefaktaDto> avklartefaktaDtoer = new HashSet<>();

        for (Avklartefakta a : avklartefakta) {
            avklartefaktaDtoer.add(new AvklartefaktaDto(a));
        }
        return avklartefaktaDtoer;
    }
}
