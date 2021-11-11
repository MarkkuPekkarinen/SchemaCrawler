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
package schemacrawler.tools.formatter.serialize;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Writer;

import schemacrawler.schema.Catalog;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerRuntimeException;

/** Decorates a database to allow for serialization to and from plain Java serialization. */
public final class JavaSerializedCatalog implements CatalogSerializer {

  private static Catalog readCatalog(final InputStream in) throws SchemaCrawlerException {
    requireNonNull(in, "No input stream provided");
    try (final ObjectInputStream objIn = new ObjectInputStream(in)) {
      return (Catalog) objIn.readObject();
    } catch (ClassNotFoundException | IOException e) {
      throw new SchemaCrawlerRuntimeException("Cannot deserialize catalog", e);
    }
  }

  private final Catalog catalog;

  public JavaSerializedCatalog(final Catalog catalog) {
    this.catalog = requireNonNull(catalog, "No catalog provided");
  }

  public JavaSerializedCatalog(final InputStream in) throws SchemaCrawlerException {
    this(readCatalog(in));
  }

  @Override
  public Catalog getCatalog() {
    return catalog;
  }

  /** {@inheritDoc} */
  @Override
  public void save(final OutputStream out) throws SchemaCrawlerException {
    requireNonNull(out, "No output stream provided");
    try (final ObjectOutputStream objOut = new ObjectOutputStream(out)) {
      objOut.writeObject(catalog);
    } catch (final IOException e) {
      throw new SchemaCrawlerRuntimeException("Could not serialize catalog", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void save(final Writer out) {
    throw new UnsupportedOperationException("Cannot serialize binary format using character data");
  }
}
