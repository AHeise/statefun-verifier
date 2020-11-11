package com.github.igalshilman.statefun.verifier;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;

@SuppressWarnings("unused")
public final class ModuleParameters implements Serializable {

  private static final long serialVersionUID = 1;

  private int numberOfFunctionInstances = 1_000;
  private int commandDepth = 10;
  private int messageCount = 100_000;
  private long sleepTimeBeforeVerifyMs = Duration.ofMinutes(2).toMillis();
  private long sleepTimeAfterVerifyMs = Duration.ofMinutes(1).toMillis();
  private int maxCommandsPerDepth = 3;
  private double stateModificationsPr = 0.4;
  private double sendPr = 0.9;
  private double sendAfterPr = 0.1;
  private double asyncSendPr = 0.1;
  private double noopPr = 0.2;
  private double sendEgressPr = 0.03;

  /**
   * Creates an instance of ModuleParameters from a key-value map.
   *
   * <p>See the bottom of a {@code flink-conf.yaml}, in this project for an example of how to
   * specify keys here, and the assosciated unit test.
   */
  public static ModuleParameters from(Map<String, String> globalConfiguration) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.convertValue(globalConfiguration, ModuleParameters.class);
  }

  public int getNumberOfFunctionInstances() {
    return numberOfFunctionInstances;
  }

  public void setNumberOfFunctionInstances(int numberOfFunctionInstances) {
    this.numberOfFunctionInstances = numberOfFunctionInstances;
  }

  public int getCommandDepth() {
    return commandDepth;
  }

  public void setCommandDepth(int commandDepth) {
    this.commandDepth = commandDepth;
  }

  public int getMessageCount() {
    return messageCount;
  }

  public void setMessageCount(int messageCount) {
    this.messageCount = messageCount;
  }

  public long getSleepTimeBeforeVerifyMs() {
    return sleepTimeBeforeVerifyMs;
  }

  public void setSleepTimeBeforeVerifyMs(long sleepTimeBeforeVerifyMs) {
    this.sleepTimeBeforeVerifyMs = sleepTimeBeforeVerifyMs;
  }

  public long getSleepTimeAfterVerifyMs() {
    return sleepTimeAfterVerifyMs;
  }

  public void setSleepTimeAfterVerifyMs(long sleepTimeAfterVerifyMs) {
    this.sleepTimeAfterVerifyMs = sleepTimeAfterVerifyMs;
  }

  public int getMaxCommandsPerDepth() {
    return maxCommandsPerDepth;
  }

  public void setMaxCommandsPerDepth(int maxCommandsPerDepth) {
    this.maxCommandsPerDepth = maxCommandsPerDepth;
  }

  public double getStateModificationsPr() {
    return stateModificationsPr;
  }

  public void setStateModificationsPr(double stateModificationsPr) {
    this.stateModificationsPr = stateModificationsPr;
  }

  public double getSendPr() {
    return sendPr;
  }

  public void setSendPr(double sendPr) {
    this.sendPr = sendPr;
  }

  public double getSendAfterPr() {
    return sendAfterPr;
  }

  public void setSendAfterPr(double sendAfterPr) {
    this.sendAfterPr = sendAfterPr;
  }

  public double getAsyncSendPr() {
    return asyncSendPr;
  }

  public void setAsyncSendPr(double asyncSendPr) {
    this.asyncSendPr = asyncSendPr;
  }

  public double getNoopPr() {
    return noopPr;
  }

  public void setNoopPr(double noopPr) {
    this.noopPr = noopPr;
  }

  public double getSendEgressPr() {
    return sendEgressPr;
  }

  public void setSendEgressPr(double sendEgressPr) {
    this.sendEgressPr = sendEgressPr;
  }

  @Override
  public String toString() {
    return "ModuleParameters{"
        + "numberOfFunctionInstances="
        + numberOfFunctionInstances
        + ", commandDepth="
        + commandDepth
        + ", messageCount="
        + messageCount
        + ", sleepTimeBeforeVerifyMs="
        + sleepTimeBeforeVerifyMs
        + ", sleepTimeAfterVerifyMs="
        + sleepTimeAfterVerifyMs
        + ", maxCommandsPerDepth="
        + maxCommandsPerDepth
        + ", stateModificationsPr="
        + stateModificationsPr
        + ", sendPr="
        + sendPr
        + ", sendAfterPr="
        + sendAfterPr
        + ", asyncSendPr="
        + asyncSendPr
        + ", noopPr="
        + noopPr
        + ", sendEgressPr="
        + sendEgressPr
        + '}';
  }
}
