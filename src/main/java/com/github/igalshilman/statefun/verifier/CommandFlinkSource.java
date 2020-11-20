package com.github.igalshilman.statefun.verifier;

import com.github.igalshilman.statefun.verifier.generated.Command;
import com.github.igalshilman.statefun.verifier.generated.Commands;
import com.github.igalshilman.statefun.verifier.generated.SourceCommand;
import com.github.igalshilman.statefun.verifier.generated.SourceSnapshot;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.OperatorStateStore;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.CheckpointListener;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class CommandFlinkSource extends RichSourceFunction<SourceCommand>
    implements CheckpointedFunction, CheckpointListener {

  private static final Logger LOG = LoggerFactory.getLogger(CommandFlinkSource.class);

  // ------------------------------------------------------------------------------------------------------------
  // Configuration
  // ------------------------------------------------------------------------------------------------------------

  private final ModuleParameters moduleParameters;

  // ------------------------------------------------------------------------------------------------------------
  // Runtime
  // ------------------------------------------------------------------------------------------------------------

  private transient ListState<SourceSnapshot> sourceSnapshotHandle;
  private transient FunctionStateTracker functionStateTracker;
  private transient int commandsSentSoFar;
  private transient int failuresSoFar;
  private transient boolean done;
  private transient boolean atLeastOneCheckpointCompleted;

  public CommandFlinkSource(ModuleParameters moduleParameters) {
    this.moduleParameters = Objects.requireNonNull(moduleParameters);
  }

  @Override
  public void initializeState(FunctionInitializationContext context) throws Exception {
    OperatorStateStore store = context.getOperatorStateStore();
    sourceSnapshotHandle =
        store.getUnionListState(new ListStateDescriptor<>("snapshot", SourceSnapshot.class));
  }

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    SourceSnapshot sourceSnapshot =
        getOnlyElement(sourceSnapshotHandle.get(), SourceSnapshot.getDefaultInstance());
    functionStateTracker =
        new FunctionStateTracker(moduleParameters.getNumberOfFunctionInstances())
            .apply(sourceSnapshot.getTracker());
    commandsSentSoFar = sourceSnapshot.getCommandsSentSoFarHandle();
    failuresSoFar = sourceSnapshot.getFailuresGeneratedSoFar();
  }

  @Override
  public void snapshotState(FunctionSnapshotContext context) throws Exception {
    sourceSnapshotHandle.clear();
    sourceSnapshotHandle.add(
        SourceSnapshot.newBuilder()
            .setCommandsSentSoFarHandle(commandsSentSoFar)
            .setTracker(functionStateTracker.snapshot())
            .setFailuresGeneratedSoFar(failuresSoFar)
            .build());

    if (commandsSentSoFar < moduleParameters.getMessageCount()) {
      double perCent = 100.0d * (commandsSentSoFar) / moduleParameters.getMessageCount();
      LOG.info(
          "Commands sent {} / {} ({} %)",
          commandsSentSoFar, moduleParameters.getMessageCount(), perCent);
    }
  }

  @Override
  public void notifyCheckpointComplete(long checkpointId) {
    atLeastOneCheckpointCompleted = true;
  }

  @Override
  public void cancel() {
    done = true;
  }

  // ------------------------------------------------------------------------------------------------------------
  // Generation
  // ------------------------------------------------------------------------------------------------------------

  @Override
  public void run(SourceContext<SourceCommand> ctx) {
    generate(ctx);
    do {
      verify(ctx);
      snooze();
      synchronized (ctx.getCheckpointLock()) {
        if (done) {
          return;
        }
      }
    } while (true);
  }

  private void generate(SourceContext<SourceCommand> ctx) {
    final int startPosition = this.commandsSentSoFar;
    final OptionalInt kaboomIndex =
        computeFailureIndex(startPosition, failuresSoFar, moduleParameters.getMaxFailures());
    if (kaboomIndex.isPresent()) {
      failuresSoFar++;
    }
    LOG.info(
        "starting at {}, kaboom at {}, total messages {}",
        startPosition,
        kaboomIndex,
        moduleParameters.getMessageCount());
    Supplier<SourceCommand> generator =
        new CommandGenerator(new JDKRandomGenerator(), moduleParameters);
    FunctionStateTracker functionStateTracker = this.functionStateTracker;
    for (int i = startPosition; i < moduleParameters.getMessageCount(); i++) {
      if (atLeastOneCheckpointCompleted && kaboomIndex.isPresent() && i >= kaboomIndex.getAsInt()) {
        throw new RuntimeException("KABOOM!!!");
      }
      SourceCommand command = generator.get();
      synchronized (ctx.getCheckpointLock()) {
        if (done) {
          return;
        }
        functionStateTracker.apply(command);
        ctx.collect(command);
        this.commandsSentSoFar = i;
      }
    }
  }

  private void verify(SourceContext<SourceCommand> ctx) {
    FunctionStateTracker functionStateTracker = this.functionStateTracker;

    for (int i = 0; i < moduleParameters.getNumberOfFunctionInstances(); i++) {
      final long expected = functionStateTracker.stateOf(i);

      Command.Builder verify =
          Command.newBuilder().setVerify(Command.Verify.newBuilder().setExpected(expected));

      SourceCommand command =
          SourceCommand.newBuilder()
              .setTarget(i)
              .setCommands(Commands.newBuilder().addCommand(verify))
              .build();
      synchronized (ctx.getCheckpointLock()) {
        ctx.collect(command);
      }
    }
  }

  // ---------------------------------------------------------------------------------------------------------------
  // Utils
  // ---------------------------------------------------------------------------------------------------------------

  private OptionalInt computeFailureIndex(int startPosition, int failureSoFar, int maxFailures) {
    if (failureSoFar >= maxFailures) {
      return OptionalInt.empty();
    }
    if (startPosition >= moduleParameters.getMessageCount()) {
      return OptionalInt.empty();
    }
    int index =
        ThreadLocalRandom.current().nextInt(startPosition, moduleParameters.getMessageCount());
    return OptionalInt.of(index);
  }

  private static void snooze() {
    try {
      Thread.sleep(2_000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static <T> T getOnlyElement(Iterable<T> items, T def) {
    Iterator<T> it = items.iterator();
    if (!it.hasNext()) {
      return def;
    }
    T item = it.next();
    if (it.hasNext()) {
      throw new IllegalStateException("Iterable has additional elements");
    }
    return item;
  }
}
