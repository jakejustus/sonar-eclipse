/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.core.internal.servers;

import com.google.common.collect.Lists;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ServersManager implements ISonarServersManager {
  static final String PREF_SERVERS = "servers";

  @Override
  public Collection<ISonarServer> getServers() {
    IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    List<ISonarServer> servers = Lists.newArrayList();
    try {
      rootNode.sync();
      if (rootNode.nodeExists(PREF_SERVERS)) {
        Preferences serversNode = rootNode.node(PREF_SERVERS);
        for (String encodedUrl : serversNode.childrenNames()) {
          Preferences serverNode = serversNode.node(encodedUrl);
          String url = EncodingUtils.decodeSlashes(encodedUrl);
          boolean auth = serverNode.getBoolean("auth", false);
          servers.add(new SonarServer(url, auth));
        }
      } else {
        // Defaults
        return Arrays.asList((ISonarServer) new SonarServer("http://localhost:9000"));
      }
    } catch (BackingStoreException e) {
      LoggerFactory.getLogger(SecurityManager.class).error(e.getMessage(), e);
    }
    return servers;
  }

  @Override
  public void addServer(String url, String username, String password) {
    SonarServer server = new SonarServer(url, username, password);
    String encodedUrl = EncodingUtils.encodeSlashes(server.getUrl());
    IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      serversNode.put("initialized", "true");
      serversNode.node(encodedUrl).putBoolean("auth", server.hasCredentials());
      serversNode.flush();
    } catch (BackingStoreException e) {
      LoggerFactory.getLogger(SecurityManager.class).error(e.getMessage(), e);
    }
  }

  /**
   * For tests.
   */
  public void clean() {
    IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      rootNode.node(PREF_SERVERS).removeNode();
      rootNode.node(PREF_SERVERS).put("initialized", "true");
      rootNode.flush();
    } catch (BackingStoreException e) {
      LoggerFactory.getLogger(SecurityManager.class).error(e.getMessage(), e);
    }
  }

  @Override
  public void removeServer(String url) {
    String encodedUrl = EncodingUtils.encodeSlashes(url);
    IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
    try {
      Preferences serversNode = rootNode.node(PREF_SERVERS);
      serversNode.node(encodedUrl).removeNode();
      serversNode.flush();
    } catch (BackingStoreException e) {
      LoggerFactory.getLogger(SecurityManager.class).error(e.getMessage(), e);
    }
  }

  @Override
  public ISonarServer findServer(String url) {
    for (ISonarServer server : getServers()) {
      if (server.getUrl().equals(url)) {
        return server;
      }
    }
    return null;
  }

  @Override
  public ISonarServer getDefault() {
    return new SonarServer("http://localhost:9000");
  }

  @Override
  public ISonarServer create(String location, String username, String password) {
    return new SonarServer(location, username, password);
  }

}
