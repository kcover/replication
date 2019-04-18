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
package org.codice.ditto.replication.admin.test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.codice.ditto.replication.api.data.ReplicationSite;
import org.codice.ditto.replication.api.persistence.SiteManager;

public class MockSiteManager implements SiteManager {

  private List<ReplicationSite> sites;

  public MockSiteManager() {
    sites = new ArrayList<>();
  }

  public void clearSites() {
    sites.clear();
  }

  @Override
  public ReplicationSite createSite(String name, String url) {
    return new ReplicationSiteImpl(name, url);
  }

  @Override
  public ReplicationSite create() {
    return new ReplicationSiteImpl();
  }

  @Override
  @Nullable
  public ReplicationSite get(String id) {
    return objects().filter(site -> site.getId().equals(id)).findFirst().orElse(null);
  }

  @Override
  public Stream<ReplicationSite> objects() {
    return sites.stream();
  }

  @Override
  public void save(ReplicationSite object) {
    sites.add(object);
  }

  @Override
  public void remove(String id) {
    sites.removeIf(site -> site.getId().equals(id));
  }

  @Override
  public boolean exists(String id) {
    return sites.stream().map(ReplicationSite::getId).anyMatch(id::equals);
  }
}
