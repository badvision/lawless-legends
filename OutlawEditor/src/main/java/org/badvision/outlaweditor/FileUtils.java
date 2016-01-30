/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
package org.badvision.outlaweditor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import javafx.stage.FileChooser;

/**
 *
 * @author brobert
 */
public class FileUtils {

    private FileUtils() {
    }

    public static enum Extension {

        XML("XML files", "xml"),
        TILESET("Tileset", "ots"),
        ALL("All files", "*"),
        BINARY("Binary", "bin");
        String description;
        String extension;

        Extension(String desc, String ext) {
            this.description = desc;
            this.extension = ext;
        }

        public FileChooser.ExtensionFilter getExtensionFilter() {
            return new FileChooser.ExtensionFilter(description + " (*." + extension + ")", "*." + extension);
        }
    }

    public static File getFile(File prevFile, String title, Boolean create, Extension... supportedExtensions) {
        FileChooser f = new FileChooser();
        if (prevFile != null) {
            if (prevFile.isFile()) {
                f.setInitialDirectory(prevFile.getParentFile());
            } else {
                f.setInitialDirectory(prevFile);
            }
        }
        f.setTitle(title);
        for (Extension e : supportedExtensions) {
            f.getExtensionFilters().add(e.getExtensionFilter());
        }
        if (create) {
            File file = f.showSaveDialog(Application.getPrimaryStage());
            if (file == null) {
                return null;
            }
            if (!file.getName().contains(".")) {
                return new File(file.getParentFile(), file.getName() + "." + supportedExtensions[0].extension);
            } else {
                return file;
            }
        } else {
            return f.showOpenDialog(Application.getPrimaryStage());
        }
    }

    public static <T> T loadFromFile(File f, Class<T> clazz) throws FileNotFoundException, IOException, ClassNotFoundException {
        if (!f.isFile() || !f.exists()) {
            return null;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        try (FileInputStream in = new FileInputStream(f)) {
            int read;
            while ((read = in.read(b)) > 0) {
                buffer.write(b, 0, read);
            }
            byte[] decompressed = decompress(buffer.toByteArray());
            return byteToObject(decompressed);
        }
    }

    public static void saveToFile(File f, Object obj) throws IOException {
        try (FileOutputStream fis = new FileOutputStream(f, false)) {
            fis.write(compress(objectToByte(obj)));
            fis.flush();
            fis.close();
        }
    }

    public static <T> T byteToObject(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return (T) is.readObject();
    }

    public static byte[] objectToByte(Object o) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream os = new ObjectOutputStream(out)) {
            os.writeObject(o);
            os.flush();
            os.close();
        }
        return out.toByteArray();
    }

    public static byte[] compress(byte[] bytesToCompress) {
        Deflater deflater = new Deflater();
        deflater.setInput(bytesToCompress);
        deflater.finish();
        byte[] bytesCompressed = new byte[Short.MAX_VALUE];
        int numberOfBytesAfterCompression = deflater.deflate(bytesCompressed);
        byte[] returnValues = new byte[numberOfBytesAfterCompression];
        System.arraycopy(
                bytesCompressed,
                0,
                returnValues,
                0,
                numberOfBytesAfterCompression);
        return returnValues;
    }

    public static byte[] decompress(byte[] bytesToDecompress) {
        try {
            int numberOfBytesToDecompress = bytesToDecompress.length;
            Inflater inflater = new Inflater();
            inflater.setInput(
                    bytesToDecompress,
                    0,
                    numberOfBytesToDecompress);
            int compressionFactorMaxLikely = 3;
            int bufferSizeInBytes = numberOfBytesToDecompress * compressionFactorMaxLikely;
            byte[] bytesDecompressed = new byte[bufferSizeInBytes];
            int numberOfBytesAfterDecompression = inflater.inflate(bytesDecompressed);
            byte[] returnValues = new byte[numberOfBytesAfterDecompression];
            System.arraycopy(
                    bytesDecompressed,
                    0,
                    returnValues,
                    0,
                    numberOfBytesAfterDecompression);
            inflater.end();
            return returnValues;
        } catch (DataFormatException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
