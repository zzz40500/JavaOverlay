package com.dim.javaoverlay.common;

import okio.BufferedSource;
import okio.Okio;

/**
 * Created by dim on 17/2/23.
 */

public class TextFileUtils {

    public static void visit(File file,TextVisitor visitor) {
        BufferedSource buffer = null;
        try {
            buffer = Okio.buffer(Okio.source(file));
            String line;
            while ((line = buffer.readUtf8Line()) != null) {
                visitor.visit(line);

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (buffer != null) {
                try {
                    buffer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public interface TextVisitor {
        void visit(String line);
    }
}
