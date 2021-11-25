# Social Community Detection module #

The Social Community Detection (SCD) module is an Extension module, and is part of the Graph Mining suite provided by Task 4.3.
SCD computes and updates social communities of the ego, based on the multilayer structure provided by the Contextual Ego Network (CEN), and the current status (Online/Offline) of the alters.

Communities are created whenever needed, and kept updated through time according to the changes detected on the CEN and thanks to a protocol implemented in the module, which observes the status of the alters and acts accordingly to spread the updates.

Other modules and apps can use SCD to lookup the current communities of the ego. Communities are intended to be social communities (based on friendship relationships), because the community detection task is performed on the CEN of the ego, which is made to store the social ties of the ego.


## About the module ##

The SCD module depends on the following HELIOS modules:
- [Contextual Ego Network module](https://github.com/helios-h2020/h.core-SocialEgoNetwork)
- Messaging module (JS version, access to this repo is available upon request to `jordi.hernandezv@atos.net`)

The module provides APIs to start and stop the module to give full control of its potential to the developer. Once started, it automatically connects to the alters and starts a messaging exchanging protocol to detect the social community structure of the ego. Communities are detected on context basis (each context has its own communities), are updated through time to reduce energy consumption and memory usage, and manages community lifecycle events locally. The seamless integration with the CEN module greatly helps in managing, recovering and using the community structure so that no overhead is spent. The module requires the identifiers used by the Nodes in the CEN module to be the same identifiers used in the Messaging module, otherwise nodes are not able to communicate with each other.


### How to configure and use the module ###

The module is not (yet!) available through Nexus, therefore an aar is provided alongside the code. The aar is available in the following directory: `socialcommunitydetection/build/outputs/aar`. To include the module in your Android Project, you have to dounload the socialcommunitydetection-release.aar file, then open the project you want to import the module into, and click File -> Project Structure. In the newly opened window, click on the '+' sign (add dependency) and select 'Jar dependency'. A new window opens which requests the path to the socialcommunitydetection-release.aar file (absoulte paths work, but relative paths are preferred) and a configuration (implementation is preferred). When finished, confirm by clicking on the 'OK' button in both windows, let the project synchronize the dependencies and build the project to make sure the module is now correctly integrated. An official guide is also available [here](https://developer.android.com/studio/projects/android-library).



## How to use the module ##
To start the SCD module, instantiate a `SocialCommunityDetection` object and then call the `startModule()` method. This method requires an already instantiated Contextual Ego Network to detect the community structure and a running ReliableHeliosMessagingNodejsLibp2pImpl for exchanging messages with other alters. The method returns true if the module was started correctly, or false is the parameters were found not valid.

To stop the module, simply call the `stopModule()` method. Data structures will be freed, and the module will stop sending messages to alters or reacting to CEN changes. The module supports object reusage, so an instance of the SCD module can be restarted by calling `startModule()` again. To check whether the module is running, use the utility method `isStarted()`. Although possible, it is strongly advised to *not* run multiple instances of this module on a single device.

Lastly, to get the community structure call the method `getCommunities()`. The method accepts a parameter, which is the context object (defined in the CEN module) for which to retrieve the communities. An unmodifiable list of Communities will be returned, please do not attempt to modify this list, otherwise an exception will be thrown. If the module is not running or the argument is invalid, null is returned.

A Community object can be inspected through several methods. The method `getCommunityId()` can be used to uniquely identify a community. The method `getCore()` returns an unmodifiable list of CEN Nodes, which represent the alters currently present in the community. Lastly, the method `prettyPrint()` can be used to generate a String (human readable) representation of the community.

## Project Structure ##
This project is structured as follows:
- The *src* directory contains the source code files.
- The *doc* directory contains the Javadoc.
