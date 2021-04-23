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
package schemacrawler.integration.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static schemacrawler.test.utility.ExecutableTestUtility.executableExecution;
import static schemacrawler.test.utility.FileHasContent.classpathResource;
import static schemacrawler.test.utility.FileHasContent.hasSameContentAs;
import static schemacrawler.test.utility.FileHasContent.outputOf;
import static schemacrawler.test.utility.TestUtility.javaVersion;
import static us.fatehi.utility.DatabaseUtility.checkConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import schemacrawler.crawl.SchemaCrawler;
import schemacrawler.inclusionrule.RegularExpressionInclusionRule;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.DatabaseUser;
import schemacrawler.schema.Property;
import schemacrawler.schemacrawler.LimitOptionsBuilder;
import schemacrawler.schemacrawler.LoadOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.schemacrawler.SchemaRetrievalOptions;
import schemacrawler.server.sqlserver.SqlServerDatabaseConnector;
import schemacrawler.test.utility.BaseAdditionalDatabaseTest;
import schemacrawler.tools.command.text.schema.options.SchemaTextOptions;
import schemacrawler.tools.command.text.schema.options.SchemaTextOptionsBuilder;
import schemacrawler.tools.databaseconnector.DatabaseConnector;
import schemacrawler.tools.executable.SchemaCrawlerExecutable;

@Testcontainers(disabledWithoutDocker = true)
@EnabledIfSystemProperty(named = "heavydb", matches = "^((?!(false|no)).)*$")
public class SqlServerTest extends BaseAdditionalDatabaseTest {

  @Container
  private final JdbcDatabaseContainer<?> dbContainer =
      new MSSQLServerContainer<>(
              DockerImageName.parse("mcr.microsoft.com/mssql/server")
                  .withTag("2017-CU22-ubuntu-16.04"))
          .acceptLicense();

  @BeforeEach
  public void createDatabase() throws SQLException, SchemaCrawlerException {
    createDataSource(
        dbContainer.getJdbcUrl(), dbContainer.getUsername(), dbContainer.getPassword());

    createDatabase("/sqlserver.scripts.txt");
  }

  @Test
  public void testSQLServerCatalog() throws Exception {
    final LimitOptionsBuilder limitOptionsBuilder =
        LimitOptionsBuilder.builder()
            .includeSchemas(new RegularExpressionInclusionRule("BOOKS\\.dbo"))
            .includeAllSequences()
            .includeAllSynonyms()
            .includeAllRoutines()
            .tableTypes("TABLE,VIEW,MATERIALIZED VIEW");
    final LoadOptionsBuilder loadOptionsBuilder =
        LoadOptionsBuilder.builder().withSchemaInfoLevel(SchemaInfoLevelBuilder.maximum());
    final SchemaCrawlerOptions schemaCrawlerOptions =
        SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
            .withLimitOptions(limitOptionsBuilder.toOptions())
            .withLoadOptions(loadOptionsBuilder.toOptions());

    final Connection connection = checkConnection(getConnection());
    final DatabaseConnector databaseConnector = new SqlServerDatabaseConnector();

    final SchemaRetrievalOptions schemaRetrievalOptions =
        databaseConnector.getSchemaRetrievalOptionsBuilder(connection).toOptions();

    final SchemaCrawler schemaCrawler =
        new SchemaCrawler(getConnection(), schemaRetrievalOptions, schemaCrawlerOptions);
    final Catalog catalog = schemaCrawler.crawl();
    final List<Property> serverInfo = new ArrayList<>(catalog.getDatabaseInfo().getServerInfo());

    assertThat(serverInfo.size(), equalTo(3));
    assertThat(serverInfo.get(0).getName(), equalTo("InstanceName"));
    assertThat(serverInfo.get(0).getValue(), is(nullValue()));

    final List<DatabaseUser> databaseUsers = (List<DatabaseUser>) catalog.getDatabaseUsers();
    assertThat(databaseUsers, hasSize(13));
    assertThat(
        databaseUsers.stream().map(DatabaseUser::getName).collect(Collectors.toList()),
        hasItems("dbo", "public", "db_datareader"));
    assertThat(
        databaseUsers
            .stream()
            .map(databaseUser -> databaseUser.getAttributes().size())
            .collect(Collectors.toList()),
        hasItems(4));
    assertThat(
        databaseUsers
            .stream()
            .map(databaseUser -> databaseUser.getAttributes().keySet())
            .flatMap(Collection::stream)
            .collect(Collectors.toSet()),
        hasItems("CREATE_DATE", "TYPE", "MODIFY_DATE", "AUTHENTICATION_TYPE"));
  }

  @Test
  public void testSQLServerWithConnection() throws Exception {
    final LimitOptionsBuilder limitOptionsBuilder =
        LimitOptionsBuilder.builder()
            .includeSchemas(new RegularExpressionInclusionRule("BOOKS\\.dbo"))
            .includeAllSequences()
            .includeAllSynonyms()
            .includeAllRoutines()
            .tableTypes("TABLE,VIEW,MATERIALIZED VIEW");
    final LoadOptionsBuilder loadOptionsBuilder =
        LoadOptionsBuilder.builder().withSchemaInfoLevel(SchemaInfoLevelBuilder.maximum());
    final SchemaCrawlerOptions schemaCrawlerOptions =
        SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
            .withLimitOptions(limitOptionsBuilder.toOptions())
            .withLoadOptions(loadOptionsBuilder.toOptions());

    final SchemaTextOptionsBuilder textOptionsBuilder = SchemaTextOptionsBuilder.builder();
    textOptionsBuilder.showDatabaseInfo().showJdbcDriverInfo();
    final SchemaTextOptions textOptions = textOptionsBuilder.toOptions();

    final SchemaCrawlerExecutable executable = new SchemaCrawlerExecutable("details");
    executable.setSchemaCrawlerOptions(schemaCrawlerOptions);
    executable.setAdditionalConfiguration(SchemaTextOptionsBuilder.builder(textOptions).toConfig());

    final String expectedResource =
        String.format("testSQLServerWithConnection.%s.txt", javaVersion());
    assertThat(
        outputOf(executableExecution(getConnection(), executable)),
        hasSameContentAs(classpathResource(expectedResource)));
  }
}
