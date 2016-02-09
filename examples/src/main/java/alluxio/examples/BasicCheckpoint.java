/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package alluxio.examples;

import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.Version;
import alluxio.client.file.FileInStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.URIStatus;
import alluxio.exception.AlluxioException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Callable;

/**
 * An example to show to how use Alluxio's API.
 */
public class BasicCheckpoint implements Callable<Boolean> {
  private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_TYPE);

  private final String mFileFolder;
  private final int mNumFiles;

  /**
   * @param fileFolder folder to use for the files
   * @param numFiles the number of files
   */
  public BasicCheckpoint(String fileFolder, int numFiles) {
    mFileFolder = fileFolder;
    mNumFiles = numFiles;
  }

  @Override
  public Boolean call() throws Exception {
    FileSystem fs = FileSystem.Factory.get();
    writeFile(fs);
    return readFile(fs);
  }

  private boolean readFile(FileSystem fs) throws IOException, AlluxioException {
    boolean pass = true;
    for (int i = 0; i < mNumFiles; i++) {
      AlluxioURI filePath = new AlluxioURI(mFileFolder + "/part-" + i);
      LOG.debug("Reading data from {}", filePath);
      FileInStream is = fs.openFile(filePath);
      URIStatus status = fs.getStatus(filePath);
      ByteBuffer buf = ByteBuffer.allocate((int) status.getBlockSizeBytes());
      is.read(buf.array());
      buf.order(ByteOrder.nativeOrder());
      for (int k = 0; k < mNumFiles; k++) {
        pass = pass && (buf.getInt() == k);
      }
      is.close();
    }
    return pass;
  }

  private void writeFile(FileSystem fs) throws IOException, AlluxioException {
    for (int i = 0; i < mNumFiles; i++) {
      ByteBuffer buf = ByteBuffer.allocate(80);
      buf.order(ByteOrder.nativeOrder());
      for (int k = 0; k < mNumFiles; k++) {
        buf.putInt(k);
      }
      buf.flip();
      AlluxioURI filePath = new AlluxioURI(mFileFolder + "/part-" + i);
      LOG.debug("Writing data to {}", filePath);
      OutputStream os = fs.createFile(filePath);
      os.write(buf.array());
      os.close();
    }
  }

  /**
   * Example program for using checkpoints.
   * Usage: {@code java -cp <ALLUXIO-VERSION> alluxio.examples.BasicCheckpoint <FileFolder> <Files>}
   *
   * @param args the folder for the files and the files to use
   * @throws IOException if the example fails to run
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.out.println("java -cp " + Version.ALLUXIO_JAR
          + " alluxio.examples.BasicCheckpoint <FileFolder> <Files>");
      System.exit(-1);
    }

    Utils.runExample(new BasicCheckpoint(args[0], Integer
        .parseInt(args[1])));
  }
}
