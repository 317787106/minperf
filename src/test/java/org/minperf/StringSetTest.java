package org.minperf;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import org.junit.Assert;
import org.minperf.universal.StringHash;

public class StringSetTest {

  public static final int MAX_CHUNK_SIZE = 1_000_000;

  private static List<String> readLargeByte32() {
    List<String> list = new ArrayList<>();
    list.addAll(Arrays.asList(
        "a",
        "b",
        "c",
        "d",
        "e",
        "f",
        "g",
        "h",
        "i",
        "j",
        "k",
        "l",
        "m",
        "n",
        "o",
        "p",
        "q",
        "r",
        "s",
        "t",
        "u",
        "v",
        "w",
        "x",
        "y",
        "z"));
    return list;
  }

  public static void main(String[] args) throws IOException {
    int leafSize = 8;
    int averageBucketSize = 1024; //128, 256, 512, 1024, 2048, 4096
    System.out.println("leafSize " + leafSize + ", averageBucketSize " + averageBucketSize +
        ", calcualted " +
        SpaceEstimator.getExpectedSpace(leafSize, averageBucketSize) + " bits/key");

    List<String> list = readLargeByte32();
    System.out.println("list size " + list.size());

    int len = list.size();
    StringHash hash = new StringHash();
    long time = System.nanoTime();
    BitBuffer buff = RecSplitBuilder.newInstance(hash)
        .leafSize(leafSize)
        .averageBucketSize(averageBucketSize)
        .eliasFanoMonotoneLists(true)
        .maxChunkSize(MAX_CHUNK_SIZE)
        .generate(list);
    time = System.nanoTime() - time;
    int bitCount = buff.position();
    buff.seek(0);
    double bitsPerKEy = (double) bitCount / len;
    System.out.println("        (" + len + ", " + bitsPerKEy + ")");
    System.out.println("...generated " + (double) time / len + " ns/key");

    RecSplitEvaluator<String> eval = RecSplitBuilder.newInstance(hash)
        .leafSize(leafSize)
        .averageBucketSize(averageBucketSize)
        .eliasFanoMonotoneLists(true)
        .buildEvaluator(buff);
    BitSet known = new BitSet();
    int i = 0;
    for (String x : list) {
      int index = eval.evaluate(x);
      if (index > len || index < 0) {
        Assert.fail("wrong entry: " + x + " " + index);
      }
      if (known.get(index)) {
        eval.evaluate(x);
        Assert.fail("duplicate entry: " + x + " " + index);
      }
      known.set(index);
      if ((i++ & 0xffffff) == 0xffffff) {
        System.out.println("...evaluated " + i);
      }
    }
  }
}
