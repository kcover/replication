package com.connexta.replication.adapters.ddf;

import static com.connexta.replication.adapters.ddf.CqlBuilder.after;
import static com.connexta.replication.adapters.ddf.CqlBuilder.allOf;
import static com.connexta.replication.adapters.ddf.CqlBuilder.anyOf;
import static com.connexta.replication.adapters.ddf.CqlBuilder.equalTo;
import static com.connexta.replication.adapters.ddf.CqlBuilder.like;
import static com.connexta.replication.adapters.ddf.CqlBuilder.negate;

import com.connexta.replication.adapters.ddf.DdfRestClientFactory.DdfRestClient;
import com.connexta.replication.adapters.ddf.adaptor.SimpleCsw;
import com.connexta.replication.data.QueryResponseImpl;
import ddf.catalog.core.versioning.MetacardVersion;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import org.codice.ddf.cxf.client.impl.ClientFactoryFactoryImpl;
import org.codice.ditto.replication.api.AdapterException;
import org.codice.ditto.replication.api.NodeAdapter;
import org.codice.ditto.replication.api.Replication;
import org.codice.ditto.replication.api.data.CreateRequest;
import org.codice.ditto.replication.api.data.CreateStorageRequest;
import org.codice.ditto.replication.api.data.DeleteRequest;
import org.codice.ditto.replication.api.data.Metadata;
import org.codice.ditto.replication.api.data.QueryRequest;
import org.codice.ditto.replication.api.data.QueryResponse;
import org.codice.ditto.replication.api.data.Resource;
import org.codice.ditto.replication.api.data.ResourceRequest;
import org.codice.ditto.replication.api.data.ResourceResponse;
import org.codice.ditto.replication.api.data.UpdateRequest;
import org.codice.ditto.replication.api.data.UpdateStorageRequest;

public class DdfNodeAdaptuer extends SimpleCsw implements NodeAdapter {

  private static final String REGISTRY_TAG = "registry";

  private static final String REGISTRY_IDENTITY_NODE = "registry.local.registry-identity-node";

  private final DdfRestClientFactory ddfRestClientFactory;

  private final URL hostUrl;

  private String cachedSystemName;

  // TODO: This is for initial testing. Remember to remove it later.
  public DdfNodeAdaptuer() throws Exception {
    this.hostUrl = new URL("https://ditto-1.phx.connexta.com:8993/services");
    this.ddfRestClientFactory = new DdfRestClientFactory(new ClientFactoryFactoryImpl());
  }

  public DdfNodeAdaptuer(DdfRestClientFactory ddfRestClientFactory, URL hostUrl) {
    this.ddfRestClientFactory = ddfRestClientFactory;
    this.hostUrl = hostUrl;
  }

  @Override
  public String getSystemName() {
    if (this.cachedSystemName != null) {
      return this.cachedSystemName;
    }
    String filter =
        String.format(
            "[[ \"%s\" = '%s' ] AND [ NOT [ \"%s\" IS NULL ] ]]",
            Metacard.TAGS, REGISTRY_TAG, REGISTRY_IDENTITY_NODE);
    List<Metadata> results;

    try {
      results = query(filter, 1, 100);
    } catch (Exception e) {
      throw new AdapterException("Failed to retrieve remote system name", e);
    }

    String systemName;
    if (!results.isEmpty()) {
      systemName = (String) results.get(0).getMap().get("title");
    } else {
      throw new AdapterException(
          "No registry metadata available on remote node. Could not retrieve remote system name");
    }

    if (systemName == null) {
      throw new AdapterException("Could not get remote name from registry metacard");
    } else {
      this.cachedSystemName = systemName;
      return this.cachedSystemName;
    }
  }

  @Override
  public QueryResponse query(QueryRequest queryRequest) {
    final String cql = queryRequest.getCql();
    final List<String> filters = new ArrayList<>();

    for (String excludedNode : queryRequest.getExcludedNodes()) {
      filters.add(negate(equalTo(Replication.ORIGINS, excludedNode)));
    }
    filters.add(equalTo(Core.METACARD_TAGS, Metacard.DEFAULT_TAG));

    final List<String> failedItemFilters = new ArrayList<>();
    for (String itemId : queryRequest.getFailedItemIds()) {
      failedItemFilters.add(equalTo(Core.ID, itemId));
    }

    String finalFilter;

    Date modifiedAfter = queryRequest.getModifiedAfter();
    List<String> deletedFilters = new ArrayList<>();
    if (modifiedAfter != null) {
      filters.add(after(Core.METACARD_MODIFIED, modifiedAfter));

      deletedFilters.add(after(MetacardVersion.VERSIONED_ON, modifiedAfter));
      deletedFilters.add(equalTo(Core.METACARD_TAGS, MetacardVersion.VERSION_TAG));
      deletedFilters.add(like(MetacardVersion.ACTION, "Deleted*"));

      finalFilter = allOf(cql, anyOf(allOf(filters), allOf(deletedFilters)));
    } else {
      filters.add(cql);
      finalFilter = allOf(filters);
    }

    if (!failedItemFilters.isEmpty()) {
      finalFilter = anyOf(finalFilter, anyOf(failedItemFilters));
    }

    Iterable<Metadata> results;
    try {
      results = query(finalFilter, 1, 100);
    } catch (Exception e) {
      throw new AdapterException("the query failed");
    }

    return new QueryResponseImpl(results);
  }

  @Override
  public boolean exists(Metadata metadata) {
    final String metacardId = metadata.getId();
    try {
      return query(equalTo(Core.ID, metacardId), 1, 1).size() > 0;
    } catch (Exception e) {
      throw new AdapterException(
          String.format(
              "Error checking for the existence of metacard %s on %s",
              metacardId, getSystemName()));
    }
  }

  @Override
  public boolean createRequest(CreateRequest createRequest) {
    List<Metadata> metadata = createRequest.getMetadata();
    DdfRestClient client = ddfRestClientFactory.create(hostUrl.toString());
    return performRequestForEach(client::post, metadata);
  }

  @Override
  public boolean updateRequest(UpdateRequest updateRequest) {
    List<Metadata> metadata = updateRequest.getMetadata();
    DdfRestClient client = ddfRestClientFactory.create(hostUrl.toString());
    return performRequestForEach(client::put, metadata);
  }

  @Override
  public boolean deleteRequest(DeleteRequest deleteRequest) {
    List<String> ids =
        deleteRequest.getMetadata().stream().map(Metadata::getId).collect(Collectors.toList());
    DdfRestClient client = ddfRestClientFactory.create(hostUrl.toString());
    return performRequestForEach(client::delete, ids);
  }

  // TODO: test the public methods below here
  @Override
  public ResourceResponse readResource(ResourceRequest resourceRequest) {
    /*Metadata metadata = resourceRequest.getMetadata();

    String id = metadata.getId();
    URI uri = metadata.getResourceUri();

    Map<String, Serializable> properties = new HashMap<>();
    properties.put(Core.ID, id);

    Resource resource;
    try {
      resource = super.retrieveResource(uri, properties).getResource();
    } catch (IOException | ResourceNotFoundException | ResourceNotSupportedException e) {
      throw new AdapterException(
          String.format("Failed to retrieve resource %s", uri.toASCIIString()), e);
    }

    return new ResourceResponseImpl(
        new ResourceImpl(
            id,
            resource.getName(),
            uri,
            getQualifier(uri),
            resource.getInputStream(),
            resource.getMimeTypeValue(),
            resource.getSize(),
            metadata));*/
    return null;
  }

  @Override
  public boolean createResource(CreateStorageRequest createStorageRequest) {
    List<Resource> resources = createStorageRequest.getResources();
    DdfRestClient client = ddfRestClientFactory.create(hostUrl.toString());
    return performRequestForEach(client::post, resources);
  }

  @Override
  public boolean updateResource(UpdateStorageRequest updateStorageRequest) {
    List<Resource> resources = updateStorageRequest.getResources();
    DdfRestClient client = ddfRestClientFactory.create(hostUrl.toString());
    return performRequestForEach(client::put, resources);
  }

  @Override
  public void close() throws IOException {}

  private <T> boolean performRequestForEach(Function<T, Response> request, List<T> requestBodies) {
    for (T body : requestBodies) {
      Response response = request.apply(body);
      if (response != null && !response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
        return false;
      }
    }

    return true;
  }
}
