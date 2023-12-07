/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2023, Sualeh Fatehi <sualeh@hotmail.com>.
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

package schemacrawler.crawl;

import static java.util.Objects.requireNonNull;
import static schemacrawler.schemacrawler.InformationSchemaKey.TABLES;
import static schemacrawler.schemacrawler.SchemaInfoMetadataRetrievalStrategy.tablesRetrievalStrategy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import schemacrawler.filter.InclusionRuleFilter;
import schemacrawler.inclusionrule.InclusionRule;
import schemacrawler.schema.NamedObjectKey;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.schema.TableType;
import schemacrawler.schema.TableTypes;
import schemacrawler.schemacrawler.InformationSchemaViews;
import schemacrawler.schemacrawler.Query;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaReference;
import schemacrawler.schemacrawler.exceptions.ExecutionRuntimeException;
import us.fatehi.utility.string.StringFormat;

/** A retriever uses database metadata to get the details about the database tables. */
final class TableRetriever extends AbstractRetriever {

  private static final Logger LOGGER = Logger.getLogger(TableRetriever.class.getName());

  TableRetriever(
      final RetrieverConnection retrieverConnection,
      final MutableCatalog catalog,
      final SchemaCrawlerOptions options)
      throws SQLException {
    super(retrieverConnection, catalog, options);
  }

  void retrieveTables(
      final String tableNamePattern,
      final TableTypes tableTypes,
      final InclusionRule tableInclusionRule)
      throws SQLException {
    requireNonNull(tableTypes, "No table types provided");

    final NamedObjectList<SchemaReference> schemas = getAllSchemas();

    final InclusionRuleFilter<Table> tableFilter =
        new InclusionRuleFilter<>(tableInclusionRule, false);
    if (tableFilter.isExcludeAll()) {
      LOGGER.log(Level.INFO, "Not retrieving tables, since this was not requested");
      return;
    }

    switch (getRetrieverConnection().get(tablesRetrievalStrategy)) {
      case data_dictionary_all:
        LOGGER.log(Level.INFO, "Retrieving tables, using fast data dictionary retrieval");
        retrieveTablesFromDataDictionary(schemas, tableTypes, tableFilter);
        break;

      case metadata:
        LOGGER.log(Level.INFO, "Retrieving tables");
        retrieveTablesFromMetadata(schemas, tableNamePattern, tableTypes, tableFilter);
        break;

      default:
        LOGGER.log(Level.INFO, "Not retrieving tables");
        break;
    }
  }

  private void createTable(
      final MetadataResultSet results,
      final NamedObjectList<SchemaReference> schemas,
      final InclusionRuleFilter<Table> tableFilter,
      final TableTypes filteredTableTypes) {
    final String catalogName = normalizeCatalogName(results.getString("TABLE_CAT"));
    final String schemaName = normalizeSchemaName(results.getString("TABLE_SCHEM"));
    final String tableName = results.getString("TABLE_NAME");
    LOGGER.log(
        Level.FINE,
        new StringFormat("Retrieving table <%s.%s.%s>", catalogName, schemaName, tableName));
    final String tableTypeString = results.getString("TABLE_TYPE");
    final String remarks = results.getString("REMARKS");

    final Optional<SchemaReference> optionalSchema =
        schemas.lookup(new NamedObjectKey(catalogName, schemaName));
    if (!optionalSchema.isPresent()) {
      return;
    }
    final Schema schema = optionalSchema.get();

    final TableType tableType =
        filteredTableTypes.lookupTableType(tableTypeString).orElse(TableType.UNKNOWN);
    if (tableType.equals(TableType.UNKNOWN)) {
      LOGGER.log(
          Level.FINE,
          new StringFormat(
              "Not including table <%s.%s>, since table type <%s> was not requested",
              schema, tableName, tableTypeString));
      return;
    }

    final MutableTable table;
    if (tableType.isView()) {
      table = new MutableView(schema, tableName);
    } else {
      table = new MutableTable(schema, tableName);
    }
    table.withQuoting(getRetrieverConnection().getIdentifiers());

    if (tableFilter.test(table)) {
      table.setTableType(tableType);
      table.setRemarks(remarks);
      table.addAttributes(results.getAttributes());

      catalog.addTable(table);
    }
  }

  private void retrieveTablesFromDataDictionary(
      final NamedObjectList<SchemaReference> schemas,
      final TableTypes tableTypes,
      final InclusionRuleFilter<Table> tableFilter)
      throws SQLException {
    final InformationSchemaViews informationSchemaViews =
        getRetrieverConnection().getInformationSchemaViews();
    if (!informationSchemaViews.hasQuery(TABLES)) {
      throw new ExecutionRuntimeException("No tables SQL provided");
    }
    final Query tablesSql = informationSchemaViews.getQuery(TABLES);
    final TableTypes supportedTableTypes = getRetrieverConnection().getTableTypes();
    final TableTypes filteredTableTypes;
    if (tableTypes.isIncludeAll()) {
      filteredTableTypes = supportedTableTypes;
    } else {
      filteredTableTypes = tableTypes;
    }
    try (final Connection connection = getRetrieverConnection().getConnection();
        final Statement statement = connection.createStatement();
        final MetadataResultSet results =
            new MetadataResultSet(
                tablesSql, statement, getSchemaInclusionRule(), getTableInclusionRule()); ) {
      int numTables = 0;
      while (results.next()) {
        numTables = numTables + 1;
        createTable(results, schemas, tableFilter, filteredTableTypes);
      }
      LOGGER.log(Level.INFO, new StringFormat("Processed %d tables", numTables));
    }
  }

  private void retrieveTablesFromMetadata(
      final NamedObjectList<SchemaReference> schemas,
      final String tableNamePattern,
      final TableTypes tableTypes,
      final InclusionRuleFilter<Table> tableFilter)
      throws SQLException {
    for (final Schema schema : schemas) {
      LOGGER.log(Level.INFO, new StringFormat("Retrieving tables for schema <%s>", schema));

      final TableTypes supportedTableTypes = getRetrieverConnection().getTableTypes();
      final TableTypes filteredTableTypes = supportedTableTypes.subsetFrom(tableTypes);
      LOGGER.log(Level.FINER, new StringFormat("Retrieving table types <%s>", filteredTableTypes));

      final String catalogName = schema.getCatalogName();
      final String schemaName = schema.getName();

      try (final Connection connection = getRetrieverConnection().getConnection();
          final MetadataResultSet results =
              new MetadataResultSet(
                  connection
                      .getMetaData()
                      .getTables(
                          catalogName, schemaName, tableNamePattern, filteredTableTypes.toArray()),
                  "DatabaseMetaData::getTables"); ) {
        int numTables = 0;
        while (results.next()) {
          numTables = numTables + 1;
          createTable(results, schemas, tableFilter, supportedTableTypes);
        }
        LOGGER.log(Level.INFO, new StringFormat("Processed %d tables", numTables));
      }
    }
  }
}
