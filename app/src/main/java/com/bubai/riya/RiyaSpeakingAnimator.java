package com.bubai.riya;

public class RiyaSpeakingAnimator {
    private final RiyaMouthView mouth;

    public RiyaSpeakingAnimator(RiyaMouthView mouth) {
        this.mouth = mouth;
    }

    public void start() {
        if (mouth != null) mouth.startSpeaking();
    }

    public void onTextCharacter(char ch) {
        if (mouth != null) mouth.setMouthForCharacter(ch);
    }

    public void stop() {
        if (mouth != null) mouth.stopSpeaking();
    }
}
