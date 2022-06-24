package com.something.oxalis.plugin;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import network.oxalis.api.evidence.EvidenceFactory;
import network.oxalis.api.persist.ReceiptPersister;
import network.oxalis.vefa.peppol.common.model.Header;
import network.oxalis.api.inbound.InboundMetadata;
import network.oxalis.api.lang.EvidenceException;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;





@Singleton

public class ExampleReceiptPersister implements ReceiptPersister {


    private final EvidenceFactory evidenceFactory;

    private final Path inboundFolder;



    @Inject

    public ExampleReceiptPersister( @Named("inbound") Path inboundFolder, EvidenceFactory evidenceFactory) {

        this.inboundFolder = inboundFolder;
        this.evidenceFactory = evidenceFactory;
    }

    private String returnCN (String theSubjectDN) {
        LdapName ln = null;
        String result="";
        try {
            ln = new LdapName(theSubjectDN);
        } catch (InvalidNameException e) {
            throw new RuntimeException(e);
        }

        for (Rdn rdn : ln.getRdns()) {
            if (rdn.getType().equalsIgnoreCase("CN")) {
                result=rdn.getValue().toString();
                break;
            }
        }
     return result;
    }

    private String returnCC (String theSubjectDN){
        LdapName ln = null;
        String result="";
        try {
            ln = new LdapName(theSubjectDN);
        } catch (InvalidNameException e) {
            throw new RuntimeException(e);
        }

        for (Rdn rdn : ln.getRdns()) {
            if (rdn.getType().equalsIgnoreCase("C")) {
                result=rdn.getValue().toString();
                break;
            }
        }
      return result;
    }

    private String getHostName() {
        String result = "Unknow";
        try  {
            result= System.getenv("HOSTNAME");
        } catch (Exception e) {
           e.printStackTrace();
        }


       return result;


    }
    @Override
    public void persist(InboundMetadata inboundMetadata, Path payloadPath) throws IOException {
        Path path = createArtifactFolders(inboundFolder, inboundMetadata.getHeader()).resolve(
                String.format("%s.receipt.dat",
                        filterString(inboundMetadata.getTransmissionIdentifier().getIdentifier())));

        String nameId = returnCN(inboundMetadata.getCertificate().getSubjectX500Principal().getName());

        String country=returnCC(inboundMetadata.getCertificate().getSubjectX500Principal().getName());



        String theFolder="/tmp/oxalis_receipt_files";
        path = Paths.get(theFolder+"/" + path.getFileName());

        try (OutputStream outputStream = Files.newOutputStream(path)) {
            evidenceFactory.write(outputStream, inboundMetadata);
        } catch (EvidenceException e) {
            throw new IOException("Unable to persist receipt.", e);
        }
        ReceiptParser parser = new ReceiptParser();
        parser.executeParsing(path.toString(), getHostName(),nameId,country);

    }
    public static Path createArtifactFolders(Path baseFolder, Header header) throws IOException {
        Path folder = baseFolder.resolve(Paths.get(
                filterString(header.getReceiver().getIdentifier()),
                filterString(header.getSender().getIdentifier())));

        Files.createDirectories(folder);

        return folder;
    }
    public static String filterString(String s) {
        return s.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }




}