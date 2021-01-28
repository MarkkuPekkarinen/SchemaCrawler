/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2021, Sualeh Fatehi <sualeh@hotmail.com>.
All rights reserved.
------------------------------------------------------------------------

SchemaCrawler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

SchemaCrawler and the accompanying materials are made available under
the terms of the Eclipse Public License v1.0, GNU General Public License
v3 or GNU Lesser General Public License v3.

You may elect to redistribute this code under any of these licenses.

The Eclipse Public License is available at:
http://www.eclipse.org/legal/epl-v10.html

The GNU General Public License v3 and the GNU Lesser General Public
License v3 are available at:
http://www.gnu.org/licenses/

========================================================================
*/

package schemacrawler.tools.catalogloader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;

import schemacrawler.SchemaCrawlerLogger;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import us.fatehi.utility.string.StringFormat;

/**
 * Registry for mapping database connectors from DatabaseConnector-line switch.
 *
 * @author Sualeh Fatehi
 */
public final class CatalogLoaderRegistry {

  private static final SchemaCrawlerLogger LOGGER =
      SchemaCrawlerLogger.getLogger(CatalogLoaderRegistry.class.getName());

  public ChainedCatalogLoader loadCatalogLoaders() throws SchemaCrawlerException {
    final List<CatalogLoader> chainedCatalogLoaders = loadCatalogLoaderRegistry();
    return new ChainedCatalogLoader(chainedCatalogLoaders);
  }

  private List<CatalogLoader> loadCatalogLoaderRegistry() throws SchemaCrawlerException {

    final List<CatalogLoader> catalogLoaderRegistry = new ArrayList<>();

    try {
      final ServiceLoader<CatalogLoader> serviceLoader = ServiceLoader.load(CatalogLoader.class);
      for (final CatalogLoader catalogLoader : serviceLoader) {
        LOGGER.log(
            Level.CONFIG,
            new StringFormat("Loading catalog loader, %s", catalogLoader.getClass().getName()));

        catalogLoaderRegistry.add(catalogLoader);
      }
    } catch (final Exception e) {
      throw new SchemaCrawlerException("Could not load catalog loader registry", e);
    }

    Collections.sort(catalogLoaderRegistry);
    return catalogLoaderRegistry;
  }
}
