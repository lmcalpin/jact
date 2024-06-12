package com.metatrope.jact.config;

import com.metatrope.jact.remote.ServerConfig;

public class DefaultConfig implements Config {
    @Override
    public ServerConfig getRemotingConfiguration() {
        return null;
    }
}
