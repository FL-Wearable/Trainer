package fl.wearable.autosport.model;

import android.graphics.Bitmap;

import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;
import com.google.gson.GsonBuilder;
import android.util.JsonReader;
import java.io.FileReader;

import static org.pytorch.Tensor.fromBlob;

class Layers{
    @com.google.gson.annotations.SerializedName(value= "0")
    public float[][] layer0;
    @com.google.gson.annotations.SerializedName(value= "1")
    public float[][] layer1;
    @com.google.gson.annotations.SerializedName(value= "2")
    public float[][] layer2;
    @com.google.gson.annotations.SerializedName(value= "3")
    public float[][] layer3;
}

public class Classifier {
    final int featureNum = 6;
    Module model;
    Layers layers = new Layers();
    float[] mean = {0.485f, 0.456f, 0.406f};
    float[] std = {0.229f, 0.224f, 0.225f};

    public Classifier(String modelPath) {
        model = Module.load(modelPath);
    }

    public void loadModelParameters(String path) throws java.io.FileNotFoundException {
        com.google.gson.Gson gson = new GsonBuilder().serializeNulls().create();
        JsonReader reader =new JsonReader(new FileReader(path));
        layers = gson.fromJson(String.valueOf(reader),Layers.class);
        System.out.println(layers);
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


    public float[] forward(float[] features) {
        //Calculate the first layer and apply activation function
        for(int i=0;i<layers.layer0.length;i++){
            float kernal_value = 0;
            for (int j=0;j<layers.layer0[0].length;j++){
                kernal_value += layers.layer0[i][j]*features[j];
            }
            layers.layer1[0][i]= kernal_value>0? kernal_value:0;
        }
        //Calculate the second later and get the raw result without softmax
        for(int i =0; i<layers.layer2.length;i++){
            float output_value = 0;
            for (int j=0;j<layers.layer2[0].length;j++){
                output_value += layers.layer2[i][j]*layers.layer1[0][j];
            }
            layers.layer3[0][i]=output_value;
        }

//        Tensor tensor = setFloatToTensor(features, featureNum);
//        IValue inputs = IValue.from(tensor);
//        Tensor outputs = model.forward(inputs).toTensor();
//        float[] scores = outputs.getDataAsFloatArray();
//        int classIndex = argMax(scores);
//        return fl.wearable.autosport.lib.Constants.MLP_CLASSES[classIndex];
        return layers.layer3[0];
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
    public float[] softmax(float[]features){
        float[] softmax_outputs = new float[]{0,0,0};
        float factor = Max(features);
        float base = (float) (Math.pow(Math.E,features[0]-factor)+ Math.pow(Math.E,features[1]-factor)+ Math.pow(Math.E,features[2]-factor));
        for(int i =0;i<softmax_outputs.length;i++){
            softmax_outputs[i]= (float) (Math.pow(Math.E,features[0]-factor)/base);
        }
        return softmax_outputs;
    }

    public int predict(float[] features,float theta){
        float[] outputs=forward(features);
        float[] probs=softmax(outputs);
        int classIndex = argMax(probs);
        float maxProb = Max(probs);
        if(maxProb>=theta){
            return classIndex;
        }
        else{
            return 3;
        }

    }

//    public int predict_with_threshold(float[] features, float theta) {
//        Tensor tensor = setFloatToTensor(features, featureNum);
//        IValue inputs = IValue.from(tensor);
//        Tensor outputs = model.forward(inputs).toTensor();
//        float[] probs = outputs.getDataAsFloatArray();
//        int classIndex = argMax(probs);
//        float maxProb = Max(probs);
//        if(maxProb>=theta){
//            return classIndex;
//        }
//        else{
//            return 3;
//        }
//
//    }


}
