package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class DokSysSystemService extends DokSysService implements DokSysFasade {

    @Autowired
    DokSysSystemService(@Qualifier("system") DokumentproduksjonConsumer dokumentproduksjonConsumer) {
        super(dokumentproduksjonConsumer);
    }
}
