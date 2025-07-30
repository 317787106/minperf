package org.minperf;


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.apache.tuweni.bytes.Bytes;
import org.junit.Assert;
import org.minperf.universal.BytesHash;

public class LargeBytesSetTest {

  public static final int MAX_CHUNK_SIZE = 1_000_000;

  private static List<Bytes> readLargeByte32() throws IOException {
    List<Bytes> list = new ArrayList<>();
    FileInputStream fileInputStream = new FileInputStream(
        "/Volumes/资料/output-directory/snapshot/trans/txId-00.data");
    BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
    byte[] bytes = new byte[32];
    int count = 0;
    while (bufferedInputStream.read(bytes) != -1) {
      byte[] newBytes = new byte[bytes.length];
      System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
      list.add(Bytes.wrap(newBytes));
      count++;
      if (count > 10_000_000) {
        break;
      }
    }
    return list;
  }

  public static void main(String[] args) throws IOException {
    int leafSize = 8;
    int averageBucketSize = 1024; //128, 256, 512, 1024, 2048, 4096
    System.out.println("leafSize " + leafSize + ", averageBucketSize " + averageBucketSize +
        ", calcualted " +
        SpaceEstimator.getExpectedSpace(leafSize, averageBucketSize) + " bits/key");

    List<Bytes> list = readLargeByte32();
    System.out.println("list size " + list.size());

    int len = list.size();
    BytesHash hash = new BytesHash();
    long time = System.nanoTime();
    BitBuffer buff = RecSplitBuilder.newInstance(hash)
        .leafSize(leafSize)
        .averageBucketSize(averageBucketSize)
        .eliasFanoMonotoneLists(false)
        .maxChunkSize(MAX_CHUNK_SIZE)
        .generate(list);
    time = System.nanoTime() - time;
    int bitCount = buff.position();
    buff.seek(0);
    double bitsPerKEy = (double) bitCount / len;
    System.out.println("        (" + len + ", " + bitsPerKEy + ")");
    System.out.println("...generated " + (double) time / len + " ns/key");

    byte[] modelDescription = buff.toByteArray();
    System.out.println("RecSplitBuilder size :" + modelDescription.length);

    RecSplitEvaluator<Bytes> eval = RecSplitBuilder.newInstance(hash)
        .leafSize(leafSize)
        .averageBucketSize(averageBucketSize)
        .eliasFanoMonotoneLists(false)
        .buildEvaluator(new BitBuffer(modelDescription));
    BitSet known = new BitSet();
    int i = 0;
    for (Bytes x : list) {
      int index = eval.evaluate(x);
      //System.out.println(x.toFastHex(true) + " " + index);
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
