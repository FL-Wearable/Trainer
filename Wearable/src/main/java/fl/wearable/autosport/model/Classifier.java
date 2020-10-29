package fl.wearable.autosport.model;
import java.util.Arrays;
import java.util.Collections;
import android.graphics.Bitmap;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;
import static org.pytorch.Tensor.fromBlob;


public class Classifier {
    final int featureNum = 6;
    Module model;
    float[] mean = {0.485f, 0.456f, 0.406f};
    float[] std = {0.229f, 0.224f, 0.225f};

    public Classifier(String modelPath) {
        model = Module.load(modelPath);
    }

    public void setMeanAndStd(float[] mean, float[] std) {
        this.mean = mean;
        this.std = std;
    }

    public Tensor preprocess(Bitmap bitmap, int size) {

        bitmap = Bitmap.createScaledBitmap(bitmap, 1, size, false);
        return TensorImageUtils.bitmapToFloat32Tensor(bitmap, this.mean, this.std);
    }

    public Tensor setFloatToTensor(float[] features, int size) {
        long[] sizeArr = new long[]{1, size};
        return fromBlob(features, sizeArr);
    }

    public int argMax(float[] inputs) {

        int maxIndex = -1;
        float maxValue = 0.0f;

        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] > maxValue) {
                maxIndex = i;
                maxValue = inputs[i];
            }
        }
        return maxIndex;
    }


    public String predict(float[] features) {
        Tensor tensor = setFloatToTensor(features, featureNum);
        IValue inputs = IValue.from(tensor);
        Tensor outputs = model.forward(inputs).toTensor();
        float[] scores = outputs.getDataAsFloatArray();
        int classIndex = argMax(scores);
        return Constants.MLP_CLASSES[classIndex];
    }

    public float Max(float[] arr){
        float max = -1.0f;
        for (float v : arr) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    public String predict_with_threshold(float[] features, float theta) {
        Tensor tensor = setFloatToTensor(features, featureNum);
        IValue inputs = IValue.from(tensor);
        Tensor outputs = model.forward(inputs).toTensor();
        float[] probs = outputs.getDataAsFloatArray();
        int classIndex = argMax(probs);
        float maxProb = Max(probs);
        if(maxProb>=theta){
            return fl.wearable.autosport.model.Constants.MLP_CLASSES[classIndex];
        }
        else{
            return fl.wearable.autosport.model.Constants.UNKNOWN;
        }

    }


}
