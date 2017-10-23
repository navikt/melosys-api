package no.nav.melosys.tjenester.gui;

import java.time.LocalDateTime;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;

public class FagsakRestTjenesteTest {

    @Autowired
    FagsakRepository fagsakRepo;

    FagsakRestTjeneste tjeneste;

    @Before
    public void setUp() throws JAXBException {
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());
        tjeneste = new FagsakRestTjeneste(fagsakRepo, dokumentFactory);
    }

    @Test
    public void hentFagsak() throws Exception {
        long saksnummer = 123;

        //Response response = tjeneste.hentFagsak(saksnummer);

    }

    @Test
    public void testMapping() throws Exception {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(123L);
        fagsak.setStatus(FagsakStatus.OPPRETTET);
        fagsak.setType(FagsakType.SØKNAD_A1);
        fagsak.setRegistrertDato(LocalDateTime.now());

        ModelMapper modelMapper = new ModelMapper();
        FagsakDto fagsakDto = new FagsakDto();
        modelMapper.map(fagsak, fagsakDto);
        modelMapper.validate();



    }





}