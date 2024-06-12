package com.metatrope.jact.actors;

import com.metatrope.jact.Actor;

public class VoidActor extends Actor<Void, Void> {

    @Override
    public Void onMessage(Void message) throws Exception {
        return null;
    }

}
