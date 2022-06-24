package com.something.oxalis.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


    public class ReceiptParser {
        String receiverId;
        String senderId;
        String MessageIdentifierByREMMD;
        String server;
        String messageSubject;
        String protocol;


        private static String yourCN="PLNXXXXXX";

        private static String yourCC="NL";

        public String getIncomingCN() {
            return incomingCN;
        }

        public void setIncomingCN(String incomingCN) {
            this.incomingCN = incomingCN;
        }

        public String getIncomingCC() {
            return incomingCC;
        }

        public void setIncomingCC(String incomingCC) {
            this.incomingCC = incomingCC;
        }

        String incomingCN;
        String incomingCC;
        public ReceiptParser() {
            super();
        }

        public void executeParsing(String fileName, String serverName,String incomingCN, String incomingCC) {


            setIncomingCC(incomingCC);
            setIncomingCN(incomingCN);

            try {
                processTheDirectory(fileName, serverName);
            } catch (SQLException e) {
                e.printStackTrace();
            }

        };

        private void processTheDirectory(String dir, String server) throws SQLException {

            Connection theConnection = this.createConnection();

            File file = new File(dir);


                    try {

                        parseTheXml(dir, server, theConnection);

                    } catch (IOException | ParserConfigurationException | SAXException e) {
                        e.printStackTrace();

                        return;

                    }
                    Path temp = null;

                    try {

                        Files.copy(Paths.get(file.getAbsoluteFile().toString()),
                                Paths.get(file.getParent() + "/processed/" + file.getName()), StandardCopyOption.REPLACE_EXISTING);

                        Files.delete(Paths.get(file.getAbsoluteFile().toString()));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }







            this.closeConnection(theConnection);


        }

        private void parseTheXml(String theFileName, String server, Connection conn2) throws ParserConfigurationException,
                SAXException, IOException,
                SAXException {

            Document doc;
            doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse("file:///" + theFileName);
            doc.normalize();
            Element emRecipient = (Element) doc.getElementsByTagName("RecipientsDetails").item(0);
            Element emSender = (Element) doc.getElementsByTagName("SenderDetails").item(0);
            setReceiverId(emRecipient.getElementsByTagName("AttributedElectronicAddress")
                    .item(0)
                    .getTextContent());
            setSenderId(emSender.getElementsByTagName("AttributedElectronicAddress")
                    .item(0)
                    .getTextContent());

            Element emMessageDetails = (Element) doc.getElementsByTagName("SenderMessageDetails").item(0);
            this.setMessageIdentifierByREMMD(emMessageDetails.getElementsByTagName("MessageIdentifierByREMMD")
                    .item(0)
                    .getTextContent());
            this.setMessageSubject(emMessageDetails.getElementsByTagName("MessageSubject")
                    .item(0)
                    .getTextContent());


            String eventTime = doc.getElementsByTagName("EventTime")
                    .item(0)
                    .getTextContent();
            eventTime = eventTime.replace("T", " ");
            eventTime = eventTime.replace("Z", " ");
            java.util.Date utilDate = null;
            try {
                utilDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(eventTime);
            } catch (ParseException e) {
            }
            java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());


            NodeList nList = doc.getElementsByTagName("Extensions");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                {
                    Element eElement = (Element) nNode;
                    NodeList elementList = eElement.getElementsByTagName("Extension");

                    for (int count = 0; count < elementList.getLength(); count++) {
                        Node node1 = elementList.item(count);

                        if (node1.getNodeType() == node1.ELEMENT_NODE) {
                            Element tp = (Element) node1;
                            Node nn = tp.getFirstChild();
                            Node nn2 = nn.getFirstChild();

                            this.setProtocol(nn2.getTextContent());
                        }
                    }


                }
            }


            try {
                insertIntoDatabase(this.getSenderId(), this.getReceiverId(), this.getMessageIdentifierByREMMD(), server,
                        sqlDate, this.getMessageSubject(), this.getProtocol(), theFileName, conn2);
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }


        }

        private Connection createConnection() throws SQLException {

            Connection con;

            con = DriverManager.getConnection("jdbc:mysql://XXXXX:3306/example_oxalis_logging", "oxalis_logging_user", "XXXXX");

            return con;
        }

        private void insertIntoDatabase(String sender, String receiver, String value, String server, java.sql.Date sqldate,
                                        String Subject, String protocol, String filename,
                                        Connection conn) throws ClassNotFoundException, SQLException {



            String query =
                    " insert into OXALIS_LOGGING (server,senderid,receiverid,message_identifier,event_time,message_subject,protocol,filename,flow,receiverCn,receiverCc,senderCn,senderCc)" +
                            " values (?,?,?,?,?,?,?,?,'INCOMING',?,?,?,?)";


            PreparedStatement preparedStmt = conn.prepareStatement(query);
            preparedStmt.setString(1, server);
            preparedStmt.setString(2, sender.replace(":", "_"));
            preparedStmt.setString(3, receiver.replace(":", "_"));
            preparedStmt.setString(4, value);

            java.sql.Timestamp timestamp = new java.sql.Timestamp(sqldate.getTime());

            preparedStmt.setTimestamp(5, (timestamp));
            preparedStmt.setString(6, Subject);
            preparedStmt.setString(7, protocol);
            preparedStmt.setString(8, filename);
            preparedStmt.setString(9, yourCN);
            preparedStmt.setString(10, yourCC);
            preparedStmt.setString(11, getIncomingCN());
            preparedStmt.setString(12, getIncomingCC());
            preparedStmt.execute();


        }

        private void closeConnection(Connection conn) throws SQLException {

            conn.close();
        }

        public void setReceiverId(String receiverId) {
            this.receiverId = receiverId;
        }

        public String getReceiverId() {
            return receiverId;
        }

        public void setSenderId(String senderId) {
            this.senderId = senderId;
        }

        public String getSenderId() {
            return senderId;
        }

        public void setMessageIdentifierByREMMD(String MessageIdentifierByREMMD) {
            this.MessageIdentifierByREMMD = MessageIdentifierByREMMD;
        }

        public String getMessageIdentifierByREMMD() {
            return MessageIdentifierByREMMD;
        }

        public void setServer(String server) {
            this.server = server;
        }

        public String getServer() {
            return server;
        }

        public void setMessageSubject(String messageSubject) {
            this.messageSubject = messageSubject;
        }

        public String getMessageSubject() {
            return messageSubject;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getProtocol() {
            return protocol;
        }


    }


