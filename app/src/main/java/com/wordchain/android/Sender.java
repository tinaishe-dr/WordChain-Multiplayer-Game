package com.wordchain.android;

import wordchain.common.Message;

/** Lets screens send protocol messages without knowing about sockets. */
interface Sender {
    void send(Message message);
}
