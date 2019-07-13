/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2019 Tom Hallman
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For more information, visit https://github.com/tomhallman/mist
 */

package com.github.tomhallman.mist.model.data;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.util.Util;

import com.github.tomhallman.mist.MIST;
import com.github.tomhallman.mist.exceptions.EmailServerException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

/**
 * 
 */
public class GmailServer extends EmailServer {
    private static Logger log = LogManager.getLogger();

    public GmailServer(int id) {
        super(id, EmailServer.TYPE_GMAIL);
    }

    /**
     * @see https://javaee.github.io/javamail/OAuth2
     */
    @Override
    public void connect(boolean selectFolder) throws EmailServerException {
        log.trace("{{}} connect()", getNickname());
        log.debug("{{}} Connecting to Gmail...", getNickname());

        store = null;
        folder = null;
        currentMessageNumber = 0;
        totalMessages = 0;

        Properties props = new Properties();
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.auth.mechanisms", "XOAUTH2");
        Session sess = Session.getInstance(props, null);

        try {
            store = sess.getStore("imap");
        } catch (NoSuchProviderException e) {
            throw new EmailServerException(e);
        }

        String accessToken = null;
        try {
            accessToken = getOAuth2AccessToken();
        } catch (TokenResponseException e) {
            store = null;
            throw new EmailServerException(e.getDetails().getErrorDescription(), e);
        } catch (IOException | GeneralSecurityException e) {
            store = null;
            throw new EmailServerException(e);
        }

        try {
            store.connect("imap.gmail.com", getUsername(), accessToken);
        } catch (MessagingException e) {
            store = null;
            throw new EmailServerException(e);
        }

        if (selectFolder) {
            try {
                folder = store.getFolder(getFolder());
                folder.open(Folder.READ_ONLY);
                totalMessages = folder.getMessageCount();
            } catch (MessagingException e) {
                folder = null;
                disconnect();
                throw new EmailServerException(e);
            }
        }
    }

    private String getOAuth2AccessToken() throws IOException, GeneralSecurityException {
        log.trace("getOAuth2AccessToken()");

        //
        // Set up authorization code flow
        //

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
            jsonFactory,
            new InputStreamReader(EmailServer.class.getResourceAsStream("resources/google_client_secrets.json")));

        List<String> scopes = Arrays.asList("https://mail.google.com/"); // Needed for IMAP access

        // Create secure file store
        Path secureStoreDir = Paths.get(getSecureFilePath());

        if (Files.notExists(secureStoreDir)) {
            Files.createDirectory(secureStoreDir);

            // Set permissions so that only the current user can read the file
            if (Util.isMac() || Util.isLinux()) {
                Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(secureStoreDir, permissions);

            } else { // Windows; see https://stackoverflow.com/a/13892920/1307022
                AclFileAttributeView aclAttr = Files.getFileAttributeView(secureStoreDir, AclFileAttributeView.class);
                UserPrincipalLookupService upls = secureStoreDir.getFileSystem().getUserPrincipalLookupService();
                UserPrincipal user = upls.lookupPrincipalByName(System.getProperty("user.name"));
                AclEntry.Builder builder = AclEntry.newBuilder();
                builder.setPermissions(
                    EnumSet.of(
                        AclEntryPermission.APPEND_DATA,
                        AclEntryPermission.DELETE,
                        AclEntryPermission.DELETE_CHILD,
                        AclEntryPermission.EXECUTE,
                        AclEntryPermission.READ_ACL,
                        AclEntryPermission.READ_ATTRIBUTES,
                        AclEntryPermission.READ_DATA,
                        AclEntryPermission.READ_NAMED_ATTRS,
                        AclEntryPermission.SYNCHRONIZE,
                        AclEntryPermission.WRITE_ACL,
                        AclEntryPermission.WRITE_ATTRIBUTES,
                        AclEntryPermission.WRITE_DATA,
                        AclEntryPermission.WRITE_NAMED_ATTRS,
                        AclEntryPermission.WRITE_OWNER));
                builder.setPrincipal(user);
                builder.setType(AclEntryType.ALLOW);
                aclAttr.setAcl(Collections.singletonList(builder.build()));
            }
        }
        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(secureStoreDir.toFile());

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport,
            jsonFactory,
            clientSecrets,
            scopes).setDataStoreFactory(dataStoreFactory).build();

        // Authorize
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize(username);

        // Get the access token
        credential.refreshToken(); // Don't need to call this if it hasn't expired, but it doesn't hurt to do so
        return credential.getAccessToken();
    }

    private String getSecureFilePath() {
        return MIST.getUserDataDir() + ".secure_store";
    }

}
