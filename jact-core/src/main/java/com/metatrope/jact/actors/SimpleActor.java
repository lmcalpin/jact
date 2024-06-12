package com.metatrope.jact.actors;

import com.metatrope.jact.Actor;

public abstract class SimpleActor extends Actor<Object, Object> {

    @Override
    public abstract Object onMessage(Object message) throws Exception;

}
