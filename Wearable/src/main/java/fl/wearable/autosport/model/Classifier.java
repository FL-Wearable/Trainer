package fl.wearable.autosport.model;

import android.util.Log;
import fl.wearable.autosport.proto.SyftModel;

import static android.content.ContentValues.TAG;

class Layers{
    @com.google.gson.annotations.SerializedName(value= "0")
    public float[][] layer0  = new float[5][6];
    @com.google.gson.annotations.SerializedName(value= "1")
    public float[][] layer1  = new float[1][5];
    @com.google.gson.annotations.SerializedName(value= "2")
    public float[][] layer2  = new float[3][5];
    @com.google.gson.annotations.SerializedName(value= "3")
    public float[][] layer3  = new float[1][3];
}

public class Classifier {
    private org.pytorch.Tensor[] modelParams;
    private String modelpath;
    Layers layers = new Layers();
    SyftModel syftmodel = new fl.wearable.autosport.proto.SyftModel("watch",null,null,null);
    public Classifier(String paramPath) {
        this.modelpath = paramPath;
        if (this.modelpath == null)
            Log.d(TAG,"it is null");
        else
            this.syftmodel.loadModelState(this.modelpath);
        this.modelParams = this.syftmodel.getParamArray();
        for (int i=0; i<5; i++)
            for (int j = 0; j < 6; j++)
                layers.layer0[i][j] = this.modelParams[0].getDataAsFloatArray()[j % 6 + i * 6];

        for (int i=0; i<1; i++)
            for (int j=0; j<5; j++)
                layers.layer1[i][j] = this.modelParams[1].getDataAsFloatArray()[j%5 + i*5];

        for (int i=0; i<3; i++)
            for (int j=0; j<5; j++)
                layers.layer2[i][j] = this.modelParams[2].getDataAsFloatArray()[j%5 + i*5];

        for (int i=0; i<1; i++)
            for (int j=0; j<3; j++)
                layers.layer3[i][j] = this.modelParams[3].getDataAsFloatArray()[j%3 + i*3];
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
        //Log.d(TAG, "forward results are " + outputs[0] + " " + outputs[1] + " " + outputs[2]);
        //float[] probs=softmax(outputs);
        //Log.d(TAG, "softmax results are " + probs[0] + " " + probs[1] + " " + probs[2]);
        int classIndex = argMax(outputs);
        float maxProb = Max(outputs);
        if(maxProb>=theta){
            return classIndex;
        }
        else{
            return 3;
        }

    }

}
