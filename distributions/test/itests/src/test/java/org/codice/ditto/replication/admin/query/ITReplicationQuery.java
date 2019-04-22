/**
 * Copyright (c) Codice Foundation
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
package org.codice.ditto.replication.admin.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

import java.net.MalformedURLException;
import org.codice.ddf.dominion.commons.options.DDFCommonOptions;
import org.codice.ddf.dominion.options.DDFOptions;
import org.codice.ditto.replication.admin.test.queryHelper;
import org.codice.dominion.Dominion;
import org.codice.dominion.interpolate.Interpolate;
import org.codice.dominion.options.Options;
import org.codice.dominion.options.Permission;
import org.codice.dominion.options.karaf.KarafOptions;
import org.codice.junit.TestDelimiter;
import org.codice.maven.MavenUrl;
import org.codice.pax.exam.junit.ConfigurationAdmin;
import org.codice.pax.exam.junit.ServiceAdmin;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@DDFCommonOptions.ConfigureVMOptionsForTesting
@DDFCommonOptions.ConfigureDebugging
@DDFCommonOptions.ConfigurePorts
@DDFCommonOptions.ConfigureLogging
@DDFOptions.InstallDistribution(solr = true)
@Options.GrantPermission(
  permission = {@Permission(clazz = java.lang.RuntimePermission.class, name = "createClassLoader")}
)
@KarafOptions.InstallFeature(
  repository =
      @MavenUrl(
        groupId = "replication.distributions.features",
        artifactId = "test-utilities",
        version = MavenUrl.AS_PROJECT,
        type = "xml",
        classifier = "features"
      )
)
@TestDelimiter(stdout = true, elapsed = true)
@ServiceAdmin
@ConfigurationAdmin
@RunWith(Dominion.class)
public class ITReplicationQuery {

  @Interpolate
  private static String GRAPHQL_ENDPOINT = "https://localhost:{port.https}/admin/hub/graphql";

  private static final String URL = "https://localhost:9999";

  private static queryHelper queryHelper;

  @BeforeClass
  public static void before() {
    queryHelper = new queryHelper(GRAPHQL_ENDPOINT);
  }

  // ----------------------------------- Site Tests -----------------------------------//

  @Test
  public void createReplicationSite() throws MalformedURLException {
    String name = "create";
    queryHelper
        .performCreateSiteQuery(name, URL)
        .body("data.createReplicationSite.name", is(name))
        .body("data.createReplicationSite.address.url", is(URL))
        .body("data.createReplicationSite.address.host.hostname", is("localhost"))
        .body("data.createReplicationSite.address.host.port", is(9999))
        .body("data.createReplicationSite.id", is(notNullValue()));
  }

  @Test
  public void createReplicationSiteWithHostnameAndPort() {
    String name = "createHostnameAndPort";
    queryHelper
        .performQuery(
            String.format(
                "{\"query\":\"mutation{ createReplicationSite(name: \\\"%s\\\", address: { host: { hostname: \\\"localhost\\\" port: 9999 }}){ id name address{ host{ hostname port } url }}}\"}",
                name))
        .body("data.createReplicationSite.name", is(name))
        .body("data.createReplicationSite.address.url", is(URL))
        .body("data.createReplicationSite.address.host.hostname", is("localhost"))
        .body("data.createReplicationSite.address.host.port", is(9999))
        .body("data.createReplicationSite.id", is(notNullValue()));
  }

  @Test
  public void createReplicationSiteWithEmptyName() {
    queryHelper.performCreateSiteQuery("", URL).body("errors.message", hasItem("EMPTY_FIELD"));
  }

  @Test
  public void createReplicationSiteWithInvalidUrl() {
    queryHelper
        .performCreateSiteQuery("badUrl", "localhost:9999")
        .body("errors.message", hasItem("INVALID_URL"));
  }

  @Test
  public void createReplicationSiteWithIntegerName() {
    queryHelper
        .performQuery(
            String.format(
                "{\"query\":\"mutation{ createReplicationSite(name: 25, address: { url: \\\"%s\\\"}){ id name address{ host{ hostname port } url }}}\"}",
                URL))
        .body("errors.errorType", hasItem("ValidationError"));
  }

  @Test
  public void getReplicationSites() {
    queryHelper.performGetSitesQuery().body("errors", is(nullValue()));
  }

  @Test
  public void updateReplicationSite() {
    queryHelper
        .performUpdateSiteQuery("siteId", "newName", URL)
        .body("errors", is(nullValue())); // what we currently get when a
    // site with the given ID
    // doesn't exist
  }

  @Test
  public void deleteReplicationSite() {
    assertThat(queryHelper.deleteSite("fakeId"), is(false));
  }

  // ----------------------------------- Replication Tests -----------------------------------//

  // TODO: make these more rigorous when Repsync persistence functionality is implemented
  @Test
  public void createRepsync() {
    queryHelper
        .performCreateRepsyncQuery("name", "sourceid", "destinationid", "filter")
        .body("data.createRepsync.name", is("name"));
  }

  @Test
  public void updateRepsync() {
    queryHelper
        .performUpdateRepsyncQuery("id", "name", "sourceid", "destinationid", "filter")
        .body("data.updateRepsync.name", is("name"));
  }

  @Test
  public void deleteRepsync() {
    queryHelper.performDeleteRepsyncQuery("id").body("data.deleteRepsync", is(true));
  }

  @Test
  public void getRepsyncs() {
    queryHelper.performGetRepsyncsQuery().body("errors", is(nullValue()));
  }

  // ----------------------------------- General Tests -----------------------------------//

  @Test
  public void undefinedFieldInQuery() {
    queryHelper
        .performQuery("{\"query\":\"mutation{ unknownField }\"}")
        .body("errors.errorType", hasItem("ValidationError"));
  }
}
