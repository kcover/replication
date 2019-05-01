package com.connexta.replication.adapters.ddf.adaptor;

import com.connexta.replication.adapters.ddf.DdfNodeAdaptuer;
import com.connexta.replication.data.MetadataImpl;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.ElementSetNameType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetCapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.filter.v_1_1_0.PropertyNameType;
import net.opengis.filter.v_1_1_0.SortByType;
import net.opengis.filter.v_1_1_0.SortOrderType;
import net.opengis.filter.v_1_1_0.SortPropertyType;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.cxf.client.impl.SecureCxfClientFactoryImpl;
import org.codice.ditto.replication.api.data.CreateRequest;
import org.codice.ditto.replication.api.data.DeleteRequest;
import org.codice.ditto.replication.api.data.Metadata;
import org.codice.ditto.replication.api.data.UpdateRequest;
import org.codice.ditto.replication.api.impl.data.CreateRequestImpl;
import org.codice.ditto.replication.api.impl.data.DeleteRequestImpl;
import org.codice.ditto.replication.api.impl.data.UpdateRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleCsw {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCsw.class);

  private SecureCxfClientFactory<Csw> factory;

  protected CswJAXBElementProvider<GetRecordsType> getRecordsTypeProvider;

  protected List<String> jaxbElementClassNames = new ArrayList<>();
  protected Map<String, String> jaxbElementClassMap = new HashMap<>();

  private String endpointUrl = "https://ditto-1.phx.connexta.com:8993/services/csw";

  private String username = "admin";

  private String password = "admin";

  public SimpleCsw() {
    factory =
        new SecureCxfClientFactoryImpl<>(
            endpointUrl,
            Csw.class,
            initProviders(),
            null,
            true,
            false,
            3000,
            3000,
            username,
            password);
  }

  protected List<Object> initProviders() {
    getRecordsTypeProvider = new CswJAXBElementProvider<>();
    getRecordsTypeProvider.setMarshallAsJaxbElement(true);

    // Adding class names that need to be marshalled/unmarshalled to
    // jaxbElementClassNames list
    jaxbElementClassNames.add(GetRecordsType.class.getName());
    jaxbElementClassNames.add(CapabilitiesType.class.getName());
    jaxbElementClassNames.add(GetCapabilitiesType.class.getName());
    jaxbElementClassNames.add(GetRecordsResponseType.class.getName());
    jaxbElementClassNames.add(AcknowledgementType.class.getName());

    getRecordsTypeProvider.setJaxbElementClassNames(jaxbElementClassNames);

    // Adding map entry of <Class Name>,<Qualified Name> to jaxbElementClassMap
    String expandedName =
        new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.GET_RECORDS).toString();
    String message = "{} expanded name: {}";
    LOGGER.debug(message, CswConstants.GET_RECORDS, expandedName);
    jaxbElementClassMap.put(GetRecordsType.class.getName(), expandedName);

    String getCapsExpandedName =
        new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.GET_CAPABILITIES).toString();
    LOGGER.debug(message, CswConstants.GET_CAPABILITIES, expandedName);
    jaxbElementClassMap.put(GetCapabilitiesType.class.getName(), getCapsExpandedName);

    String capsExpandedName =
        new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.CAPABILITIES).toString();
    LOGGER.debug(message, CswConstants.CAPABILITIES, capsExpandedName);
    jaxbElementClassMap.put(CapabilitiesType.class.getName(), capsExpandedName);

    String caps201ExpandedName =
        new QName("http://www.opengis.net/cat/csw", CswConstants.CAPABILITIES).toString();
    LOGGER.debug(message, CswConstants.CAPABILITIES, caps201ExpandedName);
    jaxbElementClassMap.put(CapabilitiesType.class.getName(), caps201ExpandedName);

    String acknowledgmentName =
        new QName("http://www.opengis.net/cat/csw/2.0.2", "Acknowledgement").toString();
    jaxbElementClassMap.put(AcknowledgementType.class.getName(), acknowledgmentName);
    getRecordsTypeProvider.setJaxbElementClassMap(jaxbElementClassMap);

    GetRecordsMessageBodyReader grmbr = new GetRecordsMessageBodyReader();

    return Arrays.asList(getRecordsTypeProvider, new CswResponseExceptionMapper(), grmbr);
  }

  public boolean isAvailable() {
    Csw csw = factory.getClient();
    GetCapabilitiesType getCapabilitiesType = new GetCapabilitiesType();
    getCapabilitiesType.setService("CSW");
    try {
      CapabilitiesType response = csw.getCapabilities(getCapabilitiesType);
      LOGGER.info(
          "Successfully contacted CSW server version {} at {}", response.getVersion(), endpointUrl);
    } catch (Exception e) {
      LOGGER.warn("Error contacting CSW Server at {}", endpointUrl, e);
      return false;
    }
    return true;
  }

  public List<Metadata> query(String cql, long startIndex, long pageSize) throws Exception {
    Csw csw = factory.getClient();

    GetRecordsType getRecordsType = new GetRecordsType();
    getRecordsType.setVersion(CswConstants.VERSION_2_0_2);
    getRecordsType.setService("CSW");
    getRecordsType.setResultType(ResultType.RESULTS);
    getRecordsType.setStartPosition(BigInteger.valueOf(startIndex));
    getRecordsType.setMaxRecords(BigInteger.valueOf(pageSize));
    getRecordsType.setOutputFormat(MediaType.APPLICATION_XML);
    getRecordsType.setOutputSchema("urn:catalog:metacard");
    QueryType queryType = new QueryType();
    queryType.setTypeNames(
        Arrays.asList(new QName("http://www.opengis.net/cat/csw/2.0.2", "Record", "csw")));

    ElementSetNameType elementSetNameType = new ElementSetNameType();
    elementSetNameType.setValue(ElementSetType.FULL);
    queryType.setElementSetName(elementSetNameType);
    SortByType cswSortBy = new SortByType();
    SortPropertyType sortProperty = new SortPropertyType();
    PropertyNameType propertyName = new PropertyNameType();
    propertyName.setContent(Arrays.asList((Object) "metacard.modified"));
    sortProperty.setPropertyName(propertyName);
    sortProperty.setSortOrder(SortOrderType.ASC);
    cswSortBy.getSortProperty().add(sortProperty);
    queryType.setSortBy(cswSortBy);
    QueryConstraintType queryConstraintType = new QueryConstraintType();
    queryConstraintType.setVersion(CswConstants.CONSTRAINT_VERSION);
    queryConstraintType.setCqlText(cql);
    queryType.setConstraint(queryConstraintType);
    ObjectFactory objectFactory = new ObjectFactory();
    getRecordsType.setAbstractQuery(objectFactory.createQuery(queryType));

    CswRecordCollection response = csw.getRecords(getRecordsType);
    return response.getCswRecords();
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("ddf.home", "C:\\DIBs\\dib-4.6.6");
    System.setProperty("org.codice.ddf.system.hostname", "localhost");
    System.setProperty(
        "javax.net.ssl.keyStore", "C:\\DIBs\\dib-4.6.6\\etc\\keystores\\serverKeystore.jks");
    System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
    System.setProperty(
        "javax.net.ssl.trustStore", "C:\\DIBs\\dib-4.6.6\\etc\\keystores\\serverTruststore.jks");
    System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    System.setProperty("javax.net.ssl.keyStoreType", "jks");
    System.setProperty("javax.net.ssl.trustStoreType", "jks");
    System.setProperty(
        "https.cipherSuites",
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
    System.setProperty("https.protocols", "TLSv1.1,TLSv1.2");
    System.setProperty("jdk.tls.client.protocols", "TLSv1.1,TLSv1.2");
    System.setProperty("jdk.tls.ephemeralDHKeySize", "matched");
    delete();
  }

  private static void create() throws Exception {
    DdfNodeAdaptuer adapter = new DdfNodeAdaptuer();
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    String string = "1987-03-17T06:29:52.010+00:00";
    Date result = df.parse(string);
    Metadata metadata =
        new MetadataImpl(rawMetadata, Metadata.class, "000e1520884b4affb844bd76bc74470e", result);
    CreateRequest createRequest = new CreateRequestImpl(Collections.singletonList(metadata));
    adapter.createRequest(createRequest);
  }

  private static void update() throws Exception {
    DdfNodeAdaptuer adapter = new DdfNodeAdaptuer();
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    String string = "1987-03-17T06:29:52.010+00:00";
    Date result = df.parse(string);
    Metadata metadata =
        new MetadataImpl(rawMetadata, Metadata.class, "000e1520884b4affb844bd76bc74470e", result);
    UpdateRequest updateRequest = new UpdateRequestImpl(Collections.singletonList(metadata));
    adapter.updateRequest(updateRequest);
  }

  private static void delete() throws Exception {
    DdfNodeAdaptuer adapter = new DdfNodeAdaptuer();
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    String string = "1987-03-17T06:29:52.010+00:00";
    Date result = df.parse(string);
    Metadata metadata =
        new MetadataImpl(rawMetadata, Metadata.class, "000e1520884b4affb844bd76bc74470e", result);
    DeleteRequest deleteRequest = new DeleteRequestImpl(Collections.singletonList(metadata));
    adapter.deleteRequest(deleteRequest);
  }

  private static String rawMetadata =
      "<metacard xmlns=\"urn:catalog:metacard\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:smil=\"http://www.w3.org/2001/SMIL20/\" xmlns:smillang=\"http://www.w3.org/2001/SMIL20/Language\" gml:id=\"000e1520884b4affb844bd76bc74470e\">\n"
          + "  <type>ddf.metacard</type>\n"
          + "  <source>ddf.distribution</source>\n"
          + "  <string name=\"title\">\n"
          + "    <value>DOLLAR SEEN UPDATED</value>\n"
          + "  </string>\n"
          + "  <dateTime name=\"created\">\n"
          + "    <value>1987-03-17T06:29:52.010+00:00</value>\n"
          + "  </dateTime><ns3:geometry xmlns:ns1=\"http://www.opengis.net/gml\" xmlns:ns2=\"http://www.w3.org/1999/xlink\" xmlns:ns3=\"urn:catalog:metacard\" xmlns:ns4=\"http://www.w3.org/2001/SMIL20/\" xmlns:ns5=\"http://www.w3.org/2001/SMIL20/Language\" name=\"location\">\n"
          + "  <ns3:value>\n"
          + "    <ns1:Point>\n"
          + "      <ns1:pos>139.69171 35.6895</ns1:pos>\n"
          + "    </ns1:Point>\n"
          + "  </ns3:value>\n"
          + "</ns3:geometry><stringxml name=\"metadata\">\n"
          + "    <value><REUTERS CGISPLIT=\"TRAINING-SET\" LEWISSPLIT=\"TRAIN\" NEWID=\"5778\" OLDID=\"10691\" TOPICS=\"YES\">\n"
          + "<DATE>16-MAR-1987 23:29:52.10</DATE>\n"
          + "<TOPICS><D>money-fx</D><D>dlr</D></TOPICS>\n"
          + "<PLACES><D>usa</D><D>japan</D></PLACES>\n"
          + "<PEOPLE/>\n"
          + "<ORGS/>\n"
          + "<EXCHANGES/>\n"
          + "<COMPANIES/>\n"
          + "<UNKNOWN> \n"
          + "RM\n"
          + "f4012reute\n"
          + "u f BC-DOLLAR-SEEN-FALLING-U   03-16 0109</UNKNOWN>\n"
          + "<TEXT>\n"
          + "<TITLE>DOLLAR SEEN FALLING UNLESS JAPAN SPURS ECONOMY</TITLE>\n"
          + "<AUTHOR>    By Rie Sagawa, Reuters</AUTHOR>\n"
          + "<DATELINE>    TOKYO, March 17 - </DATELINE><BODY>Underlying dollar sentiment is bearish,\n"
          + "and operators may push the currency to a new low unless Japan\n"
          + "takes steps to stimulate its economy as pledged in the Paris\n"
          + "accord, foreign exchange analysts polled by Reuters said here.\n"
          + "    \"The dollar is expected to try its psychological barrier of\n"
          + "150.00 yen and to fall even below that level,\" a senior dealer\n"
          + "at one leading bank said.\n"
          + "    The dollar has eased this week, but remains stable at\n"
          + "around 151.50 yen. Six major industrial countries agreed at a\n"
          + "meeting in Paris in February to foster currency stability.\n"
          + "    Some dealers said the dollar may decline in the long term,\n"
          + "but a drastic fall is unlikely because of U.S. Fears of renewed\n"
          + "inflation and fears of reduced Japanese purchases of U.S.\n"
          + "Treasury securities, needed to finance the U.S. Deficit.\n"
          + "    Dealers generally doubted whether any economic package\n"
          + "Japan could adopt soon would be effective enough to reduce its\n"
          + "trade surplus significantly, and said such measures would\n"
          + "probably invite further U.S. Steps to weaken the dollar.\n"
          + "    Under the Paris accord, Tokyo promised a package of\n"
          + "measures after the fiscal 1987 budget was passed to boost\n"
          + "domestic demand, increase imports and cut its trade surplus.\n"
          + "    But debate on the budget has been delayed by an opposition\n"
          + "boycott of Parliamentary business over the proposed imposition\n"
          + "of a five pct sales tax, and the government has only a slim\n"
          + "chance of producing a meaningful economic package in the near\n"
          + "future, the dealers said.\n"
          + "    If no such steps are taken, protectionist sentiment in the\n"
          + "U.S. Congress will grow, putting greater downward pressure on\n"
          + "the dollar, they said.\n"
          + "    The factors affecting the U.S. Currency have not changed\n"
          + "since before the Paris accord, they added.\n"
          + "    \"Underlying sentiment for the dollar remains bearish due to\n"
          + "a still-sluggish U.S. Economic outlook, the international debt\n"
          + "crisis triggered by Brazil's unilateral suspension of interest\n"
          + "payments on its foreign debts and the reduced clout of the\n"
          + "Reagan administration as a result of the Iran/Contra arms\n"
          + "scandal,\" said a senior dealer at a leading trust bank.\n"
          + "    \"There is a possibility that the dollar may decline to\n"
          + "around 140.00 yen by the end of this year,\" said Chemical Bank\n"
          + "Tokyo branch vice president Yukuo Takahashi.\n"
          + "    But operators find it hard to push the dollar either way\n"
          + "for fear of possible concerted central bank intervention.\n"
          + "    Dealers said there were widespread rumours that the U.S.\n"
          + "Federal Reserve telephoned some banks in New York to ask for\n"
          + "quotes last Wednesday, and even intervened to sell the dollar\n"
          + "when it rose to 1.87 marks.\n"
          + "    The Bank of England also apparently sold sterling in London\n"
          + "when it neared 1.60 dlrs on Wednesday, they said.\n"
          + "    But other dealers said they doubted the efficacy of central\n"
          + "bank intervention, saying it may stimulate the dollar's decline\n"
          + "because many dealers are likely to await such dollar buying\n"
          + "intervention as a chance to sell dollars.\n"
          + "    However, First National Bank of Chicago Tokyo Branch\n"
          + "assistant manager Hiroshi Mochizuki said \"The dollar will not\n"
          + "show drastic movement at least to the end of March.\"\n"
          + "    Other dealers said the U.S. Seems unwilling to see any\n"
          + "strong dollar swing until Japanese companies close their books\n"
          + "for the fiscal year ending on March 31, because a weak dollar\n"
          + "would give Japanese institutional investors paper losses on\n"
          + "their foreign holdings, which could make them lose interest in\n"
          + "purchases of U.S. Treasury securities.\n"
          + "    U.S. Monetary officials may refrain from making any\n"
          + "comments this month to avoid influencing rates, they said.\n"
          + " REUTER\n"
          + "</BODY></TEXT>\n"
          + "</REUTERS></value>\n"
          + "  </stringxml>\n"
          + "</metacard>";
}
