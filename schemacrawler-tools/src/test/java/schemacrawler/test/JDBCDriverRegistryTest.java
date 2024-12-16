package schemacrawler.test;

import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static schemacrawler.test.utility.PluginRegistryTestUtility.reload;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import schemacrawler.schemacrawler.exceptions.InternalRuntimeException;
import schemacrawler.tools.registry.JDBCDriverRegistry;
import us.fatehi.test.utility.TestDatabaseDriver;
import us.fatehi.utility.property.PropertyName;

public class JDBCDriverRegistryTest {

  @Test
  public void registeredPlugins() {
    final JDBCDriverRegistry driverRegistry = JDBCDriverRegistry.getJDBCDriverRegistry();
    final Collection<PropertyName> commandLineCommands = driverRegistry.getRegisteredPlugins();
    assertThat(commandLineCommands, hasSize(17));
  }

  @Test
  public void name() {
    final JDBCDriverRegistry driverRegistry = JDBCDriverRegistry.getJDBCDriverRegistry();
    assertThat(driverRegistry.getName(), is("JDBC Drivers"));
  }

  @Test
  public void loadError() throws Exception {
    restoreSystemProperties(
        () -> {
          System.setProperty(
              TestDatabaseDriver.class.getName() + ".force-instantiation-failure", "throw");

          assertThrows(InternalRuntimeException.class, () -> reload(JDBCDriverRegistry.class));
        });
    // Reset
    reload(JDBCDriverRegistry.class);
  }
}
