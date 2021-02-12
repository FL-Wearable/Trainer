## Introduction

Federated-Wearables is a demo for cross-device federated learning over wearables. This is  the code of the smartwatch which offloads the collected data to the paired phone via Bluetooth for training in FL, and gets the updated model to run interface in real time.

In this demo, we conduct an activity recognition task. To coordinate the FL and alleviate the calculation burden of wearables, we developed a companion app in `Application` directory. Notably, the app enables users manually correct the labels to generate more precise training samples upon witnessing wrong interface given by the model. 

Feel free to use this demo for your own task or application:)

## UI
<img src="/pic/watch1.png " width="200" height="200" /> <img src="/pic/watch2.png " width="200" height="200" /> <img src="/pic/watch3.png " width="200" height="200" /> 

<img src="/pic/watch4.png " width="200" height="200" /> <img src="/pic/watch5.png " width="200" height="200" /> <img src="/pic/watch6.png " width="200" height="200" />





## Inference

The inference happens directly on smartwatchs.

### The mainstram of Wearables

All is in `Trainer/Wearable/src/main/java/fl/wearable/autosport/MainActivity.java`

### Sensors in Wearbales 

In the folder `Trainer/Wearable/src/main/java/fl/wearable/autosport/sensors`. Here we implement sensor codes for those available in watches, such as gyroscope, heart rate, geolocation, etc.

#### Models in Wearbales

We manually implement the inference of a Multi-Layer Perception classifier in `Trainer/Wearable/src/main/java/fl/wearable/autosport/model/Classifier.java`.



## Training

The training requires the coordinations of a parameter server, the android app on the phones.

### Mobile Application

In the federated training, the model checkpoint is saved in `Trainer/Application/src/main/assets`, which is loaded by the smartwatch in the beginning of inference. The details of the parameter server is in `Trainer/Application/src/main/java/fl/wearable/autosport/`.


## Acknowledgement

[KotlinSyft](https://github.com/OpenMined/KotlinSyft) and [TrackerPublic](https://github.com/miltschek/TrackerPublic).
