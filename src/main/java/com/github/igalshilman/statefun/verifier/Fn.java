package com.github.igalshilman.statefun.verifier;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.util.Objects;

public class Fn implements StatefulFunction {

  @Persisted private final PersistedValue<Long> state = PersistedValue.of("state", Long.class);
  private final CommandInterpreter interpreter;

  public Fn(CommandInterpreter interpreter) {
    this.interpreter = Objects.requireNonNull(interpreter);
  }

  @Override
  public void invoke(Context context, Object message) {
    interpreter.interpret(state, context, message);
  }
}
