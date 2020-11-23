package db.local.testdata;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@SuppressWarnings("unused")
public class V7_1_07__LAG_INTERN_XML extends BaseJavaMigration {

    private final Jaxb2Marshaller jaxb2Marshaller;
    private final TransformerFactory transformerFactory;
    private final Map<String, Transformer> transformerMap = new HashMap<>();

    public V7_1_07__LAG_INTERN_XML() {
        jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setPackagesToScan("no.nav.melosys.domain.dokument");
        jaxb2Marshaller.setValidationEventHandler(new DefaultValidationEventHandler());

        this.transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    }

    @Override
    public void migrate(Context context) throws Exception {
        OracleConnection con = context.getConnection().unwrap(OracleConnection.class);
        con.setAutoCommit(false);
        OracleResultSet resultSet = null;
        try (Statement statement = con.createStatement()) {
            statement.setFetchSize(50);
            resultSet = (OracleResultSet) statement.executeQuery("SELECT * FROM SAKSOPPLYSNING");
            while (resultSet.next()) {
                opprettMigrering(resultSet, con);
            }
            con.commit();
        } finally {
            if (resultSet != null) resultSet.close();
        }
    }

    private void opprettMigrering(OracleResultSet resultSet, OracleConnection con) throws SQLException, TransformerException, JsonProcessingException {
        long saksopplysningID = resultSet.getLong("id");
        String versjon = resultSet.getString("versjon");
        String opplysningType = resultSet.getString("opplysning_type");
        String xmlString = resultSet.getString("dokument_xml");
        String internXml = null;

        switch (opplysningType) {
            case "ARBFORH":
                internXml = transformer(xmlString,
                    "aareg/arbeidsforhold_"+ versjon + ".xslt", versjon);
                break;
            case "INNTK":
                internXml = transformer(xmlString, "inntk/inntekt_"+ versjon + ".xslt", versjon);
                break;
            case "ORG":
                internXml = transformer(xmlString, "ereg/organisasjon_"+ versjon + ".xslt", versjon);
                break;
            case "MEDL":
                internXml = transformer(xmlString, "medl/medlemskap_"+ versjon + ".xslt", versjon);
                break;
            case "PERSHIST":
                internXml = transformer(xmlString, "tps/personhistorikk_"+ versjon + ".xslt", versjon);
                break;
            case "PERSOPL":
                internXml = transformer(xmlString, "tps/person_"+ versjon + ".xslt", versjon);
                break;
            case "SOB_SAK":
                internXml = transformer(xmlString, "sob/sakogbehandling_"+ versjon + ".xslt", versjon);
                break;
            case "UTBETAL":
                internXml = transformer(xmlString, "utbetaling/utbetaldata_"+ versjon + ".xslt", versjon);
                break;
            default:
                throw new RuntimeException("Mapping fra saksopplysning type " + opplysningType + " ikke støttet");
        }
        if (internXml != null) {
            oppdaterSaksopplysning(con, saksopplysningID, internXml);
        }
    }

    private String transformer(String xml, String path, String versjon) throws TransformerException {
        Transformer transformer;
        if (transformerMap.containsKey(path)) {
            transformer = transformerMap.get(path);
        } else {
            transformer = lagTransformer(path, versjon);
            transformerMap.put(path, transformer);
        }

        StreamResult outputTarget = new StreamResult(new StringWriter());
        StreamSource xmlSource = new StreamSource(new StringReader(xml));
        transformer.transform(xmlSource, outputTarget);
        return outputTarget.getWriter().toString();
    }

    private Transformer lagTransformer(String path, String versjon) throws TransformerConfigurationException {
        Transformer transformer = lagTemplate(path, versjon).newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        return transformer;
    }

    private Templates lagTemplate(String path, String versjon) throws TransformerConfigurationException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        return transformerFactory.newTemplates(new StreamSource(is));
    }

    private void oppdaterSaksopplysning(OracleConnection con, long saksopplysningID, String internXml) throws SQLException {
        try (OraclePreparedStatement ps = (OraclePreparedStatement) con.prepareStatement(
            "UPDATE SAKSOPPLYSNING SET INTERN_XML = XMLType(?) WHERE id = ?")) {

            ps.setCharacterStream(1, new StringReader(internXml));
            ps.setLong(2, saksopplysningID);

            ps.execute();
        }
    }

    @Override
    public Integer getChecksum() {
        return 835_626_596;
    }
}