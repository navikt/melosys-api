package no.nav.melosys.integrasjon.ereg;

import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class EregSystemService extends EregService implements EregFasade {

    @Autowired
    public EregSystemService(@Qualifier("system")OrganisasjonConsumer organisasjonConsumer, DokumentFactory dokumentFactory) {
        super(organisasjonConsumer, dokumentFactory);
    }
}
