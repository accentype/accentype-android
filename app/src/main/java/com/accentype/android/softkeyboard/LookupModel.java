package com.accentype.android.softkeyboard;

import android.content.Context;
import android.os.AsyncTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Simple lookup model.
 */
public class LookupModel implements BaseModel {

    private HashMap<Integer, HashMap<String, LocalModelItemData>> mLocalModel;
    private DataOutputStream mLocalModelBinaryWriter;
    private FileOutputStream mLocalModelOutputStream;
    private static LookupModel instance = null;

    private String mFileName;
    private String mFileDir;

    protected LookupModel(String fileName, String fileDir) {
        mFileName = fileName;
        mFileDir = fileDir;

        new LoadFromFile().execute();
    }

    public static LookupModel getInstance(String fileName, String fileDir) {
        if(instance == null) {
            instance = new LookupModel(fileName, fileDir);
        }
        return instance;
    }

    @Override public String predict(String rawPhrase) {
        if (mLocalModel != null) {
            String trimmedPhrase = rawPhrase.trim();
            if (trimmedPhrase.length() == 0) {
                return null;
            }
            int hashCode = trimmedPhrase.hashCode();
            if (mLocalModel.containsKey(hashCode)) {
                String topPrediction = null;

                HashMap<String, LocalModelItemData> hashEntryValue = mLocalModel.get(hashCode);

                int maxCount = -1;
                Iterator it = hashEntryValue.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    LocalModelItemData localModelItemData = (LocalModelItemData)pair.getValue();
                    if (maxCount < localModelItemData.count) {
                        maxCount = localModelItemData.count;
                        topPrediction = (String)pair.getKey();
                    }
                }

                // pad prediction with whitespaces in the original phrase
                int i = 0;
                for (; i < rawPhrase.length(); i++) {
                    if (!Character.isWhitespace(rawPhrase.charAt(i))) {
                        break;
                    }
                }
                StringBuilder finalPrediction = new StringBuilder(rawPhrase);
                finalPrediction.replace(i, i + topPrediction.length(), topPrediction);

                return finalPrediction.toString();
            }
        }
        return null;
    }

    @Override public void learn(String rawPhrase, String accentPhrase) {
        try {
            // TODO: for now skip input until model has been loaded
            if (mLocalModel != null && mLocalModelBinaryWriter != null) {
                String trimmedPhrase = rawPhrase.trim();
                String trimmedAccentPhrase = accentPhrase.trim();
                if (trimmedPhrase.length() == 0 || trimmedAccentPhrase.length() == 0) {
                    return;
                }
                int hashCode = trimmedPhrase.hashCode();
                HashMap<String, LocalModelItemData> hashEntryValue;
                LocalModelItemData localModelItem;
                if (mLocalModel.containsKey(hashCode)) {
                    hashEntryValue = mLocalModel.get(hashCode);
                } else {
                    hashEntryValue = new HashMap<>();
                }

                if (hashEntryValue.containsKey(trimmedAccentPhrase)) {
                    localModelItem = hashEntryValue.get(trimmedAccentPhrase);
                } else {
                    localModelItem = new LocalModelItemData();
                }
                localModelItem.count++;
                hashEntryValue.put(trimmedAccentPhrase, localModelItem);

                FileChannel fileChannel = mLocalModelOutputStream.getChannel();
                if (localModelItem.offset >= 0) {
                    fileChannel.position(localModelItem.offset);
                    mLocalModelBinaryWriter.writeInt(localModelItem.count);
                } else {
                    fileChannel.position(4);
                    mLocalModelBinaryWriter.writeInt(mLocalModel.size()); // update count

                    fileChannel.position(fileChannel.size()); // seek to end
                    mLocalModelBinaryWriter.writeInt(hashCode); // hash

                    byte[] accentBytes = trimmedAccentPhrase.getBytes("UTF-8"); // accent string
                    mLocalModelBinaryWriter.writeByte(accentBytes.length);
                    mLocalModelBinaryWriter.write(accentBytes);

                    // update offset in memory
                    localModelItem.offset = fileChannel.position();
                    mLocalModelBinaryWriter.writeInt(1); // count
                }

                // update model
                mLocalModel.put(hashCode, hashEntryValue);
            }
        }
        catch (IOException ex) { }
    }

    @Override public int version() {
        return ModelVersion.LOOKUP;
    }

    @Override public void dispose() {}

    private class LoadFromFile extends AsyncTask<Void, Void, HashMap<Integer, HashMap<String, LocalModelItemData>>> {
        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */
        protected HashMap<Integer, HashMap<String, LocalModelItemData>> doInBackground(Void... params) {
            HashMap<Integer, HashMap<String, LocalModelItemData>> localModel = new HashMap<>();

            try
            {
                String localModelFileName = mFileName;
                File localModelFile = new File(mFileDir, localModelFileName);

                mLocalModelOutputStream = new FileOutputStream(localModelFile);
                mLocalModelBinaryWriter = new DataOutputStream(mLocalModelOutputStream);

                // if not exists, then write header information and return
                if (!localModelFile.exists()) {
                    mLocalModelBinaryWriter.writeInt(version()); // model version
                    mLocalModelBinaryWriter.writeInt(0); // number of entries
                    return localModel;
                }

                CountingFileInputStream fileInputStream = new CountingFileInputStream(localModelFile);
                DataInputStream binaryReader = new DataInputStream(fileInputStream);

                try {
                    int modelVersion = binaryReader.readInt();
                    if (modelVersion != version()) {
                        return null;
                    }
                    int numEntries = binaryReader.readInt();

                    for (int i = 0; i < numEntries; i++) {
                        // Read hash code
                        int hashCode = binaryReader.readInt();

                        // Read accent string
                        byte numUnicodeBytes = binaryReader.readByte();
                        byte[] unicodeBytes = new byte[numUnicodeBytes];
                        binaryReader.read(unicodeBytes);
                        String accentString = new String(unicodeBytes, "UTF-8");

                        HashMap<String, LocalModelItemData> hashEntryValue;
                        LocalModelItemData itemData;

                        if (localModel.containsKey(hashCode)) {
                            hashEntryValue = localModel.get(hashCode);
                        } else {
                            hashEntryValue = new HashMap<>();
                        }
                        if (hashEntryValue.containsKey(accentString)) {
                            itemData = hashEntryValue.get(accentString);
                        } else {
                            itemData = new LocalModelItemData();
                        }

                        // Set position to the count
                        itemData.offset = fileInputStream.getOffset();

                        // Read count
                        byte count = binaryReader.readByte();

                        itemData.count += count;

                        hashEntryValue.put(accentString, itemData);

                        localModel.put(hashCode, hashEntryValue);
                    }
                }
                finally {
                    binaryReader.close();
                }
            }
            catch (UnsupportedEncodingException ex) { }
            catch (IOException ex) { }

            return localModel;
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(HashMap<Integer, HashMap<String, LocalModelItemData>> localModelFile) {
            mLocalModel = localModelFile;
        }
    }

    private class LocalModelItemData {
        public LocalModelItemData() {
            offset = -1;
        }

        public byte count;
        public long offset;
    }
}
