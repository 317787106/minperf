package org.minperf.universal;

import org.apache.tuweni.bytes.Bytes;

public class BytesHash implements UniversalHash<Bytes> {

  @Override
  public long universalHash(Bytes key, long index) {
    byte[] data = key.toArray();
    long hash = StringHash.getSipHash24(data, 0, data.length, index, index);
    //System.out.println("key: " + key.toHexString() + " index: " + index + " hash: " + hash);
    return hash;
  }

  @Override
  public String toString() {
    return "Byte32Hash (SipHash)";
  }
}
