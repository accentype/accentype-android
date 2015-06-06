package com.accentype.android.softkeyboard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by lhoang on 5/3/2015.
 */
public class CountingFileInputStream extends FileInputStream {
    private long offset;

    public CountingFileInputStream(File file) throws FileNotFoundException {
        super(file);
    }

    public CountingFileInputStream(String path) throws FileNotFoundException {
        super(path);
    }

    @Override public final int read() throws IOException {
        offset++;
        return super.read();
    }

    @Override
    public void close() throws IOException {
        offset = 0;
        super.close();
    }

    public long getOffset() {
        return offset;
    }
}
