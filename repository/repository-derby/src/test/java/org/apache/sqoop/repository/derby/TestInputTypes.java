/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sqoop.repository.derby;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.sqoop.model.InputEditable;
import org.apache.sqoop.model.MBooleanInput;
import org.apache.sqoop.model.MConfig;
import org.apache.sqoop.model.MConnector;
import org.apache.sqoop.model.MDriver;
import org.apache.sqoop.model.MEnumInput;
import org.apache.sqoop.model.MInput;
import org.apache.sqoop.model.MIntegerInput;
import org.apache.sqoop.model.MLink;
import org.apache.sqoop.model.MLinkConfig;
import org.apache.sqoop.model.MMapInput;
import org.apache.sqoop.model.MPersistableEntity;
import org.apache.sqoop.model.MStringInput;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test proper support of all available model types.
 */
public class TestInputTypes extends DerbyTestCase {

  DerbyRepositoryHandler handler;

  @BeforeMethod(alwaysRun = true)
  public void setUp() throws Exception {
    super.setUp();

    handler = new DerbyRepositoryHandler();
  }

  /**
   * Ensure that metadata with all various data types can be successfully
   * serialized into repository and retrieved back.
   */
  @Test
  public void testEntitySerialization() throws Exception {
    MConnector connector = getConnector();

    // Serialize the connector with all data types into repository
    handler.registerConnector(connector, getDerbyDatabaseConnection());

    // Successful serialization should update the ID
    assertNotSame(connector.getPersistenceId(), MPersistableEntity.PERSISTANCE_ID_DEFAULT);

    // Retrieve registered connector
    MConnector retrieved = handler.findConnector(connector.getUniqueName(),
        getDerbyDatabaseConnection());
    assertNotNull(retrieved);

    // Original and retrieved connectors should be the same
    assertEquals(connector, retrieved);
  }

  /**
   * Test that serializing actual data is not an issue.
   */
  @Test
  public void testEntityDataSerialization() throws Exception {
    MConnector connector = getConnector();
    MDriver driver = getDriver();

    // Register objects for everything and our new connector
    handler.registerConnector(connector, getDerbyDatabaseConnection());
    handler.registerDriver(driver, getDerbyDatabaseConnection());

    // Inserted values
    Map<String, String> map = new HashMap<String, String>();
    map.put("A", "B");

    // Connection object with all various values
    MLink link = new MLink(connector.getPersistenceId(), connector.getLinkConfig());
    MLinkConfig linkConfig = link.getConnectorLinkConfig();
    assertEquals(linkConfig.getStringInput("LINK1.I1").getEditable(), InputEditable.USER_ONLY);
    assertEquals(linkConfig.getStringInput("LINK1.I1").getOverrides(), "LINK1.I2");
    assertEquals(linkConfig.getMapInput("LINK1.I2").getEditable(), InputEditable.CONNECTOR_ONLY);
    assertEquals(linkConfig.getMapInput("LINK1.I2").getOverrides(), "LINK1.I5");
    assertEquals(linkConfig.getIntegerInput("LINK1.I3").getEditable(), InputEditable.ANY);
    assertEquals(linkConfig.getIntegerInput("LINK1.I3").getOverrides(), "LINK1.I1");
    assertEquals(linkConfig.getBooleanInput("LINK1.I4").getEditable(), InputEditable.USER_ONLY);
    assertEquals(linkConfig.getBooleanInput("LINK1.I4").getOverrides(), "");
    assertEquals(linkConfig.getEnumInput("LINK1.I5").getEditable(), InputEditable.ANY);
    assertEquals(linkConfig.getEnumInput("LINK1.I5").getOverrides(), "LINK1.I4,LINK1.I3");

    linkConfig.getStringInput("LINK1.I1").setValue("A");
    linkConfig.getMapInput("LINK1.I2").setValue(map);
    linkConfig.getIntegerInput("LINK1.I3").setValue(1);
    linkConfig.getBooleanInput("LINK1.I4").setValue(true);
    linkConfig.getEnumInput("LINK1.I5").setValue("YES");

    // Create the link in repository
    handler.createLink(link, getDerbyDatabaseConnection());
    assertNotSame(link.getPersistenceId(), MPersistableEntity.PERSISTANCE_ID_DEFAULT);

    // Retrieve created link
    MLink retrieved = handler.findLink(link.getPersistenceId(), getDerbyDatabaseConnection());
    linkConfig = retrieved.getConnectorLinkConfig();
    assertEquals("A", linkConfig.getStringInput("LINK1.I1").getValue());
    assertEquals(map, linkConfig.getMapInput("LINK1.I2").getValue());
    assertEquals(1, (int) linkConfig.getIntegerInput("LINK1.I3").getValue());
    assertEquals(true, (boolean) linkConfig.getBooleanInput("LINK1.I4").getValue());
    assertEquals("YES", linkConfig.getEnumInput("LINK1.I5").getValue());
    assertEquals(linkConfig.getEnumInput("LINK1.I5").getEditable(), InputEditable.ANY);
    assertEquals(linkConfig.getEnumInput("LINK1.I5").getOverrides(), "LINK1.I4,LINK1.I3");

  }
}
