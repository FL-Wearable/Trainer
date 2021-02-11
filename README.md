## Introduction

Federated-Wearables is a demo for federated learning () over wearables based on [KotlinSyft](https://github.com/OpenMined/KotlinSyft) and [PySyft](https://github.com/OpenMined/PySyft). This is  the code of the smartwatch which offloads the collected data to the paired phone via Bluetooth for training in FL, and gets the updated model to run interface in real time.

In this demo, we conduct an activity recognition task. We also develop a companion app in `Application` directory. Specifically, the app enables users manually correct the labels to generated precise training samples upon witnessing wrong interface given by the model. 

Feel free to use this demo for your own task or application.

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

In the federated training, the model checkpoint is saved in `Trainer/Application/src/main/assets`, which is loaded by the smartwatch in the beginning of inference. The details of the parameter server is in `Trainer/Application/src/main/java/fl/wearable/autosport/`. It gets inspirations from [KotlinSyft](https://github.com/OpenMined/KotlinSyft) 