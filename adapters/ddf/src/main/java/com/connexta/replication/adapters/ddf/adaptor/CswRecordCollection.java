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
package com.connexta.replication.adapters.ddf.adaptor;

import java.util.ArrayList;
import java.util.List;
import org.codice.ditto.replication.api.data.Metadata;

/**
 * This class represents the domain object for the list of metacards corresponding to the list of
 * CSW records returned in a GetRecords request.
 *
 * @author rodgersh
 */
public class CswRecordCollection {

  private List<Metadata> cswRecords = new ArrayList<>();

  private long numberOfRecordsReturned;

  private long numberOfRecordsMatched;

  /**
   * Retrieves the list of metacards built from the CSW Records returned in a GetRecordsResponse.
   *
   * @return
   */
  public List<Metadata> getCswRecords() {
    return cswRecords;
  }

  /**
   * Sets the list of metacards built from the CSW Records returned in a GetRecordsResponse.
   *
   * @param cswRecords
   */
  public void setCswRecords(List<Metadata> cswRecords) {
    this.cswRecords = cswRecords;
  }

  public long getNumberOfRecordsReturned() {
    return numberOfRecordsReturned;
  }

  public void setNumberOfRecordsReturned(long numberOfRecordsReturned) {
    this.numberOfRecordsReturned = numberOfRecordsReturned;
  }

  public long getNumberOfRecordsMatched() {
    return numberOfRecordsMatched;
  }

  public void setNumberOfRecordsMatched(long numberOfRecordsMatched) {
    this.numberOfRecordsMatched = numberOfRecordsMatched;
  }
}