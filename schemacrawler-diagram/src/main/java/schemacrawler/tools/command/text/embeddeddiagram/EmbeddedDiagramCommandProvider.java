/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2025, Sualeh Fatehi <sualeh@hotmail.com>.
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

package schemacrawler.tools.command.text.embeddeddiagram;

import schemacrawler.tools.command.text.diagram.GraphExecutorFactory;
import schemacrawler.tools.command.text.diagram.options.DiagramOptions;
import schemacrawler.tools.command.text.diagram.options.DiagramOptionsBuilder;
import schemacrawler.tools.command.text.diagram.options.DiagramOutputFormat;
import schemacrawler.tools.command.text.schema.options.CommandProviderUtility;
import schemacrawler.tools.executable.BaseCommandProvider;
import schemacrawler.tools.options.Config;
import schemacrawler.tools.options.OutputOptions;
import us.fatehi.utility.property.PropertyName;

public final class EmbeddedDiagramCommandProvider extends BaseCommandProvider {

  public EmbeddedDiagramCommandProvider() {
    super(CommandProviderUtility.schemaTextCommands());
  }

  @Override
  public EmbeddedDiagramRenderer newSchemaCrawlerCommand(
      final String command, final Config config) {
    final PropertyName commandName = lookupSupportedCommand(command);
    if (commandName == null) {
      throw new IllegalArgumentException("Cannot support command, " + command);
    }

    final DiagramOptions diagramOptions =
        DiagramOptionsBuilder.builder().fromConfig(config).toOptions();
    final EmbeddedDiagramRenderer scCommand =
        new EmbeddedDiagramRenderer(commandName, new GraphExecutorFactory());
    scCommand.configure(diagramOptions);
    return scCommand;
  }

  @Override
  public boolean supportsOutputFormat(final String command, final OutputOptions outputOptions) {
    return supportsOutputFormat(
        command,
        outputOptions,
        format -> {
          final DiagramOutputFormat diagramOutputFormat = DiagramOutputFormat.fromFormat(format);
          final boolean supportsOutputFormat = diagramOutputFormat == DiagramOutputFormat.htmlx;
          return supportsOutputFormat;
        });
  }
}
