package eu.h2020.helios_social.extension.socialcommunitydetection;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosMessagingReceiver;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosNetworkAddress;

class SocialCommunityMessageReceiver implements HeliosMessagingReceiver {

    @Override
    public void receiveMessage(@NonNull HeliosNetworkAddress heliosNetworkAddress, @NonNull String s, @NonNull FileDescriptor fileDescriptor) {

        try {
            FileInputStream fis = new FileInputStream(fileDescriptor);
            int size = fis.available();
            byte[] bytes = new byte[size];
            if(fis.read(bytes)>0) receiveMessage(heliosNetworkAddress, s, bytes);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void receiveMessage(@NonNull HeliosNetworkAddress heliosNetworkAddress, @NonNull String s, @NonNull byte[] bytes) {

//      TODO: parse the message and call the methods to update the communities
    }
}
