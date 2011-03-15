package org.apache.lucene.index.codecs.mocksep;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import org.apache.lucene.index.codecs.sep.IntIndexInput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.CodecUtil;
import org.apache.lucene.index.BulkPostingsEnum;

/** Reads IndexInputs written with {@link
 *  MockSingleIntIndexOutput}.  NOTE: this class is just for
 *  demonstration puprposes (it is a very slow way to read a
 *  block of ints).
 *
 * @lucene.experimental
 */
public class MockSingleIntIndexInput extends IntIndexInput {
  private final IndexInput in;

  public MockSingleIntIndexInput(Directory dir, String fileName, int readBufferSize)
    throws IOException {
    in = dir.openInput(fileName, readBufferSize);
    CodecUtil.checkHeader(in, MockSingleIntIndexOutput.CODEC,
                          MockSingleIntIndexOutput.VERSION_START,
                          MockSingleIntIndexOutput.VERSION_START);
  }

  @Override
  public Reader reader() throws IOException {
    return new Reader((IndexInput) in.clone());
  }

  @Override
  public void close() throws IOException {
    in.close();
  }

  public static class Reader extends BulkPostingsEnum.BlockReader {
    // clone:
    private final IndexInput in;
    private int offset;
    private final int[] buffer = new int[1];

    public Reader(IndexInput in) {
      this.in = in;
    }

    @Override
    public int[] getBuffer() {
      return buffer;
    }

    @Override
    public int offset() {
      return offset;
    }

    @Override
    public int end() {
      return 1;
    }

    @Override
    public int fill() throws IOException {
      buffer[0] = in.readVInt();
      offset = 0;
      return 1;
    }
  }
  
  class Index extends IntIndexInput.Index {
    private long fp;

    @Override
    public void read(DataInput indexIn, boolean absolute)
      throws IOException {
      if (absolute) {
        fp = indexIn.readVLong();
      } else {
        fp += indexIn.readVLong();
      }
    }

    @Override
    public void set(IntIndexInput.Index other) {
      fp = ((Index) other).fp;
    }

    @Override
    public void seek(BulkPostingsEnum.BlockReader other) throws IOException {
      ((Reader) other).in.seek(fp);
      other.fill();
    }

    @Override
    public String toString() {
      return Long.toString(fp);
    }

    @Override
    public Object clone() {
      Index other = new Index();
      other.fp = fp;
      return other;
    }
  }

  @Override
  public Index index() {
    return new Index();
  }
}

