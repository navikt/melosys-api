package no.nav.melosys.service;

import no.nav.melosys.integrasjon.dokumentmottak.ForsendelsesinformasjonDto;
import no.nav.melosys.integrasjon.dokumentmottak.ProsessinstansMeldingsfordeler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

// FIXME: Brukes kun til test -- skal ikke ut i prod
@Profile("test")
@Service
public class DokMotQueueTestService {

    private ProsessinstansMeldingsfordeler meldingsfordeler;

    @Autowired
    public DokMotQueueTestService(ProsessinstansMeldingsfordeler meldingsfordeler) {
        this.meldingsfordeler = meldingsfordeler;
    }

    public void mottaTynnmeldingFraTestHub(ForsendelsesinformasjonDto forsendelsesinformasjonDto) {
        meldingsfordeler.execute(forsendelsesinformasjonDto);
    }

}
