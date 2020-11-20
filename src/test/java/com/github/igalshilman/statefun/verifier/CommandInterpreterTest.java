package com.github.igalshilman.statefun.verifier;

import com.github.igalshilman.statefun.verifier.generated.SourceCommand;
import org.apache.flink.statefun.sdk.Address;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.io.EgressIdentifier;
import org.apache.flink.statefun.sdk.state.PersistedValue;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static com.github.igalshilman.statefun.verifier.Utils.aStateModificationCommand;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CommandInterpreterTest {

  @Test
  public void exampleUsage() {
    CommandInterpreter interpreter = new CommandInterpreter(new Ids(10));

    PersistedValue<Long> state = PersistedValue.of("state", Long.class);
    Context context = new MockContext();
    SourceCommand sourceCommand = aStateModificationCommand();

    interpreter.interpret(state, context, sourceCommand);

    assertThat(state.get(), is(1L));
  }

  private static final class MockContext implements Context {

    @Override
    public Address self() {
      return null;
    }

    @Override
    public Address caller() {
      return null;
    }

    @Override
    public void send(Address address, Object o) {}

    @Override
    public <T> void send(EgressIdentifier<T> egressIdentifier, T t) {}

    @Override
    public void sendAfter(Duration duration, Address address, Object o) {}

    @Override
    public <M, T> void registerAsyncOperation(M m, CompletableFuture<T> completableFuture) {}
  }
}
