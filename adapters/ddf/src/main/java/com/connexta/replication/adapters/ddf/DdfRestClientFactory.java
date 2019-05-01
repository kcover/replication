/**
 * Copyright (c) Connexta
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package com.connexta.replication.adapters.ddf;

import com.google.common.annotations.VisibleForTesting;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.endpoints.rest.RESTService;
import org.codice.ddf.security.common.Security;
import org.codice.ditto.replication.api.data.Metadata;
import org.codice.ditto.replication.api.data.Resource;

/** Factory for creating a basic client capable of posting resources to the DDF REST endpoint. */
public class DdfRestClientFactory {

  private static final String CONTENT_DISPOSITION = "Content-Disposition";

  private static final String DEFAULT_REST_ENDPOINT = "/catalog";

  private final ClientFactoryFactory clientFactoryFactory;

  private final Security security;

  public DdfRestClientFactory(ClientFactoryFactory clientFactoryFactory) {
    this(clientFactoryFactory, Security.getInstance());
  }

  @VisibleForTesting
  public DdfRestClientFactory(ClientFactoryFactory clientFactoryFactory, Security security) {
    this.clientFactoryFactory = clientFactoryFactory;
    this.security = security;
  }

  /**
   * Creates a limited functionality wrapper around a new {@link WebClient} created from a given
   * host name. The created client can be used for multiple requests.
   *
   * @param host the host for the client to connect to
   * @return a wrapped {@link WebClient}
   */
  DdfRestClient create(String host) {
    final SecureCxfClientFactory<RESTService> restClientFactory =
        clientFactoryFactory.getSecureCxfClientFactory(
            host + DEFAULT_REST_ENDPOINT, RESTService.class);

    WebClient webClient =
        security.runAsAdmin(
            () ->
                AccessController.doPrivilegedWithCombiner(
                    (PrivilegedAction<WebClient>)
                        () ->
                            restClientFactory.getWebClientForSubject(security.getSystemSubject())));

    webClient.type(MediaType.APPLICATION_XML);
    webClient.accept(MediaType.APPLICATION_JSON);

    return new DdfRestClient(webClient);
  }

  // TODO: I don't think we've every used one of these clients for more than one request. Can we use
  // them that way as our code suggests? (DdfNodeAdapter)
  /** A wrapper around the {@link WebClient}. */
  class DdfRestClient {

    private final WebClient webClient;

    DdfRestClient(WebClient webClient) {
      this.webClient = webClient;
    }

    /**
     * Post new metadata to the DDF REST endpoint.
     *
     * @param metadata the {@link Metadata} to post
     * @return the response
     */
    Response post(Metadata metadata) {
      String rawMetadata = (String) metadata.getRawMetadata();
      return webClient.post(rawMetadata);
    }

    /**
     * Put updated metadata on the DDF REST endpoint.
     *
     * @param metadata the {@link Metadata} to put
     * @return the response
     */
    Response put(Metadata metadata) {
      webClient.path(metadata.getId());
      String rawMetadata = (String) metadata.getRawMetadata();
      Response response = webClient.put(rawMetadata);
      webClient.back(true); // reset path
      return response;
    }

    /**
     * Post new content to the DDF REST endpoint.
     *
     * @param resource the {@link Resource} to post
     * @return the response
     */
    Response post(Resource resource) {
      try {
        MultipartBody multipartBody = createBody(resource);
        webClient.type(MediaType.MULTIPART_FORM_DATA);
        Response response = webClient.post(multipartBody);
        webClient.type(MediaType.APPLICATION_XML); // return content-type to xml
        return response;
      } catch (IOException e) {
        return null;
      }
    }

    /**
     * Put updated content on the DDF REST endpoint.
     *
     * @param resource the updated {@link Resource}
     * @return the response
     */
    Response put(Resource resource) {
      try {
        webClient.path(resource.getId());
        MultipartBody multipartBody = createBody(resource);
        Response response = webClient.put(multipartBody);
        webClient.back(true);
        return response;
      } catch (IOException e) {
        return null;
      }
    }

    Response delete(String id) {
      webClient.path(id);
      Response response = webClient.delete();
      webClient.back(true);
      return response;
    }

    private MultipartBody createBody(Resource resource) throws IOException {
      List<Attachment> attachments = new ArrayList<>();
      attachments.add(createParseResourceAttachment(resource));
      attachments.add(createMetadataAttachment(resource));
      return new MultipartBody(attachments);
    }

    private Attachment createParseResourceAttachment(Resource resource) throws IOException {
      ContentDisposition contentDisposition = createResourceContentDisposition(resource.getName());

      MultivaluedMap<String, String> headers = new MetadataMap<>(false, true);
      headers.putSingle(CONTENT_DISPOSITION, contentDisposition.toString());
      headers.putSingle("Content-ID", "parse.resource");
      headers.putSingle("Content-Type", resource.getMimeType());

      return new Attachment(resource.getInputStream(), headers);
    }

    private Attachment createMetadataAttachment(Resource resource) {
      ContentDisposition contentDisposition = createMetadataContentDisposition(resource.getId());

      InputStream bais =
          new ByteArrayInputStream(
              ((String) resource.getMetadata().getRawMetadata()).getBytes(StandardCharsets.UTF_8));

      return new Attachment("parse.metadata", bais, contentDisposition);
    }

    private ContentDisposition createResourceContentDisposition(String resourceFileName) {
      String contentDisposition =
          String.format("form-data; name=parse.resource; filename=%s", resourceFileName);
      return new ContentDisposition(contentDisposition);
    }

    private ContentDisposition createMetadataContentDisposition(String metadataFilename) {
      String contentDisposition =
          String.format("form-data; name=parse.metadata; filename=%s", metadataFilename);
      return new ContentDisposition(contentDisposition);
    }
  }
}
