package com.accentype.android.softkeyboard;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.io.File;

public class LinearBackoffInterpolationModelTest extends AndroidTestCase {
    private static final String ModelFile = "testmodel.at";

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        File file = new File(getContext().getFilesDir(), ModelFile);
        if (file.exists()) {
            file.delete();
        }
    }

    public void testPredict() throws Exception {
        String modelDir = getContext().getFilesDir().getPath();
        LinearBackoffInterpolationModel model = LinearBackoffInterpolationModel.getInstance(ModelFile, modelDir);
        model.learn("bao gio di choi khong", "bao giờ đi chơi không");
        model.learn("tai sao lai the", "tại sao lại thế");
        model.dispose();

        model = LinearBackoffInterpolationModel.getInstance(ModelFile, modelDir);

        // sleep to allow async file loading to finish
        Thread.sleep(100);

        // test model persistence
        String case1 = model.predict("bao gio di");
        Assert.assertEquals("baogiờđi", case1);

        String case2 = model.predict("di choi khong the");
        Assert.assertEquals("đichơikhôngthế", case2); // last word is learned from second example

        String case3 = model.predict("uay uay");
        Assert.assertEquals(null, case3);

        String case4 = model.predict("tai xe");
        Assert.assertEquals("tại..", case4); // first word is learned from second example
    }

    public void testLearn() throws Exception {
        String modelDir = getContext().getFilesDir().getPath();
        LinearBackoffInterpolationModel model = LinearBackoffInterpolationModel.getInstance(ModelFile, modelDir);
        model.learn("Chu Nhat troi nang dep", "Chủ Nhật trời nắng đẹp");
        model.dispose();
    }
}