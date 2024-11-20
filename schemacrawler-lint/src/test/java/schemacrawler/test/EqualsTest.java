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

package schemacrawler.test;

import org.junit.jupiter.api.Test;
import nl.jqno.equalsverifier.EqualsVerifier;
import schemacrawler.tools.lint.Lint;
import schemacrawler.tools.lint.config.LinterConfig;

public class EqualsTest {

  @Test
  public void lint() {
    EqualsVerifier.forClass(Lint.class)
        .withIgnoredFields("lintId", "linterInstanceId", "objectKey")
        .verify();
  }

  @Test
  public void linterConfig() {
    EqualsVerifier.forClass(LinterConfig.class)
        .withIgnoredFields(
            "config",
            "runLinter",
            "threshold",
            "tableInclusionPattern",
            "tableExclusionPattern",
            "columnInclusionPattern",
            "columnExclusionPattern")
        .verify();
  }
}
